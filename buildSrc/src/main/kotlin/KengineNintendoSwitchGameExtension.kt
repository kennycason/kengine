import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

open class KengineNintendoSwitchGameExtension(project: Project) {
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
    val blockSpriteSheetSource: RegularFileProperty = project.objects.fileProperty()
}
