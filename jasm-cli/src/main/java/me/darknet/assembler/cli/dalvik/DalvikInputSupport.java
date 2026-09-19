package me.darknet.assembler.cli.dalvik;

import me.darknet.assembler.io.DalvikDexIO;
import me.darknet.dex.tree.DexFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads standalone DEX files and the DEX entries contained in an APK.
 */
public final class DalvikInputSupport {
    private static final Pattern SECONDARY_DEX = Pattern.compile("^classes([0-9]+)\\.dex$");

    private DalvikInputSupport() {}

    /**
     * Reads one standalone DEX or all ordered DEX entries from an APK.
     *
     * @param source
     *             Input path ending in {@code .dex} or {@code .apk}.
     *
     * @return DEX files in deterministic input order.
     *
     * @throws IOException
     *         If the input cannot be read or contains no DEX entries.
     */
    public static List<DexFile> read(Path source) throws IOException {
        String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".dex")) {
            return List.of(DalvikDexIO.read(Files.readAllBytes(source)));
        }
        if (name.endsWith(".apk")) {
            return readApk(source);
        }
        throw new IOException("Dalvik input must be a .dex file or .apk: " + source);
    }

    private static List<DexFile> readApk(Path source) throws IOException {
        try (ZipFile zip = new ZipFile(source.toFile())) {
            List<ZipDexEntry> entries = new ArrayList<>();
            zip.stream().forEach(entry -> {
                if (entry.isDirectory()) {
                    return;
                }
                int index = dexIndex(entry);
                if (index >= 0) {
                    entries.add(new ZipDexEntry(index, entry));
                }
            });
            if (entries.isEmpty()) {
                throw new IOException("APK contains no classes.dex entries: " + source);
            }

            entries.sort(Comparator.comparingInt(ZipDexEntry::index));
            Set<Integer> indexes = new HashSet<>();
            List<DexFile> files = new ArrayList<>(entries.size());
            for (ZipDexEntry entry : entries) {
                if (!indexes.add(entry.index())) {
                    throw new IOException("APK contains duplicate DEX index " + entry.index() + ": " + source);
                }
                try (InputStream input = zip.getInputStream(entry.entry())) {
                    files.add(DalvikDexIO.read(input));
                } catch (IOException exception) {
                    throw new IOException("Failed to read APK DEX entry " + entry.entry().getName()
                            + ": " + exception.getMessage(), exception);
                }
            }
            return List.copyOf(files);
        }
    }

    private static int dexIndex(ZipEntry entry) {
        String name = entry.getName();
        if ("classes.dex".equals(name)) {
            return 1;
        }
        Matcher matcher = SECONDARY_DEX.matcher(name);
        if (matcher.matches()) {
            try {
                int index = Integer.parseInt(matcher.group(1));
                return index >= 2 ? index : -1;
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private record ZipDexEntry(int index, ZipEntry entry) {}
}
