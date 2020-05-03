import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import parser.ParserResult
import java.io.File
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Item(val id: Long, val frenchName: String?, val englishName: String?) {
    fun complete() = frenchName != null && englishName != null
}
data class Auction(val itemId: Long, val quantity: Long, val bid: Long, val buyout: Long?, val timestamp: Instant) {
    fun bidByUnit() = bid.toDouble() / quantity.toDouble()
    fun buyoutByUnit() = if(buyout == null) null else  buyout.toDouble() / quantity.toDouble()
}
data class WishListItem(val id: Long, val price: Long?)
data class WishListItemConfig(val name: String, val price: Long? = null)
data class Database(val items: Collection<Item>, val auctions: List<Auction>) {
    val itemById = items.map { it.id to it }
    fun findItemByName(name: String) : Item? {
        return items.find { it.frenchName == name || it.englishName == name }
    }
}

fun loadWishList(database: Database) : Collection<WishListItem> {
    val json = File("wish-list.json").readText()
    val t: TypeToken<List<WishListItemConfig>> = object : TypeToken<List<WishListItemConfig>>() {}
    val w = Gson().fromJson<List<WishListItemConfig>>(json, t.type)
    
    return w.mapNotNull {
        val item = database.findItemByName(it.name)
        if (item == null) null else WishListItem(item.id, it.price)
    }
}

fun main(args: Array<String>) {

//    val filePath = args[0]

//    val parsingResult = parseAuctionerFile(filePath)
    val parsingResult = loadResultFiles()
    val analyzer = Analyzer(parsingResult.auctions)
    val r = analyzer.bestBuyoutPerDay(21151)
    println(r)
    val rr = analyzer.bestBuyout(21151)
    println(rr)
}

fun loadResultFiles(): Database {
//    val t = object : TypeToken<List<WishListItemConfig>>() {}.type

    val items = mutableMapOf<Long, Item>()
    val auctions = mutableListOf<Auction>()
    
    File(".")
            .listFiles { f -> f.name.startsWith("result-")}
            .forEach { 
                val r = Gson().fromJson<ParserResult>(it.readText(), ParserResult::class.java)
                r.items.forEach {
                    items.compute(it.id) {id, i -> buildItem(i, it)}
                }
                auctions.addAll(r.auctions)
            }
    
    return Database(items.values, auctions)
}

private fun buildItem(item: Item?, item2: Item): Item {
    if(item == null || item2.complete()) {
        return item2
    }
    return Item(item2.id, item.frenchName ?: item2.frenchName, item.englishName ?: item2.englishName)
}

class ItemAuctions(auctions: List<Auction>) {
    val auctionsPerTimestamp : Map<Instant, Collection<Auction>> =
        auctions.groupBy { it.timestamp }
    

    fun bestBuyoutPerDay() : Map<LocalDate, Double> {
        return auctionsPerTimestamp.entries
            .groupBy({ k -> k.key.atZone(ZoneId.systemDefault()).toLocalDate()},  { v -> v.value})
            .mapValues { it.value.flatten().mapNotNull { a -> a.buyoutByUnit() }.min() }
            .filterValues { it != null } as Map<LocalDate, Double>
    }

    fun bestBuyout(): Map<Instant, Double> {
        return auctionsPerTimestamp
                .mapValues { a -> a.value.mapNotNull { a -> a.buyoutByUnit() }.min() }
                .filterValues { it != null } as Map<Instant, Double>
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
    fun bestBuyout(itemId: Long) : Map<Instant, Double> {
        val auctions = auctionsPerItem[itemId]?: throw IllegalArgumentException("Item $itemId unknown")
        return auctions.bestBuyout()
    }
    
    fun bestBuyoutPerDay(itemId: Long) : Map<LocalDate, Double> {
        val auctions = auctionsPerItem[itemId]?: throw IllegalArgumentException("Item $itemId unknown")
       return auctions.bestBuyoutPerDay()
    }
}