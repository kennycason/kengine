import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.Property

open class KenginePortableAssetsExtension(private val project: Project) {
    private val mutableAssets = mutableListOf<KenginePortableAsset>()
    var generatedSourceDir: Provider<Directory>? = null
        internal set
    var generateTaskPath: String? = null
        internal set

    val packageName: Property<String> = project.objects.property(String::class.java)
        .convention(project.name.replace('-', '.'))
    val objectName: Property<String> = project.objects.property(String::class.java)
        .convention("GameAssets")
    val assets: List<KenginePortableAsset>
        get() = mutableAssets.toList()

    fun sprite(name: String, configure: Action<KenginePortableImageAsset>) {
        val asset = KenginePortableImageAsset(name, project)
        configure.execute(asset)
        mutableAssets += asset
    }

    fun spriteSheet(name: String, configure: Action<KenginePortableSpriteSheetAsset>) {
        val asset = KenginePortableSpriteSheetAsset(name, project)
        configure.execute(asset)
        mutableAssets += asset
    }

    fun music(name: String, configure: Action<KenginePortableMusicAsset>) {
        val asset = KenginePortableMusicAsset(name, project)
        configure.execute(asset)
        mutableAssets += asset
    }
}

sealed class KenginePortableAsset(
    private val assetName: String,
    project: Project
) : Named {
    val id: Property<String> = project.objects.property(String::class.java)
        .convention(assetName)
    val source: RegularFileProperty = project.objects.fileProperty()

    override fun getName(): String = assetName
}

open class KenginePortableImageAsset(
    name: String,
    project: Project
) : KenginePortableAsset(name, project)

open class KenginePortableSpriteSheetAsset(
    name: String,
    project: Project
) : KenginePortableAsset(name, project) {
    val tileWidth: Property<Int> = project.objects.property(Int::class.javaObjectType)
    val tileHeight: Property<Int> = project.objects.property(Int::class.javaObjectType)
    val columns: Property<Int> = project.objects.property(Int::class.javaObjectType)
}

open class KenginePortableMusicAsset(
    name: String,
    project: Project
) : KenginePortableAsset(name, project)
