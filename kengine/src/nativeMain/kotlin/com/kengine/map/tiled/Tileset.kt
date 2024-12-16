package com.kengine.map.tiled

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO add documentation/comments
@Serializable
data class Tileset(
    val firstgid: UInt,
    val source: String? = null,
    var columns: Int? = null,
    var image: String? = null,
    @SerialName("imageheight")
    var imageHeight: Int? = null,
    @SerialName("imagewidth")
    var imageWidth: Int? = null,
    var margin: Int = 0,
    var name: String? = null,
    var spacing: Int = 0,
    @SerialName("tilecount")
    var tileCount: Int? = null,
    @SerialName("tileheight")
    var tileHeight: Int? = null,
    @SerialName("tilewidth")
    var tileWidth: Int? = null,
    var tiles: List<TileData>? = null
) {
    fun isExternal() = source != null
}

// For the .tsj file format
@Serializable
data class TilesetData(
    val columns: Int,
    val image: String,
    @SerialName("imageheight")
    val imageHeight: Int,
    @SerialName("imagewidth")
    val imageWidth: Int,
    val margin: Int = 0,
    val name: String,
    val spacing: Int = 0,
    @SerialName("tilecount")
    val tileCount: Int,
    @SerialName("tileheight")
    val tileHeight: Int,
    @SerialName("tilewidth")
    val tileWidth: Int,
    val tiles: List<TileData>? = null
)

@Serializable
data class TileData(
    val id: Int,
    val animation: List<Frame>? = null,
    val properties: List<TiledObject.Property>? = null
)

@Serializable
data class Frame(
    val duration: Int,
    val tileid: Int
)
