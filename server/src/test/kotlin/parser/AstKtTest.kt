package parser

import org.junit.jupiter.api.Test

class AstKtTest {
    @Test
    fun `auctionator snapshot`() {
        val input = """
{
	["faction"] = "Alliance",
	["realm"] = "Sulfuron",
	["auctions"] = {
		{
			["q"] = 1,
			["s"] = 7695,
			["b"] = 8100,
			["i"] = 4625,
			["c"] = 0,
			["o"] = "",
			["n"] = "Firebloom",
		}, 
		{
			["q"] = 1,
			["s"] = 1497,
			["b"] = 1663,
			["i"] = 4382,
			["c"] = 0,
			["o"] = "",
			["n"] = "Bronze Framework",
		}
    },
}"""

        val tokens = tokenize(input)
        val ast = buildAst(tokens)
        
        val json = ast.toJson()
        
        val expect = "";
    }
}