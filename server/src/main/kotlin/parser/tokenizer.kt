package parser

import java.lang.IllegalArgumentException


sealed class Token
sealed class ValueToken : Token()
class StringToken(val s: String) : ValueToken() {
    override fun toString(): String {
        return "String<$s>"
    }
}
class DoubleToken(val l: Double) : ValueToken()
class BooleanToken(val b: Boolean) : ValueToken()
class ObjectStart() : Token() {
    override fun toString(): String {
        return "Object start"
    }
}
class ObjectEnd() : Token() {
    override fun toString(): String {
        return "Object end"
    }
}
class Key(val key: String) : Token() {
    override fun toString(): String {
        return "Key<$key>"
    }
}
class Assignment() : Token()

fun main() {
    tokenize2("salut = fer { fd }{ [toto]= \"string\" 500= true false 2")
}

interface TokenProducer {
    fun produce(c: Char, iterator: ListIterator<Char>): Token?
}

val assignmentTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '=') Assignment() else null
    }
}

val objectStartTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '{') ObjectStart() else null
    }
}

val objectEndTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '}') ObjectEnd() else null
    }
}

val keyTokenProducer : TokenProducer = object : TokenProducer {
    override fun produce(c: Char, iterator: ListIterator<Char>): Token? {
        return if(c == '[') {
            val nextC = iterator.next() 
            val s = stringLogic(nextC, iterator)?.s ?: longLogic(nextC, iterator)?.l.toString() ?: throw IllegalArgumentException("The key is invalid")
//            val s = iterator.asSequence().takeWhile { it != '"' }.joinToString("")
            iterator.next() // to consume the [
            println("Key token ==> "+s)
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
        println("String token ==> " + s)
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
        println("Double token ==> " + (c + s))
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

fun tokenize2(s: String) : List<Token> {
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