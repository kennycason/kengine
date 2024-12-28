open class KengineNativeConfig {
    var modules: Set<KengineModule> = setOf(*KengineModule.values()) // Default all modules
    var entryPoint: String = "main"
}
