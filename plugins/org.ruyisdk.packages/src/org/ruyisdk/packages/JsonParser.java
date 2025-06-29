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
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
    public static String findInstalledToolchainForBoard(String boardName) {
        if (boardName == null || boardName.trim().isEmpty()) {
            return null;
        }

        String ruyiPath = RuyiFileUtils.getInstallPath() + "/ruyi";
        String entity = boardName.startsWith("device:") ? boardName : "device:" + boardName;
        String command = ruyiPath + " --porcelain list --related-to-entity " + entity;

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.environment().put("RUYI_EXPERIMENTAL", "true");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read the command's output stream
            StringBuilder jsonStream = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonStream.append(line);
                }
            }
            process.waitFor();

            String rawJson = jsonStream.toString();
            if (rawJson.trim().isEmpty()) {
                return null; // No output from ruyi
            }
            // The output is a stream of JSON objects, wrap it to be a valid JSON array.
            String jsonData = "[" + rawJson.replace("}{", "},{") + "]";

            // Parse the JSON to find the toolchain
            try (JsonReader reader = Json.createReader(new StringReader(jsonData))) {
                JsonArray jsonArray = reader.readArray();
                for (JsonValue value : jsonArray) {
                    if (value.getValueType() != JsonValue.ValueType.OBJECT) continue;

                    JsonObject pkgObject = (JsonObject) value;
                    String category = pkgObject.getString("category", "");

                    if ("toolchain".equals(category)) {
                        JsonArray versions = pkgObject.getJsonArray("vers");
                        if (versions == null) continue;

                        for (JsonValue verValue : versions) {
                            JsonObject verObject = (JsonObject) verValue;
                            if (verObject.getBoolean("is_installed", false)) {
                                // Extract the full package name from the install command
                                // to match the expected format.
                                if (verObject.containsKey("install_command")) {
                                    String installCommand = verObject.getString("install_command");
                                    int lastSpace = installCommand.lastIndexOf(' ');
                                    if (lastSpace != -1) {
                                        return installCommand.substring(lastSpace + 1);
                                    }
                                }
                                // Fallback if install_command is missing
                                String pkgName = pkgObject.getString("name");
                                String pkgVersion = verObject.getString("semver");
                                return pkgName + "-" + pkgVersion;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return null; // No installed toolchain found
    }   
}