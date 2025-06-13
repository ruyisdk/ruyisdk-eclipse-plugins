package org.ruyisdk.packages;
import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonObject;
import javax.json.JsonArray;
import javax.json.JsonValue;
import javax.json.*;

import org.ruyisdk.ruyi.util.RuyiFileUtils;

import java.io.StringReader;

public class JsonParser {

        public static TreeNode parseJson(String jsonData, java.util.Set<String> downloadedFiles, String hardwareType) {
            try (JsonReader reader = Json.createReader(new StringReader(jsonData))) {
                JsonStructure jsonStructure = reader.read();
                // use hardwareType as root node name
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
                throw new RuntimeException("Failed to parse JSON data:" + e.getMessage(), e);
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
    
                boolean isDownloaded = versionObject.getBoolean("is_installed", false);
    
                String installCommand = RuyiFileUtils.getInstallPath() + "/ruyi install '" + name + "(" + semver + ")'";
                TreeNode versionNode = new TreeNode(semver + remark, null, installCommand);
                versionNode.setLeaf(true);
                versionNode.setDownloaded(isDownloaded);
                packageNode.addChild(versionNode);
            }
        }
    }


    private static TreeNode findOrCreateCategoryNode(TreeNode root, String category) {
        // Traverse the root node's children to find if the category node already exists
        for (TreeNode child : root.getChildren()) {
            if (child.getName().equals(category)) {
                return child; 
            }
        }

        TreeNode categoryNode = new TreeNode(category, null);
        root.addChild(categoryNode);
        return categoryNode;
    }


    public static List<String> parseAllEntityIdsInOneLine(String jsonLine) {
        List<String> entityIds = new ArrayList<>();
        int bracketCount = 0;
        int start = -1;
        for (int i = 0; i < jsonLine.length(); i++) {
            char c = jsonLine.charAt(i);
            if (c == '{') {
                if (bracketCount == 0) {
                    start = i;
                }
                bracketCount++;
            } else if (c == '}') {
                bracketCount--;
                if (bracketCount == 0 && start != -1) {
                    // get fully json 
                    String singleObject = jsonLine.substring(start, i + 1);
                    parseSingleObject(singleObject, entityIds);
                    start = -1;
                }
            }
        }
        return entityIds;
    }
    
    private static void parseSingleObject(String jsonStr, List<String> entityIds) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonStr))) {
            JsonValue value = reader.read();
            collectEntityIds(value, entityIds); 
        } catch (Exception e) {
            System.err.println("无法解析的 JSON 对象: " + jsonStr);
        }
    }
    
    // ...existing code...
    private static void collectEntityIds(JsonValue value, List<String> entityIds) {
        switch (value.getValueType()) {
            case OBJECT:
                JsonObject obj = value.asJsonObject();
                // if the current object contains entity_id
                if (obj.containsKey("entity_id")) {
                    entityIds.add(obj.getString("entity_id"));
                }
                // continue to recursively check child fields
                for (String key : obj.keySet()) {
                    collectEntityIds(obj.get(key), entityIds);
                }
                break;
            case ARRAY:
                JsonArray arr = value.asJsonArray();
                for (JsonValue v : arr) {
                    collectEntityIds(v, entityIds);
                }
                break;
            default:
                break;
        }
    }




    
}