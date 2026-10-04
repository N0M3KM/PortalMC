package dev.portalmod.assets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** MDL 49, VVD 4 and VTX 7, highest-detail bodygroup-zero meshes. Independently implemented format reader. */
public final class SourceModelReader {
    private SourceModelReader() { }
    public static SourceModel read(VpkArchive archive, String model) throws IOException {
        try {
            String base = model.substring(0, model.length() - 4);
            ByteBuffer mdl = buffer(archive.read(model)), vvd = buffer(archive.read(base + ".vvd"));
            ByteBuffer vtx = buffer(archive.read(base + ".dx90.vtx"));
            if (mdl.getInt(0) != 0x54534449 || mdl.getInt(4) != 49 || vvd.getInt(0) != 0x56534449
                    || vvd.getInt(4) != 4 || vtx.getInt(0) != 7) throw new IOException("Unsupported Source model format");
            if (mdl.getInt(8) != vvd.getInt(8) || mdl.getInt(8) != vtx.getInt(16)) throw new IOException("Model companion checksum mismatch");
            int count = bounded(vvd.getInt(16), 100000), vertexStart = vvd.getInt(56);
            SourceModel.Vertex[] raw = new SourceModel.Vertex[count];
            for (int i = 0; i < count; i++) {
                int p = vertexStart + i * 48;
                raw[i] = new SourceModel.Vertex(vvd.getFloat(p + 16), vvd.getFloat(p + 20), vvd.getFloat(p + 24),
                        vvd.getFloat(p + 28), vvd.getFloat(p + 32), vvd.getFloat(p + 36), vvd.getFloat(p + 40), vvd.getFloat(p + 44),
                        new int[]{Byte.toUnsignedInt(vvd.get(p + 12)), Byte.toUnsignedInt(vvd.get(p + 13)), Byte.toUnsignedInt(vvd.get(p + 14))},
                        new float[]{vvd.getFloat(p), vvd.getFloat(p + 4), vvd.getFloat(p + 8)});
            }
            SourceModel.Vertex[] vertices = raw;
            if (vvd.getInt(48) > 0) {
                vertices = new SourceModel.Vertex[count]; int destination = 0;
                for (int i = 0; i < bounded(vvd.getInt(48), 100000); i++) {
                    int p = vvd.getInt(52) + i * 12, from = vvd.getInt(p + 4), length = vvd.getInt(p + 8);
                    System.arraycopy(raw, from, vertices, destination, length); destination += length;
                }
                if (destination != count) throw new IOException("Invalid VVD fixup table");
            }
            int boneCount = bounded(mdl.getInt(156), 256), boneStart = mdl.getInt(160);
            SourceModel.Bone[] bones = new SourceModel.Bone[boneCount];
            for (int i = 0; i < boneCount; i++) {
                int p = boneStart + i * 216;
                bones[i] = new SourceModel.Bone(string(mdl, p + mdl.getInt(p)), mdl.getInt(p + 4),
                        floats(mdl, p + 32, 3), floats(mdl, p + 44, 4), floats(mdl, p + 96, 12));
            }
            List<String> materialPaths = new ArrayList<>();
            for (int i = 0; i < bounded(mdl.getInt(212), 256); i++) materialPaths.add(string(mdl, mdl.getInt(mdl.getInt(216) + i * 4)));
            List<String> materials = new ArrayList<>();
            for (int i = 0; i < bounded(mdl.getInt(204), 256); i++) {
                int p = mdl.getInt(208) + i * 64;
                String name = VpkArchive.normalize(string(mdl, p + mdl.getInt(p))), found = name;
                if (!archive.contains("materials/" + name + ".vmt")) for (String path : materialPaths) {
                    String candidate = VpkArchive.normalize(path + name);
                    if (archive.contains("materials/" + candidate + ".vmt")) { found = candidate; break; }
                }
                materials.add(found);
            }
            List<SourceModel.Mesh> meshes;
            try { meshes = meshes(mdl, vtx, materials, vertices.length, false); }
            catch (IndexOutOfBoundsException | IllegalArgumentException e) { meshes = meshes(mdl, vtx, materials, vertices.length, true); }
            return new SourceModel(vertices, List.copyOf(meshes), bones);
        } catch (RuntimeException e) { throw new IOException("Malformed Source model " + model, e); }
    }
    private static List<SourceModel.Mesh> meshes(ByteBuffer mdl, ByteBuffer vtx, List<String> materials, int totalVertices, boolean extra) {
        List<SourceModel.Mesh> result = new ArrayList<>();
        int bodies = bounded(mdl.getInt(232), 64);
        if (bodies != vtx.getInt(28)) throw new IllegalArgumentException("Bodypart mismatch");
        for (int b = 0; b < bodies; b++) {
            int mb = mdl.getInt(236) + b * 16, vb = vtx.getInt(32) + b * 8;
            // First model is the default bodygroup. Alternative potato/accessory models are not selected.
            if (mdl.getInt(mb + 4) == 0 || vtx.getInt(vb) == 0) continue;
            int mm = mb + mdl.getInt(mb + 12), vm = vb + vtx.getInt(vb + 4);
            int lod = vm + vtx.getInt(vm + 4), meshCount = bounded(mdl.getInt(mm + 72), 256);
            if (meshCount != vtx.getInt(lod)) throw new IllegalArgumentException("Mesh mismatch");
            int modelVertexStart = mdl.getInt(mm + 84) / 48;
            for (int m = 0; m < meshCount; m++) {
                int mesh = mm + mdl.getInt(mm + 76) + m * 116;
                int vmesh = lod + vtx.getInt(lod + 4) + m * 9;
                int material = mdl.getInt(mesh), meshStart = modelVertexStart + mdl.getInt(mesh + 12);
                int skin = mdl.getInt(228);
                if (mdl.getInt(220) > material && skin > 0) material = Short.toUnsignedInt(mdl.getShort(skin + material * 2));
                List<Integer> triangles = new ArrayList<>();
                int groups = bounded(vtx.getInt(vmesh), 10000);
                for (int g = 0; g < groups; g++) {
                    int sg = vmesh + vtx.getInt(vmesh + 4) + g * (extra ? 33 : 25);
                    int vc = bounded(vtx.getInt(sg), 100000), vo = sg + vtx.getInt(sg + 4);
                    int ic = bounded(vtx.getInt(sg + 8), 1000000), io = sg + vtx.getInt(sg + 12);
                    int sc = bounded(vtx.getInt(sg + 16), 10000), so = sg + vtx.getInt(sg + 20);
                    int[] indices = new int[ic];
                    for (int i = 0; i < ic; i++) {
                        int index = Short.toUnsignedInt(vtx.getShort(io + i * 2));
                        if (index >= vc) throw new IllegalArgumentException("VTX vertex index");
                        indices[i] = meshStart + Short.toUnsignedInt(vtx.getShort(vo + index * 9 + 4));
                        if (indices[i] >= totalVertices) throw new IllegalArgumentException("VVD vertex index");
                    }
                    for (int s = 0; s < sc; s++) {
                        int strip = so + s * (extra ? 35 : 27), n = bounded(vtx.getInt(strip), ic), start = vtx.getInt(strip + 4);
                        if ((vtx.get(strip + 18) & 1) != 0) {
                            if (n % 3 != 0) throw new IllegalArgumentException("Triangle list length");
                            for (int i = 0; i < n; i++) triangles.add(indices[start + i]);
                        } else for (int i = 2; i < n; i++) {
                            triangles.add(indices[start + i - 2]); triangles.add(indices[start + i - (i % 2 == 0 ? 1 : 0)]);
                            triangles.add(indices[start + i - (i % 2 == 0 ? 0 : 1)]);
                        }
                    }
                }
                if (!triangles.isEmpty()) result.add(new SourceModel.Mesh(materials.get(material), triangles.stream().mapToInt(Integer::intValue).toArray()));
            }
        }
        return result;
    }
    private static int bounded(int value, int limit) { if (value < 0 || value > limit) throw new IllegalArgumentException("Source data count exceeds limit"); return value; }
    private static ByteBuffer buffer(byte[] b) { return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN); }
    private static float[] floats(ByteBuffer b, int p, int n) { float[] f = new float[n]; for (int i = 0; i < n; i++) f[i] = b.getFloat(p + i * 4); return f; }
    private static String string(ByteBuffer b, int p) { int e = p; while (b.get(e) != 0) e++; return new String(b.array(), p, e - p, java.nio.charset.StandardCharsets.UTF_8); }
}
