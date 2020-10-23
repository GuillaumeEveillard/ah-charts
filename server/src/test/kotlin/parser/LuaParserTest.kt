package parser

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class LuaParserTest {

    @Test
    fun `parse simple object`() {
        val tokens = listOf(ObjectStart, Key("faction"), Assignment, StringToken("Alliance"), Key("realm"), Assignment, StringToken("Sulfuron"), ObjectEnd)
        val ast = buildAst(tokens)
        Assertions.assertEquals(
            LuaObject(
                KV("faction", StringLiteral("Alliance")),
                KV("realm", StringLiteral("Sulfuron"))),
            ast)
    }

    @Test
    fun `parse nested object`() {
        val tokens = listOf(ObjectStart, Key("key"), Assignment, ObjectStart, Key("sub-key"), Assignment, StringToken("value"), ObjectEnd, ObjectEnd)
        val ast = buildAst(tokens)
        Assertions.assertEquals(
            LuaObject(
            KV("key", LuaObject(
                KV("sub-key", StringLiteral("value"))))),
            ast)
    }

    @Test
    fun `parse object with list`() {
        val tokens = listOf(ObjectStart,
                Key("key"), Assignment, ObjectStart,
                ObjectStart, Key("sub-key-1"), Assignment, StringToken("value1"),ObjectEnd,
                ObjectStart, Key("sub-key-2"), Assignment, DoubleToken(2.0),ObjectEnd,
                ObjectEnd)
        val ast = buildAst(tokens)
        Assertions.assertEquals(
            LuaObject(
                KV("key", LuaObject(
                    LuaObject(KV("sub-key-1", StringLiteral("value1"))), //TODO this can be simplify
                    LuaObject(KV("sub-key-2", DoubleLiteral(2.0)))))),
            ast)
    }
}