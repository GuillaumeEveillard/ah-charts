import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import parser.Operation
import java.io.File
import java.io.FileReader
import java.nio.charset.Charset


fun loadOperationHistory(f: File) : OperationHistory? {
    return if(f.exists()) {
        Gson().fromJson(FileReader(f), OperationHistory::class.java)
    } else {
        null
    }
}

fun loadAndUpdateOperationHistory(masterFile: File, unitaryFileFolder: File) : OperationHistory {
    val operationHistory = loadOperationHistory(masterFile) ?: OperationHistory()
    operationHistory.integrateOperations(unitaryFileFolder)
    operationHistory.save(masterFile)
    return operationHistory
}


data class OperationHistory(var alreadyIntegratedHash: Set<String>, var operations: List<Operation>) {
    @Transient private val gson = GsonBuilder().setPrettyPrinting().create()
    
    constructor() : this(emptySet(), emptyList())
    
    fun save(f: File) {
        val json = gson.toJson(this)
        f.writeText(json)
    }
    
    fun integrateOperations(folder: File) : Boolean {
        val allOperations = mutableSetOf<Operation>()
        val hashes = mutableSetOf<String>()
        
        folder
            .listFiles()
            .filter {
                it.name.startsWith("operation-history") &&
                    !alreadyIntegratedHash.contains(extractHashFromFilename(it)) }
            .forEach {
                val hash = extractHashFromFilename(it)
                hashes.add(hash)
                val t: TypeToken<List<Operation>> = object : TypeToken<List<Operation>>() {}
                val op : List<Operation> = gson.fromJson(it.readText(), t.type)
                allOperations.addAll(op)
            }

        if(hashes.isNotEmpty()) {
            hashes.addAll(alreadyIntegratedHash)
            allOperations.addAll(operations)
            
            alreadyIntegratedHash = hashes
            operations = allOperations.sortedBy { it.time }
        }
        
        return hashes.isNotEmpty()
    }

    private fun extractHashFromFilename(it: File) = it.name.substringAfterLast("-").substringBeforeLast(".")
}

fun main() {
    val root = File("C:\\Programmation\\ah-charts\\data")
    val operationOld = root.resolve("operation-history-old")
    val operationNew = root.resolve("operation-history")
//    convertOldAuctionHistoryFilesToNewFormat(operationOld, operationNew)

    loadAndUpdateOperationHistory(root.resolve("operation-history-master.json"), operationNew)
}


fun convertOldAuctionHistoryFilesToNewFormat(sourceFolder: File, destinationFolder: File) {
    sourceFolder
            .listFiles()
            .filter {it.name.startsWith("auction-history-")}
            .forEach{
                convertOldAuctionHistoryFileToNewFormat(it, destinationFolder)
            }
}

fun convertOldAuctionHistoryFileToNewFormat(f: File, destinationFolder: File) {
    val s = f.readText()
    val hash = Hashing.sha256().hashString(s, Charset.defaultCharset())
    val hex = BaseEncoding.base16().encode(hash.asBytes()).substring(0, 10)
//    val hex = String.format("%1$05X",hash.asLong())
    val newName = f.nameWithoutExtension + "-" + hex + ".json"
    f.copyTo(destinationFolder.resolve(newName))
}


fun readAllAuctionHistoryFiles(): List<Operation> {
    val allOperations = mutableSetOf<Operation>()
    val gson = Gson()
    File("data/database")
            .listFiles()
            .filter {it.name.startsWith("auction-history-")}
            .forEach {
                it.readText()
                val t: TypeToken<List<Operation>> = object : TypeToken<List<Operation>>() {}
                val op : List<Operation> = gson.fromJson(it.readText(), t.type)
                allOperations.addAll(op)
            }

    return allOperations.sortedBy { it.time }
}
