package dev.portalmod.assets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** VTF 7.x base-color mip decoding (BC1/BC2/BC3 and RGBA/BGRA/BGR/RGB). */
public record VtfImage(int width, int height, int[] argb) {
    public static VtfImage decode(byte[] data, int maxDimension) throws IOException {
        try {
            ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            if (b.getInt(0) != 0x00465456 || b.getInt(4) != 7) throw new IOException("Unsupported VTF version");
            int width = Short.toUnsignedInt(b.getShort(16)), height = Short.toUnsignedInt(b.getShort(18));
            if (width < 1 || height < 1 || width > 8192 || height > 8192 || b.getShort(24) != 1) throw new IOException("Unsupported VTF dimensions/frames");
            int format = b.getInt(52), mips = Byte.toUnsignedInt(b.get(56)), start = b.getInt(12);
            if (b.getInt(8) >= 3) {
                boolean found = false;
                for (int i = 0; i < b.getInt(68); i++) {
                    int r = 80 + i * 8;
                    if (b.get(r) == 0x30 && b.get(r + 1) == 0 && b.get(r + 2) == 0) { start = b.getInt(r + 4); found = true; }
                }
                if (!found) throw new IOException("Missing VTF image resource");
            } else start += size(Byte.toUnsignedInt(b.get(61)), Byte.toUnsignedInt(b.get(62)), b.getInt(57));
            int chosen = 0;
            while (Math.max(width >> chosen, height >> chosen) > maxDimension && chosen < mips - 1) chosen++;
            for (int level = mips - 1; level > chosen; level--) start += size(Math.max(1, width >> level), Math.max(1, height >> level), format);
            width = Math.max(1, width >> chosen); height = Math.max(1, height >> chosen);
            int[] pixels = new int[width * height];
            if (format == 13 || format == 14 || format == 15 || format == 20) {
                int p = start;
                for (int y = 0; y < height; y += 4) for (int x = 0; x < width; x += 4) {
                    int[] alpha = new int[16]; java.util.Arrays.fill(alpha, 255);
                    if (format == 15) {
                        int a0 = Byte.toUnsignedInt(b.get(p)), a1 = Byte.toUnsignedInt(b.get(p + 1));
                        int[] palette = new int[8]; palette[0] = a0; palette[1] = a1;
                        if (a0 > a1) for (int i = 1; i <= 6; i++) palette[i + 1] = ((7 - i) * a0 + i * a1) / 7;
                        else { for (int i = 1; i <= 4; i++) palette[i + 1] = ((5 - i) * a0 + i * a1) / 5; palette[6] = 0; palette[7] = 255; }
                        long bits = 0; for (int i = 0; i < 6; i++) bits |= (long) Byte.toUnsignedInt(b.get(p + 2 + i)) << (8 * i);
                        for (int i = 0; i < 16; i++) alpha[i] = palette[(int) (bits >> (3 * i)) & 7]; p += 8;
                    } else if (format == 14) { long bits = b.getLong(p); for (int i = 0; i < 16; i++) alpha[i] = (int) ((bits >>> (i * 4)) & 15) * 17; p += 8; }
                    int c0 = Short.toUnsignedInt(b.getShort(p)), c1 = Short.toUnsignedInt(b.getShort(p + 2));
                    int[] colors = {rgb565(c0), rgb565(c1), 0, 0};
                    boolean transparent = (format == 13 || format == 20) && c0 <= c1;
                    colors[2] = blend(colors[0], colors[1], transparent ? 1 : 2, 1);
                    colors[3] = transparent ? 0 : blend(colors[0], colors[1], 1, 2);
                    int bits = b.getInt(p + 4); p += 8;
                    for (int i = 0; i < 16; i++) if (x + i % 4 < width && y + i / 4 < height) {
                        int ci = (bits >>> (i * 2)) & 3;
                        pixels[(y + i / 4) * width + x + i % 4] = (transparent && ci == 3 ? 0 : alpha[i] << 24) | colors[ci];
                    }
                }
            } else {
                int p = start;
                for (int i = 0; i < pixels.length; i++) {
                    int r = Byte.toUnsignedInt(b.get(p++)), g = Byte.toUnsignedInt(b.get(p++)), blue = Byte.toUnsignedInt(b.get(p++));
                    if (format == 3 || format == 12 || format == 16) { int swap = r; r = blue; blue = swap; }
                    int a = format == 0 || format == 12 ? Byte.toUnsignedInt(b.get(p++)) : 255;
                    if (format == 16) p++;
                    pixels[i] = a << 24 | r << 16 | g << 8 | blue;
                }
            }
            return new VtfImage(width, height, pixels);
        } catch (RuntimeException e) { throw new IOException("Malformed VTF image", e); }
    }
    private static int size(int w, int h, int format) throws IOException {
        if (w == 0 || h == 0) return 0;
        return switch (format) { case 13, 20 -> Math.max(1, (w + 3) / 4) * Math.max(1, (h + 3) / 4) * 8;
            case 14, 15 -> Math.max(1, (w + 3) / 4) * Math.max(1, (h + 3) / 4) * 16;
            case 0, 12, 16 -> w * h * 4; case 2, 3 -> w * h * 3; default -> throw new IOException("Unsupported VTF format " + format); };
    }
    private static int rgb565(int c) { return ((c >> 11 & 31) * 255 / 31) << 16 | ((c >> 5 & 63) * 255 / 63) << 8 | (c & 31) * 255 / 31; }
    private static int blend(int a, int b, int wa, int wb) {
        int total = wa + wb, result = 0;
        for (int shift : new int[]{0, 8, 16}) result |= (((a >> shift & 255) * wa + (b >> shift & 255) * wb) / total) << shift;
        return result;
    }
}
