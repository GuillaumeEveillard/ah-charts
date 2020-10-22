package parser

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException


interface LuaElement {
    fun toJson(): String
}

data class LuaObject(val content: List<LuaElement>) : LuaElement {
    override fun toJson(): String {
        return "[\n" + content.joinToString(",\n") { it.toJson() } + "]"
    }

    fun getElementByKey(key: String): LuaElement? {
        val kv = content.find { it is KV && it.key == key } as KV?
        return kv?.value
    }
}

data class BooleanLiteral(val value: Boolean) : LuaElement {
    override fun toJson() = value.toString()
}

data class StringLiteral(val value: String) : LuaElement {
    override fun toJson() = "\"$value\""
}

data class LongLiteral(val value: Double) : LuaElement {
    override fun toJson() = value.toString()
}

data class KV(val key: String, val value: LuaElement) : LuaElement {
    override fun toJson() =  "{\"key\": \"$key\", \"value\": " + value.toJson() + "}"
}

fun buildAst(tokens: List<Token>): LuaElement {
    val iterator = tokens.iterator()
    if (iterator.next() !is ObjectStart) throw IllegalArgumentException("Expect an object start")
    val root = buildLuaObject(iterator)
    if (iterator.hasNext()) throw IllegalStateException("We didn't finished the tokens")
    return root
}

private fun buildLuaObject(tokens: Iterator<Token>): LuaElement {
    val content = mutableListOf<LuaElement>()
    while (tokens.hasNext()) {
        var currentToken = tokens.next()
        if (currentToken is ObjectEnd) {
            return LuaObject(content)
        } else if (currentToken is ObjectStart) {
            content.add(buildLuaObject(tokens))
        } else {
            if (currentToken is Key) {
                val key = currentToken.key

                currentToken = tokens.next()
                if (currentToken !is Assignment) throw IllegalArgumentException("Expect an assignment")
                currentToken = tokens.next()
                val value = when (currentToken) {
                    is StringToken -> StringLiteral(currentToken.s)
                    is DoubleToken -> LongLiteral(currentToken.l)
                    is BooleanToken -> BooleanLiteral(currentToken.b)
                    is ObjectStart -> buildLuaObject(tokens)
                    else -> throw IllegalArgumentException("Expect a literal or an object")
                }
                content.add(KV(key, value))
            } else if (currentToken is StringToken) {
                content.add(StringLiteral(currentToken.s))
            } else if (currentToken is DoubleToken) {
                content.add(LongLiteral(currentToken.l))
            } else {
                throw IllegalArgumentException("Expect a key, got $currentToken")
            }
        }
    }
    return LuaObject(content)
}