#!/usr/bin/env python3
import argparse
import copy
import json
import struct
from pathlib import Path


GLB_MAGIC = b"glTF"
GLB_VERSION_2 = 2
JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942

COMPONENT_SIZES = {
    5120: 1,
    5121: 1,
    5122: 2,
    5123: 2,
    5125: 4,
    5126: 4,
}

TYPE_COMPONENT_COUNTS = {
    "SCALAR": 1,
    "VEC2": 2,
    "VEC3": 3,
    "VEC4": 4,
    "MAT2": 4,
    "MAT3": 9,
    "MAT4": 16,
}

DEFAULT_ANIMATIONS = [
    "Armature|AreaWait64",
    "Armature|Walk",
    "Armature|Run",
    "Armature|Jump",
    "Armature|Fall",
    "Armature|Land",
    "Armature|HipDropStart",
    "Armature|HipDrop",
    "Armature|HipDropLand",
    "Armature|Punch",
    "Armature|Damage",
    "Armature|SquatStart",
    "Armature|SquatWait",
    "Armature|SquatEnd",
    "Armature|Brake",
]


def read_glb(path):
    data = Path(path).read_bytes()
    if len(data) < 12 or data[:4] != GLB_MAGIC:
        raise ValueError(f"{path} is not a GLB file")
    version, total_length = struct.unpack_from("<II", data, 4)
    if version != GLB_VERSION_2:
        raise ValueError(f"{path} is GLB version {version}; only GLB 2.0 is supported")
    if total_length != len(data):
        raise ValueError(f"{path} has a mismatched GLB length header")

    document = None
    binary = None
    offset = 12
    while offset + 8 <= len(data):
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        chunk = data[offset:offset + chunk_length]
        offset += chunk_length
        if chunk_type == JSON_CHUNK:
            document = json.loads(chunk.rstrip(b" \t\r\n\0").decode("utf-8"))
        elif chunk_type == BIN_CHUNK:
            binary = chunk

    if document is None:
        raise ValueError(f"{path} has no JSON chunk")
    if binary is None:
        raise ValueError(f"{path} has no BIN chunk")
    return document, binary


def pad4(data, byte):
    padding = (-len(data)) % 4
    if padding:
        data += bytes([byte]) * padding
    return data


def accessor_byte_size(accessor):
    component_size = COMPONENT_SIZES[accessor["componentType"]]
    component_count = TYPE_COMPONENT_COUNTS[accessor["type"]]
    return component_size * component_count


def accessor_byte_range(document, accessor_index):
    accessor = document["accessors"][accessor_index]
    if "sparse" in accessor:
        raise ValueError(f"sparse accessor {accessor_index} is not supported")
    buffer_view = document["bufferViews"][accessor["bufferView"]]
    element_size = accessor_byte_size(accessor)
    stride = buffer_view.get("byteStride", element_size)
    count = accessor["count"]
    byte_offset = buffer_view.get("byteOffset", 0) + accessor.get("byteOffset", 0)
    byte_length = 0 if count == 0 else stride * (count - 1) + element_size
    return byte_offset, byte_length, stride


def collect_used_accessors(document, selected_animations):
    used = set()

    for mesh in document.get("meshes", []):
        for primitive in mesh.get("primitives", []):
            used.update(primitive.get("attributes", {}).values())
            if "indices" in primitive:
                used.add(primitive["indices"])
            for target in primitive.get("targets", []):
                used.update(target.values())

    for skin in document.get("skins", []):
        if "inverseBindMatrices" in skin:
            used.add(skin["inverseBindMatrices"])

    for animation in selected_animations:
        for sampler in animation.get("samplers", []):
            used.add(sampler["input"])
            used.add(sampler["output"])

    return used


def copy_accessor(document, binary, accessor_index, new_document, new_binary):
    old_accessor = document["accessors"][accessor_index]
    old_buffer_view = document["bufferViews"][old_accessor["bufferView"]]
    byte_offset, byte_length, _ = accessor_byte_range(document, accessor_index)
    new_offset = len(new_binary)
    new_binary += binary[byte_offset:byte_offset + byte_length]
    new_binary = bytearray(pad4(bytes(new_binary), 0))

    new_buffer_view = {
        "buffer": 0,
        "byteOffset": new_offset,
        "byteLength": byte_length,
    }
    if "byteStride" in old_buffer_view:
        new_buffer_view["byteStride"] = old_buffer_view["byteStride"]
    if "target" in old_buffer_view:
        new_buffer_view["target"] = old_buffer_view["target"]

    new_accessor = copy.deepcopy(old_accessor)
    new_accessor["bufferView"] = len(new_document["bufferViews"])
    new_accessor["byteOffset"] = 0
    new_document["bufferViews"].append(new_buffer_view)
    new_document["accessors"].append(new_accessor)
    return len(new_document["accessors"]) - 1, new_binary


def copy_image_buffer_view(document, binary, image, new_document, new_binary):
    old_view_index = image.get("bufferView")
    if old_view_index is None:
        return image, new_binary

    old_view = document["bufferViews"][old_view_index]
    byte_offset = old_view.get("byteOffset", 0)
    byte_length = old_view["byteLength"]
    new_offset = len(new_binary)
    new_binary += binary[byte_offset:byte_offset + byte_length]
    new_binary = bytearray(pad4(bytes(new_binary), 0))

    new_view = {
        "buffer": 0,
        "byteOffset": new_offset,
        "byteLength": byte_length,
    }
    if "target" in old_view:
        new_view["target"] = old_view["target"]

    new_image = copy.deepcopy(image)
    new_image["bufferView"] = len(new_document["bufferViews"])
    new_document["bufferViews"].append(new_view)
    return new_image, new_binary


def remap_primitive_accessors(primitive, accessor_remap):
    for key, value in list(primitive.get("attributes", {}).items()):
        primitive["attributes"][key] = accessor_remap[value]
    if "indices" in primitive:
        primitive["indices"] = accessor_remap[primitive["indices"]]
    for target in primitive.get("targets", []):
        for key, value in list(target.items()):
            target[key] = accessor_remap[value]


def extract(source, destination, animation_names):
    document, binary = read_glb(source)
    available = {animation.get("name", f"Animation {index + 1}"): animation for index, animation in enumerate(document.get("animations", []))}
    missing = [name for name in animation_names if name not in available]
    if missing:
        raise ValueError("Missing requested animations: " + ", ".join(missing))

    selected_animations = [copy.deepcopy(available[name]) for name in animation_names]
    used_accessors = sorted(collect_used_accessors(document, selected_animations))

    new_document = copy.deepcopy(document)
    new_document["animations"] = selected_animations
    new_document["accessors"] = []
    new_document["bufferViews"] = []
    new_document["buffers"] = [{"byteLength": 0}]
    new_binary = bytearray()

    accessor_remap = {}
    for accessor_index in used_accessors:
        new_index, new_binary = copy_accessor(document, binary, accessor_index, new_document, new_binary)
        accessor_remap[accessor_index] = new_index

    for mesh in new_document.get("meshes", []):
        for primitive in mesh.get("primitives", []):
            remap_primitive_accessors(primitive, accessor_remap)

    for skin in new_document.get("skins", []):
        if "inverseBindMatrices" in skin:
            skin["inverseBindMatrices"] = accessor_remap[skin["inverseBindMatrices"]]

    for animation in new_document.get("animations", []):
        for sampler in animation.get("samplers", []):
            sampler["input"] = accessor_remap[sampler["input"]]
            sampler["output"] = accessor_remap[sampler["output"]]

    new_images = []
    for image in new_document.get("images", []):
        new_image, new_binary = copy_image_buffer_view(document, binary, image, new_document, new_binary)
        new_images.append(new_image)
    if "images" in new_document:
        new_document["images"] = new_images

    new_document["buffers"][0]["byteLength"] = len(new_binary)
    json_bytes = pad4(json.dumps(new_document, separators=(",", ":")).encode("utf-8"), 0x20)
    bin_bytes = pad4(bytes(new_binary), 0)
    total_length = 12 + 8 + len(json_bytes) + 8 + len(bin_bytes)

    output = bytearray()
    output += GLB_MAGIC
    output += struct.pack("<II", GLB_VERSION_2, total_length)
    output += struct.pack("<II", len(json_bytes), JSON_CHUNK)
    output += json_bytes
    output += struct.pack("<II", len(bin_bytes), BIN_CHUNK)
    output += bin_bytes

    Path(destination).parent.mkdir(parents=True, exist_ok=True)
    Path(destination).write_bytes(output)
    return {
        "animations": len(selected_animations),
        "accessors": len(new_document["accessors"]),
        "bufferViews": len(new_document["bufferViews"]),
        "bytes": len(output),
    }


def main():
    parser = argparse.ArgumentParser(description="Extract a smaller GLB with a selected subset of animation clips.")
    parser.add_argument("source")
    parser.add_argument("destination")
    parser.add_argument("--animation", action="append", dest="animations", help="Exact animation clip name to keep. May be repeated.")
    args = parser.parse_args()

    animation_names = args.animations or DEFAULT_ANIMATIONS
    result = extract(args.source, args.destination, animation_names)
    print(
        f"Wrote {args.destination}: "
        f"{result['bytes'] / 1024 / 1024:.2f} MB, "
        f"{result['animations']} animations, "
        f"{result['accessors']} accessors, "
        f"{result['bufferViews']} bufferViews"
    )


if __name__ == "__main__":
    main()
