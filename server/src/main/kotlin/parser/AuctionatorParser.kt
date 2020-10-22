package parser

import Auction
import Item
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant

enum class Language {FRENCH, ENGLISH}
data class ParserResult(val language: Language, val items: Collection<Item>, val auctions: List<Auction>, val timestamp: Instant)
data class AuctionatorSnapshot(val snapshot_at: Long, val auctions: List<AuctionatorAuction>)
data class AuctionatorAuction(val q: Long, val s: Long, val b: Long, val i: Long, val n: String)

/**
 * Read auctionator file, backup it, extract the relevant data and save them in a json file
 */
fun extractAuctionatorData(file: File, language: Language) {
    println("[AUCTIONATOR extraction] [START]")
    
    val result = parseAuctionerFile(file, language)
    val jsonFile = File("data/database/result-${result.timestamp.toEpochMilli()}.json")
    println("The result will be written in ${jsonFile.absolutePath}")
    val resultAsString = GsonBuilder().setPrettyPrinting().create().toJson(result)
    jsonFile.writeText(resultAsString)
    println("Done")

    val originalFileBackup = File("data/auctionator/original-${result.timestamp.toEpochMilli()}.lua")
    println("The original will be copied in ${originalFileBackup.absolutePath}")
    Files.copy(file.toPath(), originalFileBackup.toPath(), StandardCopyOption.REPLACE_EXISTING)

    println("[AUCTIONATOR extraction] [END]")
}

private fun parseAuctionerFile(file: File, language: Language): ParserResult {
    println("Loading source file: $file")
    val lines = file.readLines()

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
    val o = Gson().fromJson(s, AuctionatorSnapshot::class.java)

    val timestamp = Instant.ofEpochSecond(o.snapshot_at)
    val items = o.auctions.map { createItem(it, language) }.toSet()
    val auctions = o.auctions.map { 
        val buyout = if (it.b == 0L) null else it.b
        Auction(it.i, it.q, it.s,  buyout, timestamp) }

    println("Source file loaded ($file)")
    return ParserResult(language, items, auctions, timestamp)
}

private fun createItem(it: AuctionatorAuction, language: Language): Item {
     return when(language) {
         Language.FRENCH -> Item(it.i, it.n, null)
         Language.ENGLISH -> Item(it.i, null, it.n)
     }
}