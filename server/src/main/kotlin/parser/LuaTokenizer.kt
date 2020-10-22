package parser

import java.lang.IllegalArgumentException

sealed class Token
sealed class ValueToken : Token()
data class StringToken(val s: String) : ValueToken() {
    override fun toString() = "String<$s>"
}
data class DoubleToken(val l: Double) : ValueToken()
data class BooleanToken(val b: Boolean) : ValueToken()
object ObjectStart : Token() {
    override fun toString() = "Object start"
}
object ObjectEnd : Token() {
    override fun toString() = "Object end"
}
data class Key(val key: String) : Token() {
    override fun toString() = "Key<$key>"
}
object Assignment : Token()

interface TokenProducer {
    fun produce(c: Char, iterator: ListIterator<Char>): Token?
}

val assignmentTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '=') Assignment else null
    }
}

val objectStartTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '{') ObjectStart else null
    }
}

val objectEndTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '}') ObjectEnd else null
    }
}

val keyTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '[') {
            val nextC = iterator.next() 
            val s = stringLogic(nextC, iterator)?.s ?: longLogic(nextC, iterator)?.l.toString() ?: throw IllegalArgumentException("The key is invalid")
            iterator.next() // to consume the [
            Key(s)
        } else {
            null
        }
    }
}

val stringTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): StringToken? {
        return stringLogic(c, iterator)
    }
}

private fun stringLogic(c: Char, iterator: ListIterator<Char>): StringToken? {
    return if (c == '"') {
        val s = iterator.asSequence().takeWhile { it != '"' }.joinToString("")
        StringToken(s)
    } else {
        null
    }
}

val longTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): DoubleToken? {
        return longLogic(c, iterator)
    }
}

private fun longLogic(c: Char, iterator: ListIterator<Char>): DoubleToken? {
    return if (c.isDigit() || c == '-') {
        val s = iterator.asSequence().takeWhile { it.isDigit() || it == '.' }.joinToString("")
        iterator.previous()
        val x = iterator.next()
        if (!x.isDigit()) {
            iterator.previous()
        }
        return DoubleToken((c + s).toDouble())
    } else {
        null
    }
}

val booleanToken : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c.isLetter()) {
            val s = iterator.asSequence().takeWhile { it.isLetter() }.joinToString("")
            iterator.previous()
            val x = iterator.next()
            if(!x.isLetter()) {
                iterator.previous()
            }
            BooleanToken((c+s).toBoolean())
        } else {
            null
        }
    }
}

fun tokenize(s: String) : List<Token> {
    val tokens = mutableListOf<Token>()
    val iterator = s.toList().listIterator()
    while(iterator.hasNext()) {
        val c = iterator.next()
        val token = assignmentTokenProducer.produce(c, iterator) ?:
            objectStartTokenProducer.produce(c, iterator) ?:
            objectEndTokenProducer.produce(c, iterator) ?:
            keyTokenProducer.produce(c, iterator) ?:
            stringTokenProducer.produce(c, iterator) ?:
            longTokenProducer.produce(c, iterator) ?:
            booleanToken.produce(c, iterator)
        if(token != null) {
            tokens.add(token)
        }
    }
    return tokens
}