import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

open class KengineN64GameExtension(private val project: Project) {
    private val mutableSpriteAssets = mutableListOf<KengineN64SpriteAsset>()
    private val mutableSoundAssets = mutableListOf<KengineN64SoundAsset>()
    private val mutableMusicAssets = mutableListOf<KengineN64SoundAsset>()
    private val mutableGameSourceProjects = mutableListOf<Project>()

    val artifactBaseName: Property<String> = project.objects.property(String::class.java)
        .convention(project.name)
    val displayName: Property<String> = project.objects.property(String::class.java)
        .convention(project.name.split('-', '_').joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } })
    val author: Property<String> = project.objects.property(String::class.java)
        .convention("kengine")
    val version: Property<String> = project.objects.property(String::class.java)
        .convention(project.version.toString().takeIf { it != "unspecified" } ?: "1.0.0")
    val mainClass: Property<String> = project.objects.property(String::class.java)
    val backendBuildTaskName: Property<String> = project.objects.property(String::class.java)
    val spriteAssets: List<KengineN64SpriteAsset>
        get() = mutableSpriteAssets.toList()
    val soundAssets: List<KengineN64SoundAsset>
        get() = mutableSoundAssets.toList()
    val musicAssets: List<KengineN64SoundAsset>
        get() = mutableMusicAssets.toList()
    val gameSourceProjects: List<Project>
        get() = mutableGameSourceProjects.toList()

    fun sprite(name: String, configure: Action<KengineN64ImageAsset>) {
        val asset = KengineN64ImageAsset(name, project)
        configure.execute(asset)
        mutableSpriteAssets += asset
    }

    fun spriteSheet(name: String, configure: Action<KengineN64SpriteSheetAsset>) {
        val asset = KengineN64SpriteSheetAsset(name, project)
        configure.execute(asset)
        mutableSpriteAssets += asset
    }

    fun sound(name: String, configure: Action<KengineN64SoundAsset>) {
        val asset = KengineN64SoundAsset(name, project)
        configure.execute(asset)
        mutableSoundAssets += asset
    }

    fun music(name: String, configure: Action<KengineN64SoundAsset>) {
        val asset = KengineN64SoundAsset(name, project)
        configure.execute(asset)
        mutableMusicAssets += asset
    }

    fun gameSourceProject(sourceProject: Project) {
        project.evaluationDependsOn(sourceProject.path)
        mutableGameSourceProjects += sourceProject
    }

    fun assetsFrom(sourceProject: Project) {
        project.evaluationDependsOn(sourceProject.path)
        val portableAssets = sourceProject.extensions.findByName("kenginePortableAssets") as? KenginePortableAssetsExtension
            ?: throw GradleException("${sourceProject.path} must apply kengine.portable-assets before assetsFrom can be used.")

        portableAssets.assets.forEach { portableAsset ->
            when (portableAsset) {
                is KenginePortableImageAsset -> {
                    val asset = KengineN64ImageAsset(portableAsset.name, project)
                    asset.id.set(portableAsset.id)
                    asset.source.set(portableAsset.source)
                    mutableSpriteAssets += asset
                }
                is KenginePortableSpriteSheetAsset -> {
                    val asset = KengineN64SpriteSheetAsset(portableAsset.name, project)
                    asset.id.set(portableAsset.id)
                    asset.source.set(portableAsset.source)
                    asset.tileWidth.set(portableAsset.tileWidth)
                    asset.tileHeight.set(portableAsset.tileHeight)
                    asset.columns.set(portableAsset.columns)
                    mutableSpriteAssets += asset
                }
                is KenginePortableSoundAsset -> {
                    val asset = KengineN64SoundAsset(portableAsset.name, project)
                    asset.id.set(portableAsset.id)
                    asset.source.set(portableAsset.source)
                    mutableSoundAssets += asset
                }
                is KenginePortableMusicAsset -> {
                    // Music is memory-expensive on N64; declare only needed tracks explicitly with music(...).
                }
            }
        }
    }
}

sealed class KengineN64SpriteAsset(
    private val assetName: String,
    project: Project
) : Named {
    val id: Property<String> = project.objects.property(String::class.java)
        .convention(assetName)
    val source: RegularFileProperty = project.objects.fileProperty()
    val extraInputs: ConfigurableFileCollection = project.objects.fileCollection()

    override fun getName(): String = assetName
}

open class KengineN64ImageAsset(
    name: String,
    project: Project
) : KengineN64SpriteAsset(name, project)

open class KengineN64SpriteSheetAsset(
    name: String,
    project: Project
) : KengineN64SpriteAsset(name, project) {
    val tileWidth: Property<Int> = project.objects.property(Int::class.javaObjectType)
    val tileHeight: Property<Int> = project.objects.property(Int::class.javaObjectType)
    val columns: Property<Int> = project.objects.property(Int::class.javaObjectType)
}

open class KengineN64SoundAsset(
    private val assetName: String,
    project: Project
) : Named {
    val id: Property<String> = project.objects.property(String::class.java)
        .convention(assetName)
    val source: RegularFileProperty = project.objects.fileProperty()
    val extraInputs: ConfigurableFileCollection = project.objects.fileCollection()

    override fun getName(): String = assetName
}
