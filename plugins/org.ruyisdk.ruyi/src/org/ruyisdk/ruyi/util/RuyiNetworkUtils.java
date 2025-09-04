package org.ruyisdk.ruyi.util;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

public class RuyiNetworkUtils {
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int BUFFER_SIZE = 8192;

    /**
     * 下载文件到本地路径
     */
    public static void downloadFile(String fileUrl, String destinationPath, IProgressMonitor monitor,
                    BiConsumer<Long, Long> progressCallback) throws IOException {
        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;

        try {
            URL url = new URL(fileUrl);

            System.out.println("fileUrl===" + fileUrl);

            connection = (HttpURLConnection) url.openConnection();
            configureConnection(connection);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + responseCode + ": " + connection.getResponseMessage());
            }

            long fileSize = connection.getContentLengthLong();
            // SubMonitor subMonitor = SubMonitor.convert(monitor, "Downloading " + url.getFile(), 100);

            input = connection.getInputStream();
            output = new BufferedOutputStream(new FileOutputStream(destinationPath));

            byte[] buffer = new byte[BUFFER_SIZE];
            long totalRead = 0;
            int bytesRead;

            // while ((bytesRead = input.read(buffer)) != -1 && !subMonitor.isCanceled()) {
            while ((bytesRead = input.read(buffer)) != -1 && !monitor.isCanceled()) {
                output.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                if (fileSize > 0) {
                    // int percentDone = (int)(totalRead * 100 / fileSize);
                    // subMonitor.setWorkRemaining(100 - percentDone);
                    // subMonitor.worked(1);
                    //
                    if (progressCallback != null) {
                        progressCallback.accept(totalRead, fileSize);
                    }
                    // } else {
                    // subMonitor.worked(1);
                }
            }

            // if (subMonitor.isCanceled()) {
            if (monitor.isCanceled()) {
                throw new InterruptedIOException("Download cancelled by user");
            }
        } finally {
            closeQuietly(input);
            closeQuietly(output);
            if (connection != null) {
                connection.disconnect();
            }
            // if (monitor != null) {
            // monitor.done();
            // }
        }
    }

    /**
     * 发送HTTP GET请求获取字符串内容
     */
    public static String fetchStringContent(String urlString, IProgressMonitor monitor) throws IOException {
        HttpURLConnection connection = null;
        InputStream input = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            configureConnection(connection);

            if (monitor != null) {
                monitor.subTask("Connecting to " + url.getHost());
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP request failed with code: " + responseCode);
            }

            input = connection.getInputStream();
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null && (monitor == null || !monitor.isCanceled())) {
                content.append(line).append("\n");

                if (monitor != null) {
                    monitor.worked(1);
                }
            }

            if (monitor != null && monitor.isCanceled()) {
                throw new InterruptedIOException("Operation cancelled by user");
            }

            return content.toString();
        } finally {
            closeQuietly(input);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送JSON格式的POST请求
     */
    public static String postJson(String urlString, Map<String, Object> data, IProgressMonitor monitor)
                    throws IOException {
        HttpURLConnection connection = null;
        OutputStream output = null;
        InputStream input = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            configureConnection(connection);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            if (monitor != null) {
                monitor.subTask("Preparing request data");
            }

            String jsonInput = JsonUtils.toJson(data);
            byte[] inputBytes = jsonInput.getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(inputBytes.length);
            output = connection.getOutputStream();
            output.write(inputBytes);
            output.flush();

            if (monitor != null) {
                monitor.worked(30);
                monitor.subTask("Waiting for server response");
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP request failed with code: " + responseCode);
            }

            input = connection.getInputStream();
            StringBuilder response = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null && (monitor == null || !monitor.isCanceled())) {
                response.append(line);

                if (monitor != null) {
                    monitor.worked(1);
                }
            }

            if (monitor != null && monitor.isCanceled()) {
                throw new InterruptedIOException("Operation cancelled by user");
            }

            return response.toString();
        } finally {
            closeQuietly(output);
            closeQuietly(input);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 检查URL是否可访问
     */
    public static boolean isUrlAccessible(String urlString, int timeoutMillis) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void configureConnection(HttpURLConnection connection) {
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "RuyiSDK-EclipsePlugin");
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    // 内部JSON工具类
    private static class JsonUtils {
        static String toJson(Map<String, Object> data) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                json.append("\"").append(escapeJson(entry.getKey())).append("\":");
                Object value = entry.getValue();

                if (value instanceof String) {
                    json.append("\"").append(escapeJson((String) value)).append("\"");
                } else if (value instanceof Number || value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"").append(escapeJson(value.toString())).append("\"");
                }
            }

            json.append("}");
            return json.toString();
        }

        private static String escapeJson(String str) {
            return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\b", "\\b").replace("\f", "\\f")
                            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }
    }
}
