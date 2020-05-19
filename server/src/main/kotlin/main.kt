import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import parser.ParserResult
import java.io.File
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import kotlin.math.max

data class Item(val id: Long, val frenchName: String?, val englishName: String?) {
    fun complete() = frenchName != null && englishName != null
}
data class Auction(val itemId: Long, val quantity: Long, val bid: Long, val buyout: Long?, val timestamp: Instant) {
    fun bidByUnit() = bid.toDouble() / quantity.toDouble()
    fun buyoutByUnit() = if(buyout == null) null else  buyout.toDouble() / quantity.toDouble()
}

data class Database(val items: Collection<Item>, val auctions: List<Auction>) {
    val itemById = items.map { it.id to it }
    val auctionsPerItem = auctions.groupBy { it.itemId }.mapValues { ItemAuctions(it.value) }
    
    fun findItemByName(name: String) : Item? {
        return items.find { it.frenchName == name || it.englishName == name }
    }

    fun latestBestBuyout(itemId: Long) : Double {
        return bestBuyout(itemId).toSortedMap().values.last()
    }

    fun bestBuyout(itemId: Long) : Map<Instant, Double> {
        val auctions = auctionsPerItem[itemId]?: throw IllegalArgumentException("Item $itemId unknown")
        return auctions.bestBuyout()
    }

    fun bestBuyoutPerDay(itemId: Long) : Map<LocalDate, Double> {
        val auctions = auctionsPerItem[itemId]?: throw IllegalArgumentException("Item $itemId unknown")
        return auctions.bestBuyoutPerDay()
    }

    fun bestAverageBuyout(itemId: Long, n: Long) : Map<Instant, Double> {
        val auctions = auctionsPerItem[itemId]?: throw IllegalArgumentException("Item $itemId unknown")
        return auctions.bestAverageBuyout(n)
    }
}

fun loadDatabaseFromJsonFiles(folder: File): Database {
    val items = mutableMapOf<Long, Item>()
    val auctions = mutableListOf<Auction>()
    
    folder
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

    fun bestAverageBuyout(n: Long): Map<Instant, Double> {
        return auctionsPerTimestamp
                .mapValues { a -> bestAverageBuyout(a.value, n) }
                .filterValues { it != null } as Map<Instant, Double>
               
    }
    
    private fun bestAverageBuyout(auctions: Collection<Auction>, n: Long) : Double? {
        val avgPrice = auctions
                .filter { it.buyoutByUnit() != null }
                .map { AveragePrice(it.buyoutByUnit()!!, it.quantity) }
                .sortedBy { it.price }
                .reduce { acc, pair -> acc.addPrice(pair.price, pair.quantity, n) }
        return if (avgPrice.quantity < n) null else avgPrice.price   
    }

}

data class AveragePrice(val price: Double, val quantity: Long) {
    fun addPrice(price: Double, quantity: Long, limit: Long) : AveragePrice{
        if(this.quantity >= limit) {
            return this
        } else {
            val totalQuantity = max(this.quantity + quantity, limit)
            val avgPrice = (this.price * this.quantity + price*(totalQuantity - this.quantity)) / totalQuantity
            return AveragePrice(avgPrice, this.quantity + quantity)
        }
    }
}