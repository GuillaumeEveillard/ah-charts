package parser

import org.junit.jupiter.api.Test

internal class LuaTokenizerTest {
    @Test
    fun aa() {
        tokenize("salut = fer { fd }{ [toto]= \"string\" 500= true false 2")
    }
}
