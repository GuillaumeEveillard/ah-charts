package parser

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.time.Instant

enum class OperationType {BUY, SELL}
data class Operation(val type: OperationType, val item: Long,val stackSize: Long,val quantity: Long,val price: Long,val otherPlayer: String ,val player: String,val time: Instant)

interface ObjectBuilder<T> {
    fun buildObject(elems: Map<String,String>) : T
}

class CsvParser<T>(private val builder: ObjectBuilder<T>, private val header: String) {

    private var column: List<String> = header.split(",")

    fun parse(line: String): T {
        return builder.buildObject(line.split(",").mapIndexed{index: Int, s: String ->  Pair(column[index], s)}.toMap())
    }
}

enum class Slot {INVENTORY, BANK, MAIL, AUCTION}

class Stock {
    val stocks = mutableMapOf<Long, ItemStock>()

    fun add(item: ItemInStock) {
        stocks.compute(item.item){_, itemStock -> (itemStock?:ItemStock()).add(item)}
    }

    override fun toString(): String {
        return "Stock(stocks=$stocks)"
    }

    fun getItemInStock(itemId: Long): List<ItemInStock> {
        return stocks[itemId]?.getItemInStock() ?: emptyList()
    }


}

class ItemStock {
    val stock = mutableMapOf<String, MutableMap<Slot, ItemInStock>>()
    
    fun add(item: ItemInStock) : ItemStock {
        val stockForCharacter = stock.getOrPut(item.character) {mutableMapOf()}
        stockForCharacter.compute(item.slot) {_, q -> if(q == null) item else q.add(item.quantity)}
        return this
    }

    override fun toString(): String {
        return "ItemStock(stock=$stock)"
    }

    fun getItemInStock(): List<ItemInStock> {
        return stock.values.map { it.values }.flatten()
    }


}

data class ItemInStock(val item: Long, val quantity: Long, val character: String, val slot: Slot) {
    fun add(quantity: Long): ItemInStock {
        return this.copy(quantity = this.quantity + quantity)
    }
}

fun main() {
    readStockFromTsm()

}

fun readStockFromTsm(): Stock {
    val filePath = "F:\\Jeux\\World of Warcraft\\_classic_\\WTF\\Account\\GGYE\\SavedVariables\\TradeSkillMaster.lua"
    val lines = File(filePath).readLines()

    val text = StringBuilder()

    for (line in lines) {
        val cleanLine = line.substringBeforeLast("--")
        if (cleanLine.startsWith("TradeSkillMasterDB")) {
            text.append("{")
        } else if (cleanLine.startsWith("TSMItemInfoDB")) {
            break
        } else {
            text.append(cleanLine)

        }
    }

    val tokens = tokenize2(text.toString())
    val ast = buildAst(tokens)


    val buys = parseBuys(ast)
    val sells = parseSales(ast)

    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(buys + sells)

    File("auction-history.json").writeText(json)
    val timestamp = Instant.now().epochSecond
    File("auction-history-$timestamp.json").writeText(json)


    val keys = discoverTsmStockKey(ast)


    val itemsInStock = keys.map { key ->
        val tsmKey = key.tsmKey()
        val o = getObject(ast, tsmKey)
        getValues(o)
                .mapValues { if (it.value is LongLiteral) (it.value as LongLiteral).value else null }
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

data class StockReadingKey(val character: String, val slot: Slot) {
    fun tsmKey() : String {
        val tsmSlot = when(slot) {
            Slot.INVENTORY -> "bagQuantity"
            Slot.BANK -> "bankQuantity"
            Slot.MAIL -> "mailQuantity"
            Slot.AUCTION -> "auctionQuantity"
        }
        return "s@$character - Alliance - Sulfuron@internalData@$tsmSlot"
    }
}

fun parseBuys(ast: LuaElement): List<Operation> {
    val value = getValue(ast,"r@Sulfuron@internalData@csvBuys")
    val lines = value.split("\\n")
    val parser = CsvParser(OperationBuilder(OperationType.BUY), lines[0])
    return lines.subList(1, lines.size).mapNotNull { parser.parse(it) }
}

fun parseSales(ast: LuaElement): List<Operation> {
    val value = getValue(ast,"r@Sulfuron@internalData@csvSales")
    val lines = value.split("\\n")
    val parser = CsvParser(OperationBuilder(OperationType.SELL), lines[0])
    return lines.subList(1, lines.size).mapNotNull { parser.parse(it) }
}

fun getContent(ast: LuaElement) : List<LuaElement> {
    if(ast is LuaObject) {
        return ast.content
    }
    throw IllegalArgumentException("Not good format")
}

fun getValue(ast: LuaElement, key: String) : String {
    if(ast is LuaObject) {
        val value = ast.getElementByKey(key)
        if(value is StringLiteral) {
            return value.value
        }
    }
    throw IllegalArgumentException("Not good format")
}

fun getValues(ast: LuaElement) : Map<String, LuaElement> {
    if(ast is LuaObject) {
        return ast.content.mapNotNull { if(it is KV) Pair(it.key, it.value) else null }.toMap()
    }
    throw IllegalArgumentException("Not good format")
}

fun getObject(ast: LuaElement, key: String) : LuaObject {
    if(ast is LuaObject) {
        val value = ast.getElementByKey(key)
        if(value is LuaObject) {
            return value
        }
    }
    throw IllegalArgumentException("Not good format")
}

class OperationBuilder(private val type: OperationType): ObjectBuilder<Operation?> {
    override fun buildObject(elems: Map<String, String>): Operation? {
        //itemString,stackSize,quantity,price,otherPlayer,player,time
        if(elems["source"] != "Auction") {
            return null
        } else {
            return Operation(
                    type,
                    elems["itemString"]!!.substringAfter(":").substringBefore(":").toLong(),
                    elems["stackSize"]!!.toLong(),
                    elems["quantity"]!!.toLong(),
                    elems["price"]!!.toLong(),
                    elems["otherPlayer"]!!,
                    elems["player"]!!,
                    Instant.ofEpochSecond(elems["time"]!!.toLong()))
        }
    }
}