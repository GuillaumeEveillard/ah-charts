import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.MissingRequestParameterException
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun main() {
    val parsingResult = loadResultFiles()
    val analyzer = Analyzer(parsingResult.auctions)
    val wishList = loadWishList(parsingResult)

    val server = embeddedServer(Netty, port = 9898) {
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
        }
    }
    server.start(wait = true)
}