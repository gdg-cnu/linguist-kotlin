package io.github.shapelayer.linguist

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

@Serializable
data class LanguageExtensions(
    val extensions: Map<String, String>
)

data class LanguageStat(
    val language: String,
    var linesOfCode: Int = 0
)

class Linguist(lookupTargetPath: String?, ext: Map<String, String>?) {
    var lookupTargetPath: String?
    val extensionMap: Map<String, String>

    companion object {
        val resourcePath = "config/languages.json"
    }

    init {
        if (ext != null) {
            this.extensionMap = ext
        } else {
            val languageExtensionSerialized: String? = {}.javaClass.classLoader.getResource(resourcePath)?.readText()
            if (languageExtensionSerialized == null) {
                throw Error("Required resource `languages.json` is not exists.")
            }
            this.extensionMap = _parseLanguageMap(languageExtensionSerialized);
        }

        this.lookupTargetPath = lookupTargetPath
    }

    constructor(lookupTargetPath: String?): this(lookupTargetPath, null)

    private fun _parseLanguageMap(content: String): Map<String, String> {
        val json = Json { ignoreUnknownKeys = true }
        val parsed = json.decodeFromString<LanguageExtensions>(content)
        return parsed.extensions
    }

    private fun _getFileExtension(file: File): String? {
        return file.extension.takeIf { it.isNotBlank() }
    }
    private fun _countLines(file: File): Int {
        return file.useLines { it.count() }
    }

    private fun _scanDirectory(directory: File, languageExtensions: Map<String, String>): List<LanguageStat> {
        val languageStats = mutableMapOf<String, LanguageStat>()

        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                val extension = _getFileExtension(file)
                val language = languageExtensions[extension]

                if (language != null) {
                    val lines = _countLines(file)
                    languageStats[language]?.let {
                        it.linesOfCode += lines
                    } ?: run {
                        languageStats[language] = LanguageStat(language, lines)
                    }
                }
            }
        }
        return languageStats.values.toList()
    }

    private fun _loadLookupTarget(lookupTargetPath: String?): File {
        if (this.lookupTargetPath == null) {
            if (lookupTargetPath == null) {
                throw Error()
            }
            this.lookupTargetPath = lookupTargetPath
        }

        val lookupTarget = File(this.lookupTargetPath)
        if (!lookupTarget.exists() || !lookupTarget.isDirectory) {
            throw Error("$this.lookupTargetPath is not valid path.")
        }
        return lookupTarget
    }

    fun evaluate(): List<LanguageStat> {
        return this.evaluate(null)
    }
    fun evaluate(lookupTargetPath: String?): List<LanguageStat> {
        val lookupTarget = _loadLookupTarget(lookupTargetPath)

        return _scanDirectory(lookupTarget, this.extensionMap)
    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        println("Usage: linguist <path>")
        return
    }
    val lookupTargetPath: String = args[0]

    val linguist: Linguist = Linguist(lookupTargetPath)

    val languageStats: List<LanguageStat> = linguist.evaluate()
    val totalLines: Int = languageStats.sumOf { it.linesOfCode }

    println("Language Statistics:")
    languageStats.forEach { stat ->
        val percentage = (stat.linesOfCode.toDouble() / totalLines) * 100
        println("${stat.language}: ${stat.linesOfCode} lines (${String.format("%.2f", percentage)}%)")
    }
}
