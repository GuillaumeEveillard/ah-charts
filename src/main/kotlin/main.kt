import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import parser.ParserResult
import parser.parseAuctionerFile
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Item(val id: Long, val frenchName: String?, val englishName: String?)
data class Auction(val itemId: Long, val quantity: Long, val bid: Long, val buyout: Long, val timestamp: Instant) {
    fun bidByUnit() = bid.toDouble() / quantity.toDouble()
    fun buyoutByUnit() = buyout.toDouble() / quantity.toDouble()
}
data class WishListItem(val id: Long, val price: Long?)
data class WishListItemConfig(val name: String, val price: Long? = null)


fun main(args: Array<String>) {

    val filePath = args[0]

//    val parsingResult = parseAuctionerFile(filePath)
    val parsingResult = loadResultFiles()
    val analyzer = Analyzer(parsingResult.auctions)
    val r = analyzer.bestPricePerDay(7078)
    println(r)
}

fun loadResultFiles(): ParserResult {
//    val t = object : TypeToken<List<WishListItemConfig>>() {}.type

    val items = mutableSetOf<Item>()
    val auctions = mutableListOf<Auction>()
    
    File(".")
            .listFiles { f -> f.name.startsWith("result-")}
            .forEach { 
                val r = Gson().fromJson<ParserResult>(it.readText(), ParserResult::class.java)
                items.addAll(r.items)
                auctions.addAll(r.auctions)
            }
    
    return ParserResult(items, auctions)
}

class ItemAuctions(auctions: List<Auction>) {
    val auctionsPerTimestamp : Map<Instant, Collection<Auction>> =
        auctions.groupBy { it.timestamp }
    

    fun bestBuyoutPerDay() : Map<LocalDate, Double> {
        return auctionsPerTimestamp.entries
            .groupBy({k -> k.key.atZone(ZoneId.systemDefault()).toLocalDate()},  { v -> v.value})
            .mapValues { it.value.flatten().map { a -> a.buyoutByUnit() }.min()!! }
    }
    
}

class Analyzer(val auctions: Collection<Auction>) {

    val auctionsPerItem = auctions.groupBy { it.itemId }.mapValues { ItemAuctions(it.value) }
    
//    val auctionPerItem: Map<Long, NavigableMap<Instant, Collection<Auction>>> = auctions
//        .groupingBy { it.itemId }
//        .aggregate{_, acc, elem, _ -> 
//            val x = acc ?: TreeMap<Instant, Auction>()
//            x.compute(elem.timestamp, { _, v -> if(v == null) mutableListOf<>(elem) else v.
//            x
//        }
//
    fun bestPricePerDay(itemId: Long) : Map<LocalDate, Double> {
        val auctions = auctionsPerItem[itemId]?: throw IllegalArgumentException("Item $itemId unknown")
       return auctions.bestBuyoutPerDay()
    }
}