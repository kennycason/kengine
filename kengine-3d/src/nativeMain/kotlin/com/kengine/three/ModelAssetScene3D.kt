package com.kengine.three

fun ModelAssetSourceBundle3D.createTerrainCollider(asset: ModelAsset3D): TerrainMeshCollider3D {
    return modelSource(asset).createTerrainCollider()
}

fun LoadedModelAssetBundle3D.createTerrainCollider(asset: ModelAsset3D): TerrainMeshCollider3D {
    return modelSource(asset).createTerrainCollider()
}

fun ModelAssetSourceBundle3D.createStaticCollider(asset: ModelAsset3D): StaticMeshCollider3D {
    return modelSource(asset).createStaticCollider()
}

fun LoadedModelAssetBundle3D.createStaticCollider(asset: ModelAsset3D): StaticMeshCollider3D {
    return modelSource(asset).createStaticCollider()
}

fun Scene3D.addModelAssetNode(
    bundle: LoadedModelAssetBundle3D,
    asset: ModelAsset3D,
    transform: Transform3D = Transform3D(),
    itemTransform: Transform3D = Transform3D(),
    light: DirectionalLight3D? = null,
    isVisible: Boolean = true
): Node3D<SceneModel3D> {
    return addModelNode(
        model = bundle.model(asset),
        transform = transform,
        itemTransform = itemTransform,
        light = light,
        isVisible = isVisible
    )
}

fun Scene3D.addAnimatedModelAssetNode(
    bundle: LoadedModelAssetBundle3D,
    asset: AnimatedModelAsset3D,
    transform: Transform3D = Transform3D(),
    itemTransform: Transform3D = Transform3D(),
    pose: AnimationPose3D = AnimationPose3D(),
    light: DirectionalLight3D? = null,
    isVisible: Boolean = true
): Node3D<AnimatedModelInstance3D> {
    return addAnimatedModelNode(
        model = bundle.animatedModel(asset),
        transform = transform,
        itemTransform = itemTransform,
        pose = pose,
        light = light,
        isVisible = isVisible
    )
}
