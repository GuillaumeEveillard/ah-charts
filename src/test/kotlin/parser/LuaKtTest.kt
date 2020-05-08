package parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import transformLuaDataStructureToJson

internal class LuaKtTest {
    
    @Test
    fun `simple object`() {
        val lua = """{ ["faction"] = "Alliance", ["realm"] = "Sulfuron", }"""
        val json = transformLuaDataStructureToJson(lua)
        
        val expected = """{"faction": "Alliance", "real": "Sulfuron"}"""

        JSONAssert.assertEquals(expected, json, JSONCompareMode.STRICT)
    }

    @Test
    fun `simple object multiline`() {
        val lua = """
{ 
    ["faction"] = "Alliance", 
    ["realm"] = "Sulfuron",
}""".trimIndent()
        val json = transformLuaDataStructureToJson(lua)

        val expected = """{"faction": "Alliance", "real": "Sulfuron"}"""

        JSONAssert.assertEquals(expected, json, JSONCompareMode.STRICT)
    }

    @Test
    fun `nested object`() {

    }
    
    @Test
    fun `auctionator snapshot`() {
    """
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
		}, -- [1]
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
    }
    
    @Test
    fun `ark inventory db`() {
        val x = """
{
	["global"] = {
		["player"] = {
			["data"] = {
				["Directcompo - Sulfuron"] = {
					["info"] = {
						["guid"] = "Player-4464-01314EDF",
						["class"] = "PALADIN",
						["player_id"] = "Directcompo - Sulfuron",
						["race"] = "Human",
						["proj"] = 2,
						["realm"] = "Sulfuron",
						["isplayer"] = true,
						["money"] = 199403,
						["gender"] = 3,
						["level"] = 1,
						["name"] = "Directcompo",
						["faction"] = "Alliance",
						["race_local"] = "Human",
						["class_local"] = "Paladin",
						["faction_local"] = "Alliance",
					},
					["location"] = {
						{
							["bag"] = {
								{
									["q"] = 0,
									["type"] = 1,
									["slot"] = {
										{
											["q"] = 2,
											["loc_id"] = 1,
											["slot_id"] = 1,
											["sb"] = 0,
											["count"] = 1,
											["bag_id"] = 1,
											["age"] = 26387779,
											["h"] = "|cff1eff00|Hitem:7969::::::::1:::::::|h[Mithril Spurs]|h|r",
										}, -- [1]
										{
											["q"] = 2,
											["loc_id"] = 1,
											["slot_id"] = 2,
											["sb"] = 0,
											["count"] = 1,
											["bag_id"] = 1,
											["age"] = 26387779,
											["h"] = "|cff1eff00|Hitem:7969::::::::1:::::::|h[Mithril Spurs]|h|r",
										},
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
        """.trimIndent()
    }
}