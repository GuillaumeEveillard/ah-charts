package parser

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class LuaTokenizerTest {

    @Test
    fun `string token`() {
        Assertions.assertEquals(listOf(StringToken("salut")), tokenize("\"salut\""))
    }

    @Test
    fun `boolean token`() {
        Assertions.assertEquals(listOf(BooleanToken(true)), tokenize("true"))
    }

    @Test
    fun `double token`() {
        Assertions.assertEquals(listOf(DoubleToken(250.0)), tokenize("250.0"))
    }

    @Test
    fun `start object token`() {
        Assertions.assertEquals(listOf(ObjectStart), tokenize("{"))
    }

    @Test
    fun `end object token`() {
        Assertions.assertEquals(listOf(ObjectEnd), tokenize("}"))
    }

    @Test
    fun `key token`() {
        Assertions.assertEquals(listOf(Key("my-key")), tokenize("""["my-key"]"""))
    }

    @Test
    fun `assignment token`() {
        Assertions.assertEquals(listOf(Assignment), tokenize("="))
    }

    @Test
    fun `simple object`() {
        val tokens = tokenize("""{ ["faction"] = "Alliance", ["realm"] = "Sulfuron", }""")
        Assertions.assertEquals(listOf(ObjectStart, Key("faction"), Assignment, StringToken("Alliance"), Key("realm"), Assignment, StringToken("Sulfuron"), ObjectEnd), tokens)
    }

    @Test
    fun `nested object`() {
        val tokens = tokenize("""{ ["key"] = { ["sub-key"] = "value" } }""")
        Assertions.assertEquals(listOf(ObjectStart, Key("key"), Assignment, ObjectStart, Key("sub-key"), Assignment, StringToken("value"), ObjectEnd, ObjectEnd), tokens)
    }

    @Test
    fun `object with list`() {
        val tokens = tokenize("""{ ["key"] = { { ["sub-key-1"] = "value1" },  { ["sub-key-2"] = 2 }}""")
        Assertions.assertEquals(listOf(ObjectStart, 
            Key("key"), Assignment, ObjectStart, 
                ObjectStart, Key("sub-key-1"), Assignment, StringToken("value1"), ObjectEnd, 
                ObjectStart, Key("sub-key-2"), Assignment, DoubleToken(2.0), ObjectEnd,
            ObjectEnd), tokens)
    }
}
