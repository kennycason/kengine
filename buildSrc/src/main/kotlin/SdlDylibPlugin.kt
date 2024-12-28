import org.gradle.api.Plugin
import org.gradle.api.Project

class SdlDylibPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            SdlDylibCopier(project).registerSDLDylibs()
        }
    }
}
