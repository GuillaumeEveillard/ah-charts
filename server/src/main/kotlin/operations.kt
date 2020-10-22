import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import parser.Operation
import java.io.File
import java.io.FileReader
import java.lang.RuntimeException
import java.nio.charset.Charset
import java.time.Instant


fun loadAndUpdateOperationHistory(dataFolder: File, unitaryFileFolder: File) : OperationHistory {
    val computerName = computerName()

    val masterFile = dataFolder.resolve("operation-history-master-$computerName.json")
    
    val otherMasterFiles = dataFolder.listFiles()
            .filter { it.name.startsWith("operation-history-master-") && it.name != masterFile.name}
    
    val operationHistory = loadOperationHistory(masterFile) ?: OperationHistory()
    println("[Operation history merge] ${operationHistory.operations.size} operations in master file before merge "+masterFile.absolutePath)

    otherMasterFiles.forEach {
        val otherOperationHistory = loadOperationHistory(it)
        operationHistory.merge(otherOperationHistory!!)
        println("[Operation history merge]  ${operationHistory.operations.size} operations after merging ${it.absolutePath}")
    }

    print("[Operation history merge] Integrating unitary files...")
    operationHistory.integrateOperations(unitaryFileFolder)
    println(" done")
    
    println("[Operation history merge] ${operationHistory.operations.size} operations in master file after merge "+masterFile.absolutePath)
    operationHistory.save(masterFile)

    return operationHistory
}

private fun computerName(): String {
    return if (System.getProperty("os.name").startsWith("Windows")) {
        System.getenv("COMPUTERNAME")
    } else {
        System.getenv("HOSTNAME") ?: throw RuntimeException("Impossible to determine computer name")
    }
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
    
    fun merge(otherOperationHistory: OperationHistory) {
        val allOperations = mutableSetOf<Operation>()
        val hashes = mutableSetOf<String>()
        
        allOperations.addAll(otherOperationHistory.operations)
        hashes.addAll(otherOperationHistory.alreadyIntegratedHash)

        alreadyIntegratedHash = hashes
        operations = allOperations.sortedBy { it.time }
    }
}

fun saveOperationHistory(dataFolder: File, operations: List<Operation>, timestamp: Instant) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(operations)

    val operationHistoryFolder = dataFolder.resolve("operation-history")
    if (!operationHistoryFolder.exists()) {
        operationHistoryFolder.mkdirs()
    }

    val hash = Hashing.sha256().hashString(json, Charset.defaultCharset())
    val hex = BaseEncoding.base16().encode(hash.asBytes()).substring(0, 10)
    val destinationFile = operationHistoryFolder.resolve("operation-history-${timestamp.epochSecond}-$hex.json")
    destinationFile.writeText(json)
    println("The "+operations.size+" operations have been save into "+destinationFile.absolutePath)
}

private fun loadOperationHistory(f: File) : OperationHistory? {
    return if(f.exists()) {
        Gson().fromJson(FileReader(f), OperationHistory::class.java)
    } else {
        null
    }
}