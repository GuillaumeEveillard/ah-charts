import com.google.gson.GsonBuilder
import java.io.File

fun main() {
    val wishList = listOf(
        WishListItemConfig("Elixir de défense excellente"),
        WishListItemConfig("Elixir de défense supérieure"),
        WishListItemConfig("Potion de Bouclier de pierre inférieure"),
        WishListItemConfig("Potion de Bouclier de pierre supérieure"),
        WishListItemConfig("Rhum de Rumsey label noir", buyPrice = 5000))

    val json = GsonBuilder().setPrettyPrinting().create().toJson(wishList)
    File("data/wish-list.json").writeText(json)
}