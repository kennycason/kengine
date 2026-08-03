package com.kengine.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RenderAssetIdTest {
    @Test
    fun createsStableSpriteIds() {
        assertEquals(1145756846, RenderAssetId.sprite("demo/pokeball"))
        assertEquals(RenderAssetId.sprite("demo/pokeball"), RenderAssetId.sprite("demo/pokeball"))
        assertNotEquals(RenderAssetId.sprite("demo/pokeball"), RenderAssetId.sprite("demo/other"))
    }
}
