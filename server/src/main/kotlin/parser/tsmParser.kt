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

fun main() {
    val filePath = "F:\\Jeux\\World of Warcraft\\_classic_\\WTF\\Account\\GGYE\\SavedVariables\\TradeSkillMaster.lua"
    val lines = File(filePath).readLines()

    val text = StringBuilder()
    
    for (line in lines) {
      val cleanLine = line.substringBeforeLast("--")
      if(cleanLine.startsWith("TradeSkillMasterDB")) {
          text.append("{")
      } else if(cleanLine.startsWith("TSMItemInfoDB")) {
        break          
      }else {
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

fun getValue(ast: LuaElement, key: String) : String {
    if(ast is LuaObject) {
        val value = ast.getElementByKey(key)
        if(value is StringLiteral) {
            return value.value
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