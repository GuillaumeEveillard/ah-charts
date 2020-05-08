
import org.apache.commons.text.StringTokenizer
import org.apache.commons.text.matcher.StringMatcherFactory
import parser.*
import java.lang.IllegalArgumentException
import java.util.*

fun transformLuaDataStructureToJson(luaDataStructure: String) : String {
    val lines = luaDataStructure
        .split("\n")
        .map { it.substringBeforeLast("--").replace("[", "").replace("]", "").replace("=", ":") }

    tokenize2(luaDataStructure)
    
    return ""
}

fun tokenize(s: String) : List<Token> {
    val scanner = Scanner(s)

    val tokens = mutableListOf<Token>()
    while (scanner.hasNext()) {
        if(scanner.hasNextLong()) {
            tokens.add(LongToken(scanner.nextLong()))
        } else {
            val x = scanner.next()
            when(x) {
                "{" -> tokens.add(ObjectStart())
                "}" -> tokens.add(ObjectEnd())
                "=" -> {}
                else -> {
                    if(x.startsWith("[")) {
                        tokens.add(Key(x.replace("[", "").replace("]", "").replace("\"", "")))
                    } else if(x.startsWith("\"").and(x.endsWith("\""))) {
                        tokens.add(StringToken(x.substring(1, x.length-1)))
                    } else {
                        throw IllegalArgumentException("Strange: "+x)
                    }
                }
            }
        }
    }
    
    return tokens
}

fun tokenize2(s: String) : List<Token> {
    val delimMatcher = StringMatcherFactory.INSTANCE.charSetMatcher(' ', ',', '{', '}', '=')
    val quoteMatcher = StringMatcherFactory.INSTANCE.charSetMatcher('"')
    val st = StringTokenizer(s, delimMatcher, quoteMatcher)
    st.setIgnoreEmptyTokens(false)
    while (st.hasNext()) {
        val t = st.nextToken();
        t.toString()
    }
    
    return emptyList()
}

