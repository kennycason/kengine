#!/usr/bin/env python3
"""Bake one skeletal GLB animation pose into an ordinary static GLB.

The output keeps the source mesh/material/image data, replaces skinned vertex
positions and normals with one sampled pose, and removes the runtime skeleton.
It intentionally uses only the Python standard library so build assets can be
regenerated without Blender or a Python package environment.
"""

import argparse
import bisect
import json
import math
import struct
from pathlib import Path


GLB_MAGIC = b"glTF"
GLB_VERSION_2 = 2
JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942

COMPONENT_FORMATS = {
    5120: ("b", 1),
    5121: ("B", 1),
    5122: ("h", 2),
    5123: ("H", 2),
    5125: ("I", 4),
    5126: ("f", 4),
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


def read_glb(path):
    data = Path(path).read_bytes()
    if len(data) < 12 or data[:4] != GLB_MAGIC:
        raise ValueError(f"{path} is not a GLB file")
    version, total_length = struct.unpack_from("<II", data, 4)
    if version != GLB_VERSION_2 or total_length != len(data):
        raise ValueError(f"{path} is not a valid GLB 2.0 file")

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
            binary = bytearray(chunk)
    if document is None or binary is None:
        raise ValueError(f"{path} must contain JSON and BIN chunks")
    return document, binary


def pad4(data, byte):
    return data + bytes([byte]) * ((-len(data)) % 4)


def write_glb(path, document, binary):
    document["buffers"] = [{"byteLength": len(binary)}]
    json_bytes = pad4(json.dumps(document, separators=(",", ":")).encode("utf-8"), 0x20)
    bin_bytes = pad4(bytes(binary), 0)
    total_length = 12 + 8 + len(json_bytes) + 8 + len(bin_bytes)
    output = bytearray(GLB_MAGIC)
    output += struct.pack("<II", GLB_VERSION_2, total_length)
    output += struct.pack("<II", len(json_bytes), JSON_CHUNK)
    output += json_bytes
    output += struct.pack("<II", len(bin_bytes), BIN_CHUNK)
    output += bin_bytes
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(output)


def accessor_layout(document, accessor_index):
    accessor = document["accessors"][accessor_index]
    if "sparse" in accessor:
        raise ValueError(f"sparse accessor {accessor_index} is not supported")
    view = document["bufferViews"][accessor["bufferView"]]
    component_format, component_size = COMPONENT_FORMATS[accessor["componentType"]]
    component_count = TYPE_COMPONENT_COUNTS[accessor["type"]]
    element_size = component_size * component_count
    stride = view.get("byteStride", element_size)
    offset = view.get("byteOffset", 0) + accessor.get("byteOffset", 0)
    return accessor, component_format, component_count, element_size, stride, offset


def normalize_integer(value, component_type):
    if component_type == 5120:
        return max(value / 127.0, -1.0)
    if component_type == 5121:
        return value / 255.0
    if component_type == 5122:
        return max(value / 32767.0, -1.0)
    if component_type == 5123:
        return value / 65535.0
    if component_type == 5125:
        return value / 4294967295.0
    return value


def read_accessor(document, binary, accessor_index):
    accessor, component_format, component_count, _, stride, offset = accessor_layout(
        document, accessor_index
    )
    row_format = "<" + component_format * component_count
    rows = []
    for index in range(accessor["count"]):
        values = struct.unpack_from(row_format, binary, offset + index * stride)
        if accessor.get("normalized") and accessor["componentType"] != 5126:
            values = tuple(normalize_integer(value, accessor["componentType"]) for value in values)
        rows.append(values)
    return rows


def write_float_accessor(document, binary, accessor_index, rows):
    accessor, component_format, component_count, _, stride, offset = accessor_layout(
        document, accessor_index
    )
    if component_format != "f" or len(rows) != accessor["count"]:
        raise ValueError(f"accessor {accessor_index} is not a matching float accessor")
    row_format = "<" + "f" * component_count
    for index, values in enumerate(rows):
        struct.pack_into(row_format, binary, offset + index * stride, *values)
    if rows and accessor["type"] in ("VEC2", "VEC3", "VEC4"):
        accessor["min"] = [min(row[component] for row in rows) for component in range(component_count)]
        accessor["max"] = [max(row[component] for row in rows) for component in range(component_count)]


def identity_matrix():
    return (
        1.0, 0.0, 0.0, 0.0,
        0.0, 1.0, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0,
    )


def multiply_matrices(left, right):
    result = [0.0] * 16
    for column in range(4):
        for row in range(4):
            result[column * 4 + row] = sum(
                left[inner * 4 + row] * right[column * 4 + inner]
                for inner in range(4)
            )
    return tuple(result)


def normalized_quaternion(value):
    length = math.sqrt(sum(component * component for component in value))
    if length == 0.0:
        return (0.0, 0.0, 0.0, 1.0)
    return tuple(component / length for component in value)


def slerp_quaternion(start, target, amount):
    start = normalized_quaternion(start)
    end = normalized_quaternion(target)
    cosine = sum(a * b for a, b in zip(start, end))
    if cosine < 0.0:
        cosine = -cosine
        end = tuple(-value for value in end)
    if cosine > 0.9995:
        return normalized_quaternion(tuple(a + (b - a) * amount for a, b in zip(start, end)))
    angle = math.acos(max(-1.0, min(1.0, cosine)))
    sine = math.sin(angle)
    if abs(sine) < 0.000001:
        return start
    from_scale = math.sin((1.0 - amount) * angle) / sine
    to_scale = math.sin(amount * angle) / sine
    return normalized_quaternion(
        tuple(a * from_scale + b * to_scale for a, b in zip(start, end))
    )


def transform_matrix(translation, rotation, scale):
    x, y, z, w = normalized_quaternion(rotation)
    sx, sy, sz = scale
    xx, yy, zz = x * x, y * y, z * z
    xy, xz, yz = x * y, x * z, y * z
    wx, wy, wz = w * x, w * y, w * z
    return (
        (1.0 - 2.0 * (yy + zz)) * sx,
        2.0 * (xy + wz) * sx,
        2.0 * (xz - wy) * sx,
        0.0,
        2.0 * (xy - wz) * sy,
        (1.0 - 2.0 * (xx + zz)) * sy,
        2.0 * (yz + wx) * sy,
        0.0,
        2.0 * (xz + wy) * sz,
        2.0 * (yz - wx) * sz,
        (1.0 - 2.0 * (xx + yy)) * sz,
        0.0,
        translation[0], translation[1], translation[2], 1.0,
    )


def transform_point(matrix, point):
    x, y, z = point
    return (
        matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12],
        matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13],
        matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14],
    )


def transform_vector(matrix, vector):
    x, y, z = vector
    return (
        matrix[0] * x + matrix[4] * y + matrix[8] * z,
        matrix[1] * x + matrix[5] * y + matrix[9] * z,
        matrix[2] * x + matrix[6] * y + matrix[10] * z,
    )


def normalized_vector(vector):
    length = math.sqrt(sum(component * component for component in vector))
    if length == 0.0:
        return (0.0, 1.0, 0.0)
    return tuple(component / length for component in vector)


def node_transform(node):
    return {
        "translation": tuple(node.get("translation", (0.0, 0.0, 0.0))),
        "rotation": normalized_quaternion(tuple(node.get("rotation", (0.0, 0.0, 0.0, 1.0)))),
        "scale": tuple(node.get("scale", (1.0, 1.0, 1.0))),
    }


def sample_values(times, values, time_seconds, path, interpolation):
    if len(times) == 1 or time_seconds <= times[0][0]:
        return values[0]
    flat_times = [row[0] for row in times]
    end_index = bisect.bisect_left(flat_times, time_seconds)
    if end_index >= len(flat_times):
        return values[-1]
    start_index = end_index - 1
    start_time = flat_times[start_index]
    end_time = flat_times[end_index]
    amount = 0.0 if end_time <= start_time else (time_seconds - start_time) / (end_time - start_time)
    if interpolation == "STEP":
        amount = 0.0
    if path == "rotation":
        return slerp_quaternion(values[start_index], values[end_index], amount)
    return tuple(
        start + (end - start) * amount
        for start, end in zip(values[start_index], values[end_index])
    )


def sample_node_matrices(document, binary, animation, time_seconds):
    transforms = [node_transform(node) for node in document["nodes"]]
    animated_nodes = set()
    duration = 0.0
    for sampler in animation["samplers"]:
        times = read_accessor(document, binary, sampler["input"])
        if times:
            duration = max(duration, times[-1][0])
    local_time = time_seconds % duration if duration > 0.0 else 0.0

    for channel in animation["channels"]:
        target = channel.get("target", {})
        node_index = target.get("node")
        path = target.get("path")
        if node_index is None or path not in ("translation", "rotation", "scale"):
            continue
        sampler = animation["samplers"][channel["sampler"]]
        times = read_accessor(document, binary, sampler["input"])
        values = read_accessor(document, binary, sampler["output"])
        transforms[node_index][path] = sample_values(
            times, values, local_time, path, sampler.get("interpolation", "LINEAR")
        )
        animated_nodes.add(node_index)

    local_matrices = []
    for index, node in enumerate(document["nodes"]):
        if "matrix" in node and index not in animated_nodes:
            local_matrices.append(tuple(node["matrix"]))
        else:
            transform = transforms[index]
            local_matrices.append(
                transform_matrix(transform["translation"], transform["rotation"], transform["scale"])
            )

    world_matrices = [identity_matrix() for _ in document["nodes"]]

    def visit(node_index, parent_matrix):
        world = multiply_matrices(parent_matrix, local_matrices[node_index])
        world_matrices[node_index] = world
        for child_index in document["nodes"][node_index].get("children", []):
            visit(child_index, world)

    scene_index = document.get("scene", 0)
    for root_index in document["scenes"][scene_index].get("nodes", []):
        visit(root_index, identity_matrix())
    return world_matrices


def bake_pose(document, binary, animation_name, time_seconds):
    animations = document.get("animations", [])
    animation = next((item for item in animations if item.get("name") == animation_name), None)
    if animation is None:
        available = ", ".join(item.get("name", "<unnamed>") for item in animations)
        raise ValueError(f"animation {animation_name!r} not found; available: {available}")

    world_matrices = sample_node_matrices(document, binary, animation, time_seconds)
    skin_matrices = []
    for skin in document.get("skins", []):
        inverse_bind = read_accessor(document, binary, skin["inverseBindMatrices"])
        skin_matrices.append([
            multiply_matrices(world_matrices[node_index], inverse_bind[joint_index])
            for joint_index, node_index in enumerate(skin["joints"])
        ])

    static_nodes = []
    written_positions = set()
    for node_index, node in enumerate(document["nodes"]):
        mesh_index = node.get("mesh")
        skin_index = node.get("skin")
        if mesh_index is None or skin_index is None:
            continue
        matrices = skin_matrices[skin_index]
        mesh = document["meshes"][mesh_index]
        for primitive in mesh.get("primitives", []):
            attributes = primitive.get("attributes", {})
            position_index = attributes.get("POSITION")
            joint_index = attributes.get("JOINTS_0")
            weight_index = attributes.get("WEIGHTS_0")
            if position_index is None or joint_index is None or weight_index is None:
                continue
            if position_index in written_positions:
                raise ValueError("a position accessor is instanced by multiple skinned nodes")
            written_positions.add(position_index)
            positions = read_accessor(document, binary, position_index)
            joints = read_accessor(document, binary, joint_index)
            weights = read_accessor(document, binary, weight_index)
            normals_index = attributes.get("NORMAL")
            normals = read_accessor(document, binary, normals_index) if normals_index is not None else None
            baked_positions = []
            baked_normals = []
            for vertex_index, position in enumerate(positions):
                total_weight = sum(max(0.0, value) for value in weights[vertex_index])
                if total_weight <= 0.0:
                    baked_positions.append(position)
                    if normals is not None:
                        baked_normals.append(normals[vertex_index])
                    continue
                out_position = [0.0, 0.0, 0.0]
                out_normal = [0.0, 0.0, 0.0]
                for influence in range(4):
                    weight = max(0.0, weights[vertex_index][influence]) / total_weight
                    if weight == 0.0:
                        continue
                    matrix = matrices[int(joints[vertex_index][influence])]
                    transformed_position = transform_point(matrix, position)
                    for component in range(3):
                        out_position[component] += transformed_position[component] * weight
                    if normals is not None:
                        transformed_normal = transform_vector(matrix, normals[vertex_index])
                        for component in range(3):
                            out_normal[component] += transformed_normal[component] * weight
                baked_positions.append(tuple(out_position))
                if normals is not None:
                    baked_normals.append(normalized_vector(tuple(out_normal)))
            write_float_accessor(document, binary, position_index, baked_positions)
            if normals_index is not None:
                write_float_accessor(document, binary, normals_index, baked_normals)
            attributes.pop("JOINTS_0", None)
            attributes.pop("WEIGHTS_0", None)

        static_nodes.append({"name": node.get("name", f"Pose mesh {mesh_index}"), "mesh": mesh_index})

    if not static_nodes:
        raise ValueError("source contains no supported skinned mesh nodes")
    document["nodes"] = static_nodes
    document["scenes"] = [{"name": f"{animation_name} pose", "nodes": list(range(len(static_nodes)))}]
    document["scene"] = 0
    document.pop("animations", None)
    document.pop("skins", None)
    document.pop("cameras", None)
    return len(static_nodes), sum(len(mesh.get("primitives", [])) for mesh in document["meshes"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", help="Rigged source GLB")
    parser.add_argument("destination", help="Static pose GLB to write")
    parser.add_argument("--animation", required=True, help="Exact animation clip name")
    parser.add_argument("--time", required=True, type=float, help="Clip time in seconds")
    args = parser.parse_args()

    document, binary = read_glb(args.source)
    node_count, primitive_count = bake_pose(
        document, binary, args.animation, args.time
    )
    write_glb(args.destination, document, binary)
    print(
        f"Baked {args.animation} at {args.time:.3f}s: "
        f"{node_count} mesh nodes, {primitive_count} primitives -> {args.destination}"
    )


if __name__ == "__main__":
    main()
