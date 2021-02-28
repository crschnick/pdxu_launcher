package com.crschnick.pdx_unlimiter.updater.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DirectoryHelper {

    public static Optional<String> getVersion(Path dir) throws IOException {
        Path vFile = dir.resolve("version");
        return Optional.ofNullable(Files.exists(vFile) ? Files.readString(vFile) : null);
    }

    public static void writeVersion(Path dir, String version) throws IOException {
        Path vFile = dir.resolve("version");
        Files.writeString(vFile, version);
    }

    public static void deleteOldVersion(Path path) throws Exception {
        File f = path.toFile();
        FileUtils.deleteDirectory(f);
        FileUtils.forceMkdir(f);
    }

    public static void unzip(Path zipFilePath, Path destDir) throws Exception {
        File dir = destDir.toFile();
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        FileUtils.forceMkdir(dir);

        ZipFile f = new ZipFile(zipFilePath.toString());
        for (Iterator<? extends ZipEntry> it = f.stream().iterator(); it.hasNext(); ) {
            ZipEntry e = it.next();
            String fileName = e.getName();
            Path p = destDir.resolve(fileName);
            if (e.isDirectory()) {
                FileUtils.forceMkdir(p.toFile());
            } else {
                Files.write(p, f.getInputStream(e).readAllBytes());

                // Workaround since the Java Zip API can not access permissions
                if (fileName.contains("bin") || fileName.contains("lib")) {
                    if (!p.toFile().setExecutable(true)) {
                        throw new IOException("Can't make " + p.toString() + " executable!");
                    }
                }
            }
        }
        f.close();
    }
}
