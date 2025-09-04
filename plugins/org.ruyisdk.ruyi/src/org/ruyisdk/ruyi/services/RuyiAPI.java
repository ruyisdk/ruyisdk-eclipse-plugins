package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.ruyisdk.core.ruyi.model.RuyiReleaseInfo;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;

/**
 * RuyiSDK 网络接口服务类
 */
public class RuyiAPI {
    private static final String BASE_URL = "https://api.ruyisdk.cn";
    private static final int TIMEOUT = 10000;

    /**
     * 获取稳定版发布信息
     * 
     * @param osArch 系统架构 (x86_64/aarch64/riscv64)
     * @return 结构化版本信息
     * @throws RuyiAPIException 当API请求或数据处理失败时抛出
     */
    public static RuyiReleaseInfo getLatestRelease(String osArch) throws RuyiAPIException {
        try {
            JSONObject response = sendGetRequest("/releases/latest-pm");
            JSONObject stable = response.getJSONObject("channels").getJSONObject("stable");

            // 验证架构支持
            String platformKey = SystemInfo.getPlatformKey();
            System.out.println("platformKey==" + platformKey);


            JSONObject downloads = stable.getJSONObject("download_urls");
            if (!downloads.has(platformKey)) {
                throw new RuyiAPIException("Unsupported architecture: " + osArch);
            }

            // 构建版本信息对象
            String[] urls = downloads.getJSONArray(platformKey).toList().stream().map(Object::toString)
                            .toArray(String[]::new);

            String version = stable.getString("version");
            String filename = urls[0].split(version + "/")[1];
            return new RuyiReleaseInfo(RuyiVersion.parse(version), stable.getString("channel"), filename, urls[0], // GitHub
                                                                                                                   // URL
                            urls[1] // Mirror URL
            );

        } catch (Exception e) {
            throw new RuyiAPIException("Failed to get release info: " + e.getMessage(), e);
        }
    }

    private static JSONObject sendGetRequest(String path) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;

        try {
            conn = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            if (conn.getResponseCode() != 200) {
                throw new IOException("HTTP " + conn.getResponseCode());
            }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return new JSONObject(response.toString());
        } finally {
            if (reader != null)
                reader.close();
            if (conn != null)
                conn.disconnect();
        }
    }
}


/**
 * RuyiAPI专用异常
 */
class RuyiAPIException extends Exception {
    public RuyiAPIException(String message) {
        super(message);
    }

    public RuyiAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
