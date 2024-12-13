package com.kengine.map.tiled

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// For references in the map file
@Serializable
data class TilesetReference(
    val firstgid: Int,
    val source: String? = null,
    // For embedded tilesets
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
    var tileWidth: Int? = null
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

// for the actual tileset data in the .tsj/.tsx file
//@Serializable
//data class Tileset(
//    @SerialName("tilewidth")
//    val tileWidth: Int,
//    @SerialName("tileheight")
//    val tileHeight: Int,
//    @SerialName("imagewidth")
//    val imageWidth: Int,
//    @SerialName("imageheight")
//    val imageHeight: Int,
//    val image: String,
//    val columns: Int,
//    @SerialName("tilecount")
//    val tileCount: Int,
//    val margin: Int = 0,
//    val spacing: Int = 0,
//    val name: String
//)