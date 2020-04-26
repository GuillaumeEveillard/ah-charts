package parser

import Auction
import Item
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.lang.StringBuilder
import java.time.Instant

data class ParserResult(val items: Collection<Item>, val auctions: List<Auction>)
data class AuctionatorSnapshot(val snapshot_at: Long, val auctions: List<AuctionatorAuction>)
data class AuctionatorAuction(val q: Long, val s: Long, val b: Long, val i: Long, val n: String)

fun main(args: Array<String>) {
    
    val filePath = args[0]

    parseAuctionerFile(filePath)

}

public fun parseAuctionerFile(filePath: String): ParserResult {
    val lines = File(filePath).readLines()

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
                    snapshot.append(",").append(System.lineSeparator())
                    comma = false
                }

                if (cleanLine.contains("\"auctions\" : {")) {
                    snapshot.append("\t\"auctions\" : [")
                } else if (cleanLine.startsWith("\t}")) {
                    snapshot.append("\t]").append(System.lineSeparator())
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
    val o = Gson().fromJson<AuctionatorSnapshot>(s, AuctionatorSnapshot::class.java)

    val timestamp = Instant.ofEpochSecond(o.snapshot_at)
    val items = o.auctions.map { Item(it.i, it.n, null) }.toSet()
    val auctions = o.auctions.map { Auction(it.i, it.q, it.s, it.b, timestamp) }
    val result = ParserResult(items, auctions)
    
    val ss = GsonBuilder().setPrettyPrinting().create().toJson(result)
    File("result-${timestamp.toEpochMilli()}.json").writeText(ss)
    
    return result 
}