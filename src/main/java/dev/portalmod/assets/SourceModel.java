package dev.portalmod.assets;

import java.util.List;

public record SourceModel(Vertex[] vertices, List<Mesh> meshes, Bone[] bones) {
    public record Vertex(float x, float y, float z, float nx, float ny, float nz,
                         float u, float v, int[] bones, float[] weights) { }
    public record Mesh(String material, int[] triangles) { }
    public record Bone(String name, int parent, float[] localPosition, float[] localQuaternion, float[] inverseBind) { }
    public int triangleCount() { return meshes.stream().mapToInt(m -> m.triangles.length / 3).sum(); }
}
