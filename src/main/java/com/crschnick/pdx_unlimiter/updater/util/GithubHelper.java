package com.crschnick.pdx_unlimiter.updater.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class GithubHelper {

    public static Path downloadFile(URL url, Consumer<Float> c, Callable<Boolean> shouldSkip) throws Exception {
        byte[] file = executeGet(url, c, shouldSkip);
        if (file == null) {
            return null;
        }

        String tempDir = System.getProperty("java.io.tmpdir");
        Path path = Paths.get(tempDir, url.getFile());
        FileUtils.forceMkdirParent(path.toFile());
        Files.write(path, file);
        return path;
    }

    public static DownloadInfo getInfo(URL targetURL, String fileName, String fileEnding, boolean platformSpecific) throws Exception {
        HttpURLConnection connection = null;

        try {
            //Create connection
            connection = (HttpURLConnection) targetURL.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", "https://github.com/crschnick/pdxu_launcher");
            connection.addRequestProperty("Accept", "*/*");
            connection.setInstanceFollowRedirects(false);

            int responseCode = connection.getResponseCode();
            if (responseCode != 302) {
                throw new IOException("Got http " + responseCode + " for " + targetURL);
            }

            String suffix = platformSpecific ? ("-"
                    + (SystemUtils.IS_OS_WINDOWS ? "windows" : (SystemUtils.IS_OS_LINUX ? "linux" : "mac"))) : "";
            String location = connection.getHeaderField("location");
            String version = Path.of(new URL(location).getFile()).getFileName().toString();
            URL toDownload = new URL(location + "/" + fileName + suffix + "." + fileEnding);
            URL changelog = new URL(location + "/" + "changelog.txt");
            DownloadInfo i = new DownloadInfo();
            i.changelogUrl = changelog;
            i.url = toDownload;
            i.version = version;
            return i;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] executeGet(URL targetURL, Consumer<Float> progress, Callable<Boolean> shouldSkip) throws Exception {
        HttpURLConnection connection = null;

        try {
            //Create connection
            connection = (HttpURLConnection) targetURL.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", "https://github.com/crschnick/pdxu_launcher");
            connection.addRequestProperty("Accept", "*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Got http " + responseCode + " for " + targetURL);
            }

            InputStream is = connection.getInputStream();
            int size = Integer.parseInt(connection.getHeaderField("Content-Length"));

            byte[] line;
            int bytes = 0;
            ByteBuffer b = ByteBuffer.allocate(size);
            while ((line = is.readNBytes(500000)).length > 0) {
                if (shouldSkip.call()) {
                    return null;
                }

                b.put(line);
                bytes += line.length;
                if (progress != null) {
                    progress.accept((float) bytes / (float) size);
                }
            }
            return b.array();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static class DownloadInfo {
        public URL url;
        public URL changelogUrl;
        public String version;

        @Override
        public String toString() {
            return "Info{" +
                    "url=" + url +
                    ", changelogUrl=" + changelogUrl +
                    ", version='" + version + '\'' +
                    '}';
        }
    }
}
