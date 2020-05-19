import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class WishListItem(val id: Long, val comment: String?, val buyPrice: Long?, val sellPrice: Long?)
data class WishListItemConfig(val name: String, val comment: String? = null, val buyPrice: Long? = null, val sellPrice: Long? = null)

fun loadWishList(database: Database) : Collection<WishListItem> {
    val json = File("data/wish-list.json").readText()
    val t: TypeToken<List<WishListItemConfig>> = object : TypeToken<List<WishListItemConfig>>() {}
    val w = Gson().fromJson<List<WishListItemConfig>>(json, t.type)

    return w.mapNotNull {
        val item = database.findItemByName(it.name)
        if (item == null) null else WishListItem(item.id, it.comment, it.buyPrice, it.sellPrice)
    }
}