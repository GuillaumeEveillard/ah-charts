import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.MissingRequestParameterException
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import parser.*
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CliArgs(parser: ArgParser) {
    val port : Int by parser.storing(
            "-p", "--port",
            help = "Port used by the server (default = 9898).") {
        toInt()
    }.default(9898)

    val wowFolder : String by parser.storing(
                    "-w", "--wow-folder",
                    help = "WoW folder (default = C:\\Program Files (x86)\\World of Warcraft ).")
            .default("C:\\Program Files (x86)\\World of Warcraft")

    val language by parser.storing(
                    "-l", "--language",
                    help = "WoW language (ENGLISH or FRENCH, default: ENGLISH).")
            .default("ENGLISH")
}

fun main(args: Array<String>) {
    mainBody {
        ArgParser(args).parseInto(::CliArgs).run {
            val wowFolder = File(wowFolder)
            val auctionatorFilePath = wowFolder.resolve("_classic_\\WTF\\Account\\GGYE\\SavedVariables\\Auctionator.lua")
            val tsmFilePath = wowFolder.resolve("_classic_\\WTF\\Account\\GGYE\\SavedVariables\\TradeSkillMaster.lua")

            extractAuctionatorData(auctionatorFilePath, Language.valueOf(language))

            val stock = readStockFromTsm(tsmFilePath)

            val parsingResult = loadResultFiles()
            val analyzer = Analyzer(parsingResult.auctions)
            val wishList = loadWishList(parsingResult)

//            val t: TypeToken<List<Operation>> = object : TypeToken<List<Operation>>() {}
//            val operations = Gson().fromJson<List<Operation>>(File("data/database/auction-history.json").readText(), t.type)
            val operations = readAllAuctionHistoryFiles()
            val auctionHistory = AuctionHistory(operations)

            val server = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    gson {
                        registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { date, _, _ -> if (date == null) JsonNull.INSTANCE else JsonPrimitive(date.format(
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME)) })
                        registerTypeAdapter(Instant::class.java, JsonSerializer<Instant> { instant, _, _ -> if (instant == null) JsonNull.INSTANCE else JsonPrimitive(instant.atZone(
                                ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)) })
                        setPrettyPrinting()
                    }
                }
                install(CORS) {
                    method(HttpMethod.Get)
                    method(HttpMethod.Post)
                    method(HttpMethod.Delete)
                    method(HttpMethod.Put)
                    method(HttpMethod.Patch)
                    anyHost()
                }

                routing {
                    static("/") {
                        resources("static")
                        defaultResource("static/index.html")
                    }

                    static("static") {
                        resources("static/static")
                    }

                    route("wish") {
                        get {
                            call.respond(HttpStatusCode.OK, wishList)
                        }
                        get("/ready-to-buy") {
                            val x = wishList.filter { analyzer.latestBestBuyout(it.id) <= (it.buyPrice ?: 0) }
                            call.respond(HttpStatusCode.OK, x)
                        }
                    }
                    route("/items") {
                        get {
                            call.respond(HttpStatusCode.OK, parsingResult.items)
                        }
                    }

                    route("/quotations") {
                        route("/{itemId}") {
                            get {
                                val itemId = call.parameters["itemId"]?.toLongOrNull()
                                        ?: throw MissingRequestParameterException("Item id " + call.parameters["itemId"] + " invalid.")
                                call.respond(HttpStatusCode.OK, analyzer.bestBuyout(itemId))
                            }
                            get("/best-average/{n}") {
                                val itemId = call.parameters["itemId"]?.toLongOrNull()
                                        ?: throw MissingRequestParameterException("Item id " + call.parameters["itemId"] + " invalid.")
                                val n = call.parameters["n"]?.toLongOrNull()
                                        ?: throw MissingRequestParameterException("N " + call.parameters["n"] + " invalid.")
                                call.respond(HttpStatusCode.OK, analyzer.bestAverageBuyout(itemId, n))
                            }
                        }
                    }

                    route("auctions") {
                        route("history") {
                            get("/{itemId}") {
                                val itemId = call.parameters["itemId"]?.toLongOrNull()
                                        ?: throw MissingRequestParameterException("Item id " + call.parameters["itemId"] + " invalid.")
                                val operations = auctionHistory.getOperations(itemId)
                                call.respond(HttpStatusCode.OK, operations)
                            }
                        }
                    }

                    route("stock") {
                        get("/{itemId}") {
                            val itemId = call.parameters["itemId"]?.toLongOrNull()
                                    ?: throw MissingRequestParameterException("Item id " + call.parameters["itemId"] + " invalid.")
                            val itemInStock = stock.getItemInStock(itemId)
                            call.respond(HttpStatusCode.OK, itemInStock)
                        }
                    }
                }
            }
            server.start(wait = true)
        }
    }
}

class AuctionHistory(val operations: Collection<Operation>) {
    val operationsByItem = operations.groupBy { it.item }
    
    fun getOperations(item: Long): List<Operation> {
        return operationsByItem[item] ?: emptyList()
    }
}