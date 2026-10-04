package dev.portalmod.assets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Read-only Source VPK v1/v2 reader. No assets are extracted or shipped. */
public final class VpkArchive {
    private record Entry(int archive, long offset, int length, byte[] preload) { }
    private final Path directory;
    private final long inlineOffset;
    private final Map<String, Entry> entries = new HashMap<>();
    public VpkArchive(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath();
        byte[] data = Files.readAllBytes(directory);
        if (data.length > 32 * 1024 * 1024) throw new IOException("VPK directory exceeds reader limit");
        ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (b.getInt() != 0x55AA1234) throw new IOException("Invalid VPK signature");
        int version = b.getInt(), treeSize = b.getInt();
        if (version != 1 && version != 2) throw new IOException("Unsupported VPK version " + version);
        int header = version == 1 ? 12 : 28;
        inlineOffset = (long) header + treeSize;
        b.position(header);
        String extension, folder, name;
        while (!(extension = string(b)).isEmpty()) {
            while (!(folder = string(b)).isEmpty()) {
                while (!(name = string(b)).isEmpty()) {
                    b.getInt();
                    int preloadLength = Short.toUnsignedInt(b.getShort());
                    int archive = Short.toUnsignedInt(b.getShort());
                    long offset = Integer.toUnsignedLong(b.getInt());
                    int length = b.getInt();
                    if (b.getShort() != (short) 0xffff || length < 0 || length > 32 * 1024 * 1024)
                        throw new IOException("Invalid VPK entry");
                    byte[] preload = new byte[preloadLength]; b.get(preload);
                    entries.put(normalize((folder.equals(" ") ? "" : folder + "/") + name + "." + extension),
                            new Entry(archive, offset, length, preload));
                }
            }
        }
    }
    public boolean contains(String path) { return entries.containsKey(normalize(path)); }
    public byte[] read(String path) throws IOException {
        Entry e = entries.get(normalize(path));
        if (e == null) throw new IOException("Portal 2 asset not found: " + path);
        String base = directory.getFileName().toString().replace("_dir.vpk", "");
        Path archive = e.archive == 0x7fff ? directory : directory.resolveSibling(base + "_%03d.vpk".formatted(e.archive));
        byte[] result = new byte[e.preload.length + e.length];
        System.arraycopy(e.preload, 0, result, 0, e.preload.length);
        if (e.length > 0) try (RandomAccessFile file = new RandomAccessFile(archive.toFile(), "r")) {
            file.seek(e.offset + (e.archive == 0x7fff ? inlineOffset : 0));
            file.readFully(result, e.preload.length, e.length);
        }
        return result;
    }
    public static String normalize(String path) { return path.replace('\\', '/').toLowerCase(Locale.ROOT); }
    private static String string(ByteBuffer b) throws IOException {
        int start = b.position();
        while (b.hasRemaining()) if (b.get() == 0) {
            return new String(b.array(), start, b.position() - start - 1, StandardCharsets.UTF_8);
        }
        throw new IOException("Unterminated VPK directory string");
    }
}
