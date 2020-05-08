package sample

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.perror

fun hello(): String = "Hello, Kotlin/Native!"


enum class Language {FRENCH, ENGLISH}

@Serializable
data class AuctionatorSnapshot(val snapshot_at: Long, val auctions: List<AuctionatorAuction>)
@Serializable
data class AuctionatorAuction(val q: Long, val s: Long, val b: Long, val i: Long, val n: String)

@Serializable
data class ParserResult(val language: Language, val items: Collection<Item>, val auctions: List<Auction>, val timestamp: String)
@Serializable
data class Item(val id: Long, val frenchName: String?, val englishName: String?) {
    fun complete() = frenchName != null && englishName != null
}
@Serializable
data class Auction(val itemId: Long, val quantity: Long, val bid: Long, val buyout: Long?, val timestamp: String) {
    fun bidByUnit() = bid.toDouble() / quantity.toDouble()
    fun buyoutByUnit() = if(buyout == null) null else  buyout.toDouble() / quantity.toDouble()
}

fun main(args: Array<String>) {
    val language = Language.valueOf(args[0])
    val filePath = args[1]

    val file = fopen(filePath, "r")
    if (file == null) {
        perror("cannot open input file $filePath")
        return
    }

    val lines = mutableListOf<String>()

    try {
        memScoped {
            val bufferLength = 64 * 1024
            val buffer = allocArray<ByteVar>(bufferLength)
            
            do {
                val nextLine = fgets(buffer, bufferLength, file)?.toKString()
                if(nextLine != null) {
                    lines.add(nextLine)
                }
            }
            while (nextLine != null)
        }
    } finally {
        fclose(file)
    }
    
    
//    println(lines)

    val r = parseAuctionerFile(lines, language)
}

//private suspend fun xx(snap: AuctionatorSnapshot) {
//    val client = HttpClient()
//
//    val json = io.ktor.client.features.json.defaultSerializer()
//    client.post<Unit>() {
//        url("http://127.0.0.1:8080/")
//        body = json.write(snap) // Generates an OutgoingContent
//    }
//}

private fun parseAuctionerFile(lines: List<String>, language: Language): ParserResult {
    val snapshot = StringBuilder()
    var record = false
    var comma = false
    for (line in lines) {
        if (record) {

            if (line.startsWith("}")) {
                snapshot.append("}")
                break
            }

            val cleanLine = line.substringBeforeLast("--").replace("[", "").replace("]", "").replace("=", ":")
            if (record) {
                if (comma && !cleanLine.contains("}")) {
                    snapshot.append(",").append("\n")
                    comma = false
                }

                if (cleanLine.contains("\"auctions\" : {")) {
                    snapshot.append("\t\"auctions\" : [")
                } else if (cleanLine.startsWith("\t}")) {
                    snapshot.append("\t]").append("\n")
                } else {
                    snapshot.append(cleanLine.replace(",", ""))
                    if (!cleanLine.contains("{")) {
                        comma = true
                    }
                }
            }

        } else {
            if (line.contains("AUCTIONATOR_SNAPSHOT")) {
                record = true
                snapshot.append("{")
            }
        }
    }

    val s = snapshot.toString()

    println(s)

    val json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))
    val o = json.parse(AuctionatorSnapshot.serializer(), s)

    println(o)
    

    val timestamp = o.snapshot_at.toString()
    val items = o.auctions.map { createItem(it, language) }.toSet()
    val auctions = o.auctions.map {
        val byout = if (it.b == 0L) null else it.b
        Auction(it.i, it.q, it.s,  byout, timestamp) }
    return ParserResult(language, items, auctions, timestamp)
}

private fun createItem(it: AuctionatorAuction, language: Language): Item {
    return when(language) {
        Language.FRENCH -> Item(it.i, it.n, null)
        Language.ENGLISH -> Item(it.i, null, it.n)
    }
}