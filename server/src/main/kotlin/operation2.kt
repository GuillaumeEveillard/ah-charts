import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import java.io.File
import java.nio.charset.Charset

/**
 * Experimental code, not used in production
 */

fun main() {
    val root = File("D:\\Source\\ah-charts\\data")
    val operationOld = root.resolve("database")
    val operationNew = root.resolve("operation-history")
    convertOldAuctionHistoryFilesToNewFormat(operationOld, operationNew)

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
    val newName = f.nameWithoutExtension + "-" + hex + ".json"
    f.copyTo(destinationFolder.resolve(newName))
}