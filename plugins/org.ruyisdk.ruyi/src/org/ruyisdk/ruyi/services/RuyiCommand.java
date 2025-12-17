package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.ruyi.services.RuyiProperties.TelemetryStatus;

/**
 * Command executor for Ruyi operations.
 */
public class RuyiCommand {

    /**
     * Gets installed Ruyi version from path.
     *
     * @param ruyiPath ruyi installation path
     * @return version or null
     */
    public static RuyiVersion getInstalledVersion(String ruyiPath) {
        try {
            Process process = new ProcessBuilder(ruyiPath + "/ruyi", "-V").start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                // 仅读取首行内容
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    return null;
                }

                // 精准截取版本号部分
                String prefix = "Ruyi ";
                if (firstLine.startsWith(prefix)) {
                    // 截断字符串并清理首尾空格
                    String versionStr = firstLine.substring(prefix.length()).trim();

                    // 正则校验版本号格式（如0.31.0）
                    if (versionStr.matches("^\\d+\\.\\d+\\.\\d+$")) {
                        return RuyiVersion.parse(versionStr);
                    }
                }
            }
        } catch (IOException e) {
            // 可在此处添加日志输出
        }
        return null;
    }

    /**
     * Sets repository remote URL.
     *
     * @param ruyiPath ruyi installation path
     * @param repourl repository URL
     * @throws Exception if command fails
     */
    public static void setRepoRemote(String ruyiPath, String repourl) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(ruyiPath + "/ruyi", "config", "set", "repo.remote", repourl);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new Exception("ruyi config set repo.remote execution failure. \n" + output);
            }
        } catch (IOException | InterruptedException e) {
            throw new Exception("Failed to set repo.remote. ", e);
        }
    }

    /**
     * Gets repository remote URL.
     *
     * @param ruyiPath ruyi installation path
     * @return repository URL or null
     */
    public static String getRepoRemote(String ruyiPath) {
        try {
            Process process = new ProcessBuilder(ruyiPath + "/ruyi", "config", "get", "repo.remote").start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                // 仅读取首行内容
                String firstLine = reader.readLine();
                return firstLine;
            }
        } catch (IOException e) {
            // 可在此处添加日志输出
        }
        return null;
    }

    /**
     * Updates Ruyi index.
     *
     * @param ruyiPath ruyi installation path
     * @throws Exception if update fails
     */
    public static void updateRuyi(String ruyiPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(ruyiPath + "/ruyi", "update");
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new Exception("ruyi update execution failure. \n" + output);
            }
        } catch (IOException | InterruptedException e) {
            throw new Exception("Failed to update index. ", e);
        }
    }

    /**
     * Sets telemetry status.
     *
     * @param ruyiPath ruyi installation path
     * @param status telemetry status
     * @throws Exception if command fails
     */
    public static void setTelemetry(String ruyiPath, TelemetryStatus status) throws Exception {
        String parameter = "consent";
        switch (status) {
            case ON:
                parameter = "consent";
                break;
            case LOCAL:
                parameter = "local";
                break;
            case OFF:
                parameter = "optout";
                break;
            default:
                parameter = "consent";
                break;
        }

        ProcessBuilder pb = new ProcessBuilder(ruyiPath + "/ruyi", "telemetry", parameter);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new Exception("ruyi telemetry execution failure. \n" + output);
            }
        } catch (IOException | InterruptedException e) {
            throw new Exception("Failed to run ruyi telemetry. ", e);
        }
    }

    /**
     * Gets telemetry status.
     *
     * @param ruyiPath ruyi installation path
     * @return telemetry status or null
     */
    public static String getTelemetryStatus(String ruyiPath) {
        try {
            Process process = new ProcessBuilder(ruyiPath + "/ruyi", "telemetry", "status").start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                // 仅读取首行内容
                String firstLine = reader.readLine();
                return firstLine;
            }
        } catch (IOException e) {
            // 可在此处添加日志输出
        }
        return null;
    }

    /**
     * Uploads telemetry data.
     *
     * @param ruyiPath ruyi installation path
     * @throws Exception if upload fails
     */
    public static void telemetryUpload(String ruyiPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(ruyiPath + "/ruyi", " telemetry", "upload");
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                throw new Exception("ruyi telemetry upload execution failure. \n" + output);
            }
        } catch (IOException | InterruptedException e) {
            throw new Exception("Failed to upload telemetry data. ", e);
        }
    }
}
