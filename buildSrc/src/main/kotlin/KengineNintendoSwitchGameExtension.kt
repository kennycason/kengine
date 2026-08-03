import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

open class KengineNintendoSwitchGameExtension(private val project: Project) {
    private val mutableSpriteAssets = mutableListOf<KengineNintendoSwitchSpriteAsset>()
    private val mutableSoundAssets = mutableListOf<KengineNintendoSwitchSoundAsset>()
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
    val cDefines: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(emptyList())
    val musicSource: RegularFileProperty = project.objects.fileProperty()
    val iconSource: RegularFileProperty = project.objects.fileProperty()
    val spriteAssets: List<KengineNintendoSwitchSpriteAsset>
        get() = mutableSpriteAssets.toList()
    val soundAssets: List<KengineNintendoSwitchSoundAsset>
        get() = mutableSoundAssets.toList()
    val gameSourceProjects: List<Project>
        get() = mutableGameSourceProjects.toList()

    fun sprite(name: String, configure: Action<KengineNintendoSwitchImageAsset>) {
        val asset = KengineNintendoSwitchImageAsset(name, project)
        configure.execute(asset)
        mutableSpriteAssets += asset
    }

    fun spriteSheet(name: String, configure: Action<KengineNintendoSwitchSpriteSheetAsset>) {
        val asset = KengineNintendoSwitchSpriteSheetAsset(name, project)
        configure.execute(asset)
        mutableSpriteAssets += asset
    }

    fun sound(name: String, configure: Action<KengineNintendoSwitchSoundAsset>) {
        val asset = KengineNintendoSwitchSoundAsset(name, project)
        configure.execute(asset)
        mutableSoundAssets += asset
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
                    val asset = KengineNintendoSwitchImageAsset(portableAsset.name, project)
                    asset.id.set(portableAsset.id)
                    asset.source.set(portableAsset.source)
                    mutableSpriteAssets += asset
                }
                is KenginePortableSpriteSheetAsset -> {
                    val asset = KengineNintendoSwitchSpriteSheetAsset(portableAsset.name, project)
                    asset.id.set(portableAsset.id)
                    asset.source.set(portableAsset.source)
                    asset.tileWidth.set(portableAsset.tileWidth)
                    asset.tileHeight.set(portableAsset.tileHeight)
                    asset.columns.set(portableAsset.columns)
                    mutableSpriteAssets += asset
                }
                is KenginePortableMusicAsset -> {
                    if (!musicSource.isPresent) {
                        musicSource.set(portableAsset.source)
                    }
                }
                is KenginePortableSoundAsset -> {
                    val asset = KengineNintendoSwitchSoundAsset(portableAsset.name, project)
                    asset.id.set(portableAsset.id)
                    asset.source.set(portableAsset.source)
                    mutableSoundAssets += asset
                }
            }
        }
    }
}

sealed class KengineNintendoSwitchSpriteAsset(
    private val assetName: String,
    project: Project
) : Named {
    val id: Property<String> = project.objects.property(String::class.java)
        .convention(assetName)
    val source: RegularFileProperty = project.objects.fileProperty()
    val extraInputs: ConfigurableFileCollection = project.objects.fileCollection()

    override fun getName(): String = assetName
}

open class KengineNintendoSwitchImageAsset(
    name: String,
    project: Project
) : KengineNintendoSwitchSpriteAsset(name, project)

open class KengineNintendoSwitchSpriteSheetAsset(
    name: String,
    project: Project
) : KengineNintendoSwitchSpriteAsset(name, project) {
    val tileWidth: Property<Int> = project.objects.property(Int::class.javaObjectType)
    val tileHeight: Property<Int> = project.objects.property(Int::class.javaObjectType)
    val columns: Property<Int> = project.objects.property(Int::class.javaObjectType)
}

open class KengineNintendoSwitchSoundAsset(
    private val assetName: String,
    project: Project
) : Named {
    val id: Property<String> = project.objects.property(String::class.java)
        .convention(assetName)
    val source: RegularFileProperty = project.objects.fileProperty()
    val extraInputs: ConfigurableFileCollection = project.objects.fileCollection()

    override fun getName(): String = assetName
}
