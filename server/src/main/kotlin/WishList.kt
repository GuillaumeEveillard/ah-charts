import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.IllegalArgumentException

data class WishListItem(val id: Long, val comment: String?, val buyPrice: Long?, val sellPrice: Long?, val profiles: List<String>)
data class WishListItemConfig(val name: String, val comment: String? = null, val buyPrice: Long? = null, val sellPrice: Long? = null, val profiles: List<String>)

class WishList(val wishListItems: Collection<WishListItem>) {
    
    private val wishListByProfile = wishListItems
            .map { it.profiles.map { p -> p to it } }
            .flatten()
            .groupBy ( {e -> e.first}, {e -> e.second})
    
    fun profiles() = wishListByProfile.keys
    
    fun wishListItem(profile: String) = wishListByProfile[profile] ?: throw IllegalArgumentException("The profile $profile does not exist")
}

fun loadWishList(database: Database) : WishList {
    val json = File("data/wish-list.json").readText()
    val t: TypeToken<List<WishListItemConfig>> = object : TypeToken<List<WishListItemConfig>>() {}
    val w = Gson().fromJson<List<WishListItemConfig>>(json, t.type)

    return WishList(w.mapNotNull {
        val item = database.findItemByName(it.name)
        if (item == null) null else WishListItem(item.id, it.comment, it.buyPrice, it.sellPrice, it.profiles?: emptyList())
    })
}