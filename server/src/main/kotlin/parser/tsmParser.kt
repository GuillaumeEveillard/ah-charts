package parser

import ItemInStock
import Slot
import Stock
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import extractStockFromTsmDb
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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



fun main() {
    val tsmFile = File("C:\\Program Files (x86)\\World of Warcraft\\_classic_\\WTF\\Account\\GGYE\\SavedVariables\\TradeSkillMaster.lua")
    readStockFromTsm(tsmFile)
}



fun readStockFromTsm(tsmFile: File): Stock {
    println("[TSM extraction] [START]")

    val timestamp = Instant.now()
    
    backupTsmFile(tsmFile, timestamp)

    val text = readDbSectionOfTsmFile(tsmFile)

    val tokens = tokenize2(text)
    val ast = buildAst(tokens)


    val buys = parseBuys(ast)
    val sells = parseSales(ast)
    val operations = buys + sells

    saveOperationHistory(operations, timestamp)

    return extractStockFromTsmDb(ast)
}

private fun saveOperationHistory(operations: List<Operation>, timestamp: Instant) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(operations)

    val databaseFolder = File("data/database")
    if (!databaseFolder.exists()) {
        databaseFolder.mkdirs()
    }

    databaseFolder.resolve("auction-history.json").writeText(json)
    databaseFolder.resolve("auction-history-${timestamp.epochSecond}.json").writeText(json)
}

private fun backupTsmFile(tsmFile: File, timestamp: Instant) {
    val targetTsmFolder = File("data/tsm")
    if (!targetTsmFolder.exists()) {
        targetTsmFolder.mkdirs()
    }
    
    val originalFileBackup = targetTsmFolder.resolve("original-${timestamp.epochSecond}.lua")
    Files.copy(tsmFile.toPath(), originalFileBackup.toPath(), StandardCopyOption.REPLACE_EXISTING)
}



private fun readDbSectionOfTsmFile(tsmFile: File): String {
    val lines = tsmFile.readLines()

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
    return text.toString()
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