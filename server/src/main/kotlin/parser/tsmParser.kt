package parser

import java.io.File
import java.lang.StringBuilder

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

    val json = ast.toJson()
    
    File("hey.json").writeText(json)
//    println(json)
}