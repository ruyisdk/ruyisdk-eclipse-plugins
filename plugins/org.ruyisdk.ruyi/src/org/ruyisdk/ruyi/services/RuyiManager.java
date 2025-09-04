package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ruyisdk.core.config.Constants;
import org.ruyisdk.core.ruyi.model.RuyiReleaseInfo;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;
import org.ruyisdk.ruyi.util.RuyiFileUtils;

public class RuyiManager {

    public static boolean isRuyiInstalled() {
        try {
            Process process = new ProcessBuilder(RuyiFileUtils.getInstallPath() + "/ruyi", "-V").start();
            // Process process = Runtime.getRuntime().exec(RuyiFileUtils.getInstallPath()+"/ruyi -V");
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static RuyiVersion getInstalledVersion() {
        try {
            Process process = new ProcessBuilder(RuyiFileUtils.getInstallPath() + "/ruyi", "-V").start();

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

    public static RuyiVersion getLatestVersion() {
        String archSuffix = SystemInfo.detectArchitecture().getSuffix();
        RuyiVersion version = null;
        try {
            RuyiReleaseInfo info = RuyiAPI.getLatestRelease(archSuffix);
            version = info.getVersion();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return version;
    }

    // //此方法无法获取网页信息
    // public static RuyiVersion getLatestVersion1() {
    // try {
    // URL url = new URL(Constants.NetAddress.MIRROR_RUYI_RELEASES);
    // System.out.print(url);
    // HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // conn.setRequestMethod("GET");
    // System.out.print(conn.getResponseCode());
    //
    // // 处理非 200 状态码
    // if (conn.getResponseCode() != 200) {
    // System.err.println("HTTP 错误码: " + conn.getResponseCode());
    // return null;
    // }
    // try (BufferedReader reader = new BufferedReader(
    // new InputStreamReader(conn.getInputStream()))) {
    // String line;
    // RuyiVersion maxVersion = new RuyiVersion(0, 0, 0);
    //
    // while ((line = reader.readLine()) != null) {
    // Matcher matcher = Pattern.compile("href=\"(\\d+\\.\\d+\\.\\d+)/\"").matcher(line);
    // if (matcher.find()) {
    // String versionStr = matcher.group(1);
    // try {
    // RuyiVersion current = RuyiVersion.parse(versionStr);
    // if (current.compareTo(maxVersion) > 0) {
    // maxVersion = current;
    // }
    // } catch (IllegalArgumentException e) {
    // // 忽略格式不正确的版本号
    // System.err.println("忽略无效版本号: " + versionStr);
    // }
    // }
    // }
    //
    // return maxVersion;
    // }
    // } catch (IOException e) {
    // System.err.println("获取最新版本失败: " + e.getMessage());
    // return null;
    // }
    // }
}
