package com.kengine.map.tiled

class XmlElement(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<XmlElement>,
    val text: String
) {
    fun child(name: String): XmlElement? = children.firstOrNull { it.name == name }
    fun childrenByName(name: String): List<XmlElement> = children.filter { it.name == name }
    fun attr(name: String): String? = attributes[name]
    fun attrInt(name: String): Int? = attributes[name]?.toIntOrNull()
    fun attrFloat(name: String): Float? = attributes[name]?.toFloatOrNull()
    fun attrDouble(name: String): Double? = attributes[name]?.toDoubleOrNull()

    companion object {
        fun parse(xml: String): XmlElement {
            val parser = XmlParser(xml)
            return parser.parseDocument()
        }
    }
}

private class XmlParser(private val input: String) {
    private var pos = 0

    fun parseDocument(): XmlElement {
        skipWhitespace()
        if (input.startsWith("<?", pos)) skipProcessingInstruction()
        skipWhitespace()
        return parseElement()
    }

    private fun parseElement(): XmlElement {
        expect('<')
        val name = readName()
        val attributes = mutableMapOf<String, String>()

        while (true) {
            skipWhitespace()
            if (peek() == '/' && input[pos + 1] == '>') {
                pos += 2
                return XmlElement(name, attributes, emptyList(), "")
            }
            if (peek() == '>') {
                pos++
                break
            }
            val (key, value) = readAttribute()
            attributes[key] = value
        }

        val children = mutableListOf<XmlElement>()
        val textBuilder = StringBuilder()

        while (true) {
            if (input.startsWith("</", pos)) {
                pos += 2
                val closeName = readName()
                skipWhitespace()
                expect('>')
                return XmlElement(name, attributes, children, textBuilder.toString().trim())
            }
            if (peek() == '<') {
                children.add(parseElement())
            } else {
                textBuilder.append(input[pos])
                pos++
            }
        }
    }

    private fun readName(): String {
        val start = pos
        while (pos < input.length && input[pos].isNameChar()) pos++
        return input.substring(start, pos)
    }

    private fun readAttribute(): Pair<String, String> {
        val key = readName()
        skipWhitespace()
        expect('=')
        skipWhitespace()
        val quote = input[pos]
        pos++
        val start = pos
        while (input[pos] != quote) pos++
        val value = input.substring(start, pos)
        pos++
        return key to decodeXmlEntities(value)
    }

    private fun decodeXmlEntities(s: String): String {
        if ('&' !in s) return s
        return s.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    private fun skipProcessingInstruction() {
        while (!input.startsWith("?>", pos)) pos++
        pos += 2
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun peek(): Char = input[pos]

    private fun expect(c: Char) {
        if (input[pos] != c) throw IllegalArgumentException("Expected '$c' at position $pos, got '${input[pos]}'")
        pos++
    }

    private fun Char.isNameChar(): Boolean =
        isLetterOrDigit() || this == '_' || this == '-' || this == '.' || this == ':'
}
