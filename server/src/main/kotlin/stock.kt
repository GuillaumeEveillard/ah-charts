import parser.*

enum class Slot {INVENTORY, BANK, MAIL, AUCTION}

/**
 * Global stock of all items for all characters
 */
class Stock {
    private val stocks = mutableMapOf<Long, ItemStock>()

    fun add(item: ItemInStock) {
        stocks.compute(item.item){_, itemStock -> (itemStock?:ItemStock()).add(item)}
    }

    override fun toString() = "Stock(stocks=$stocks)"

    fun getItemInStock(itemId: Long): List<ItemInStock> = stocks[itemId]?.getItemInStock() ?: emptyList()
}

/**
 * Total stock (all characters and slots) for one item
 */
class ItemStock {
    private val stock = mutableMapOf<String, MutableMap<Slot, ItemInStock>>()

    fun add(item: ItemInStock) : ItemStock {
        val stockForCharacter = stock.getOrPut(item.character) {mutableMapOf()}
        stockForCharacter.compute(item.slot) {_, q -> q?.add(item.quantity) ?: item }
        return this
    }

    override fun toString() = "ItemStock(stock=$stock)"

    fun getItemInStock(): List<ItemInStock> = stock.values.map { it.values }.flatten()
}

/**
 * The stock of one item for a particular character and a particular slot
 */
data class ItemInStock(val item: Long, val quantity: Long, val character: String, val slot: Slot) {
    fun add(quantity: Long) = this.copy(quantity = this.quantity + quantity)
}

fun extractStockFromTsmDb(ast: LuaElement): Stock {
    val keys = discoverTsmStockKey(ast)

    val itemsInStock = keys.map { key ->
        val tsmKey = key.tsmKey()
        val o = getObject(ast, tsmKey)
        getValues(o)
                .mapValues { if (it.value is DoubleLiteral) (it.value as DoubleLiteral).value else null }
                .filterValues { it != null }
                .map {
                    ItemInStock(
                            it.key.substringAfter(":").substringBefore(":").toLong(),
                            it.value!!.toLong(),
                            key.character,
                            key.slot)
                }
    }.flatten()

    val stock = Stock()
    itemsInStock.forEach { stock.add(it) }

    println("[TSM extraction] [DONE]")

    return stock
}

private fun discoverTsmStockKey(ast: LuaElement): List<StockReadingKey> {
    val scopeKeys = getObject(ast, "_scopeKeys")
    val char = getObject(scopeKeys, "sync")
    val r = getContent(char).mapNotNull { if (it is StringLiteral) it.value else null }

    return r.map {
        val character = it.substringBefore(" ")
        listOf(
                StockReadingKey(character, Slot.INVENTORY),
                StockReadingKey(character, Slot.BANK),
                StockReadingKey(character, Slot.MAIL),
                StockReadingKey(character, Slot.AUCTION)
        )
    }.flatten()
}