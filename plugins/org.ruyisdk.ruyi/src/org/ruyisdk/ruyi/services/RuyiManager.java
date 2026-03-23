package org.ruyisdk.ruyi.services;

import org.ruyisdk.core.ruyi.model.RuyiReleaseInfo;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;

/**
 * Manager for Ruyi operations.
 */
public class RuyiManager {

    /**
     * Checks if Ruyi is installed.
     *
     * @return true if installed
     */
    public static boolean isRuyiInstalled() {
        return getInstalledVersion() != null;
    }

    /**
     * Gets installed Ruyi version.
     *
     * @return installed version or null
     */
    public static RuyiVersion getInstalledVersion() {
        return RuyiCli.getInstalledVersion();
    }

    /**
     * Gets latest Ruyi version.
     *
     * @return latest version or null
     */
    public static RuyiVersion getLatestVersion() {
        String archSuffix = SystemInfo.detectArchitecture().getSuffix();
        RuyiVersion version = null;
        try {
            RuyiReleaseInfo info = RuyiApi.getLatestRelease(archSuffix);
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
