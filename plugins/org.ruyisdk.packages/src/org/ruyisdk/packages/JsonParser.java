package org.ruyisdk.packages;

import javax.json.*;

import org.ruyisdk.ruyi.util.RuyiFileUtils;

import java.io.StringReader;

public class JsonParser {

        public static TreeNode parseJson(String jsonData, java.util.Set<String> downloadedFiles, String hardwareType) {
            try (JsonReader reader = Json.createReader(new StringReader(jsonData))) {
                JsonStructure jsonStructure = reader.read();
                // 用 hardwareType 作为根节点名
                TreeNode root = new TreeNode(hardwareType, null);
                if (jsonStructure instanceof JsonObject) {
                    parseJsonObject((JsonObject) jsonStructure, root, downloadedFiles);
                } else if (jsonStructure instanceof JsonArray) {
                    JsonArray jsonArray = (JsonArray) jsonStructure;
                    for (JsonValue value : jsonArray) {
                        if (value instanceof JsonObject) {
                            parseJsonObject((JsonObject) value, root, downloadedFiles);
                        }
                    }
                }
                return root;
            } catch (Exception e) {
                throw new RuntimeException("解析 JSON 数据失败：" + e.getMessage(), e);
            }
        }





        private static void parseJsonObject(JsonObject rootObject, TreeNode root, java.util.Set<String> downloadedFiles) {
        if (!rootObject.containsKey("category") || !rootObject.containsKey("name")) {
            return;
        }
        String category = rootObject.getString("category");
        TreeNode categoryNode = findOrCreateCategoryNode(root, category);
    
        String name = rootObject.getString("name");
        TreeNode packageNode = new TreeNode(name, null);
        categoryNode.addChild(packageNode);
    
        JsonArray versions = rootObject.getJsonArray("vers");
        if (versions != null) {
            for (JsonValue versionValue : versions) {
                JsonObject versionObject = versionValue.asJsonObject();
                if (!versionObject.containsKey("semver")) {
                    continue;
                }
                String semver = versionObject.getString("semver");
                JsonArray remarks = versionObject.getJsonArray("remarks");
                String remark = (remarks != null && !remarks.isEmpty()) ? " [" + remarks.getString(0) + "]" : "";

                // 放在这里 ↓↓↓
                boolean isDownloaded = false;
                if (versionObject.containsKey("pm")) {
                    JsonObject pmObj = versionObject.getJsonObject("pm");
                    if (pmObj.containsKey("distfiles")) {
                        JsonArray distfiles = pmObj.getJsonArray("distfiles");
                        if (distfiles != null && !distfiles.isEmpty()) {
                            for (int i = 0; i < distfiles.size(); i++) {
                                JsonObject distfileObj = distfiles.getJsonObject(i);
                                if (distfileObj.containsKey("name")) {
                                    String fileName = distfileObj.getString("name");
                                    if (downloadedFiles != null && downloadedFiles.contains(fileName)) {
                                        isDownloaded = true;
                                        break; // 有一个文件已下载就算已下载
                                    }
                                }
                            }
                        }
                    }
                }

                // String installCommand = "ruyi install '" + name + "(" + semver + ")'";
                System.out.println("Ruyi 路径: " + RuyiFileUtils.getInstallPath());
                String installCommand = RuyiFileUtils.getInstallPath() + "/ruyi install '" + name + "(" + semver + ")'";
                TreeNode versionNode = new TreeNode(semver + remark, null, installCommand);
                versionNode.setLeaf(true);
                versionNode.setDownloaded(isDownloaded);
                packageNode.addChild(versionNode);
            }
        }
    }


    private static TreeNode findOrCreateCategoryNode(TreeNode root, String category) {
        // 遍历根节点的子节点，查找是否已存在该类别节点
        for (TreeNode child : root.getChildren()) {
            if (child.getName().equals(category)) {
                return child; // 如果找到，直接返回
            }
        }

        // 如果未找到，创建新的类别节点并添加到根节点
        TreeNode categoryNode = new TreeNode(category, null);
        root.addChild(categoryNode);
        return categoryNode;
    }
}