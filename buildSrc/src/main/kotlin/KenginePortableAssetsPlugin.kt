import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class KenginePortableAssetsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<KenginePortableAssetsExtension>(
            "kenginePortableAssets",
            project
        )
        val generatedSourceDir = project.layout.buildDirectory.dir("generated/kenginePortableAssets/commonMain/kotlin")
        val generatedSourceFile = extension.packageName.flatMap { packageName ->
            extension.objectName.map { objectName ->
                val packagePath = packageName.replace('.', '/')
                generatedSourceDir.get().file("$packagePath/$objectName.kt")
            }
        }
        val generateTask = project.tasks.register<KengineGeneratePortableAssetsTask>("generatePortableAssetCatalog") {
            group = "build"
            description = "Generates a small common Kotlin catalog for portable game assets."

            this.extension = extension
            outputFile.set(generatedSourceFile)
            inputs.property("packageName", extension.packageName)
            inputs.property("objectName", extension.objectName)
            inputs.property("assets", project.providers.provider {
                extension.assets.joinToString("|") { asset ->
                    listOf(
                        asset.name,
                        asset.id.orNull.orEmpty(),
                        asset.source.orNull?.asFile?.relativeToOrSelf(project.projectDir)?.invariantSeparatorsPath.orEmpty(),
                        (asset as? KenginePortableSpriteSheetAsset)?.tileWidth?.orNull ?: 0,
                        (asset as? KenginePortableSpriteSheetAsset)?.tileHeight?.orNull ?: 0,
                        (asset as? KenginePortableSpriteSheetAsset)?.columns?.orNull ?: 0,
                        when (asset) {
                            is KenginePortableImageAsset -> "sprite"
                            is KenginePortableSpriteSheetAsset -> "spriteSheet"
                            is KenginePortableMusicAsset -> "music"
                        }
                    ).joinToString(":")
                }
            })
        }

        extension.generatedSourceDir = generatedSourceDir
        extension.generateTaskPath = generateTask.map { it.path }.get()

        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            addCommonMainSourceDir(project, generatedSourceDir)
            project.tasks.matching { task ->
                task.name.startsWith("compile") && task.name.contains("Kotlin")
            }.configureEach {
                dependsOn(generateTask)
            }
        }
    }

    private fun addCommonMainSourceDir(project: Project, generatedSourceDir: Any) {
        val kotlin = project.extensions.findByName("kotlin") ?: return
        val sourceSets = kotlin.javaClass.methods
            .first { it.name == "getSourceSets" && it.parameterCount == 0 }
            .invoke(kotlin)
        val commonMain = sourceSets.javaClass.methods
            .first { it.name == "getByName" && it.parameterCount == 1 }
            .invoke(sourceSets, "commonMain")
        val kotlinSources = commonMain.javaClass.methods
            .first { it.name == "getKotlin" && it.parameterCount == 0 }
            .invoke(commonMain)
        kotlinSources.javaClass.methods
            .first { it.name == "srcDir" && it.parameterCount == 1 }
            .invoke(kotlinSources, generatedSourceDir)
    }
}

abstract class KengineGeneratePortableAssetsTask : DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    lateinit var extension: KenginePortableAssetsExtension

    @get:org.gradle.api.tasks.OutputFile
    abstract val outputFile: org.gradle.api.file.RegularFileProperty

    init {
        outputs.cacheIf { true }
    }

    @TaskAction
    fun generate() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()

        val packageName = extension.packageName.get()
        val objectName = extension.objectName.get()
        val assets = extension.assets
        val imageAssets = assets.filterIsInstance<KenginePortableImageAsset>()
        val spriteSheets = assets.filterIsInstance<KenginePortableSpriteSheetAsset>()
        val musicAssets = assets.filterIsInstance<KenginePortableMusicAsset>()

        output.writeText(
            buildString {
                appendLine("package $packageName")
                appendLine()
                appendLine("import com.kengine.assets.PortableAssetCatalog")
                appendLine("import com.kengine.assets.PortableMusicAsset")
                appendLine("import com.kengine.assets.PortableSpriteAsset")
                appendLine("import com.kengine.assets.PortableSpriteSheetAsset")
                appendLine()
                appendLine("object $objectName : PortableAssetCatalog {")
                assets.forEach { asset ->
                    appendAssetConstants(asset)
                }
                appendLine()
                if (imageAssets.isEmpty()) {
                    appendLine("    override val sprites = emptyList<PortableSpriteAsset>()")
                } else {
                    appendLine("    override val sprites = listOf(")
                    imageAssets.forEach { asset ->
                        appendLine("        PortableSpriteAsset(${assetConstantName(asset)}_ID, ${assetConstantName(asset)}_SOURCE),")
                    }
                    appendLine("    )")
                }
                appendLine()
                if (spriteSheets.isEmpty()) {
                    appendLine("    override val spriteSheets = emptyList<PortableSpriteSheetAsset>()")
                } else {
                    appendLine("    override val spriteSheets = listOf(")
                    spriteSheets.forEach { asset ->
                        val constant = assetConstantName(asset)
                        appendLine(
                            "        PortableSpriteSheetAsset(" +
                                "${constant}_ID, " +
                                "${constant}_SOURCE, " +
                                "${constant}_TILE_WIDTH, " +
                                "${constant}_TILE_HEIGHT, " +
                                "${constant}_COLUMNS" +
                                "),"
                        )
                    }
                    appendLine("    )")
                }
                appendLine()
                if (musicAssets.isEmpty()) {
                    appendLine("    override val music = emptyList<PortableMusicAsset>()")
                } else {
                    appendLine("    override val music = listOf(")
                    musicAssets.forEach { asset ->
                        appendLine("        PortableMusicAsset(${assetConstantName(asset)}_ID, ${assetConstantName(asset)}_SOURCE),")
                    }
                    appendLine("    )")
                }
                appendLine("}")
            }
        )
    }

    private fun StringBuilder.appendAssetConstants(asset: KenginePortableAsset) {
        val constant = assetConstantName(asset)
        appendLine("    const val ${constant}_ID = ${asset.id.get().quoted()}")
        appendLine("    const val ${constant}_SOURCE = ${relativeAssetPath(asset.source).quoted()}")
        if (asset is KenginePortableSpriteSheetAsset) {
            appendLine("    const val ${constant}_TILE_WIDTH = ${asset.tileWidth.required(asset.name, "tileWidth")}")
            appendLine("    const val ${constant}_TILE_HEIGHT = ${asset.tileHeight.required(asset.name, "tileHeight")}")
            appendLine("    const val ${constant}_COLUMNS = ${asset.columns.orNull ?: 0}")
        }
    }

    private fun relativeAssetPath(source: org.gradle.api.file.RegularFileProperty): String {
        val file = source.orNull?.asFile
            ?: throw GradleException("Portable asset source must be configured.")
        return file.relativeToOrSelf(project.projectDir).invariantSeparatorsPath
    }

    private fun assetConstantName(asset: KenginePortableAsset): String {
        return asset.name.uppercase()
            .map { char -> if (char in 'A'..'Z' || char in '0'..'9') char else '_' }
            .joinToString("")
            .trim('_')
            .ifEmpty { "ASSET" }
    }

    private fun org.gradle.api.provider.Property<Int>.required(assetName: String, propertyName: String): Int {
        val value = orNull
            ?: throw GradleException("Sprite sheet '$assetName' must configure $propertyName.")
        if (value <= 0) {
            throw GradleException("Sprite sheet '$assetName' $propertyName must be positive.")
        }
        return value
    }

    private fun String.quoted(): String = buildString {
        append('"')
        for (char in this@quoted) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
}
