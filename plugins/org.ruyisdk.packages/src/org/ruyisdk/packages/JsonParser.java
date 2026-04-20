package org.ruyisdk.packages;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.ruyisdk.packages.model.TreeNode;
import org.ruyisdk.ruyi.services.RuyiCli;

/**
 * JSON parser for package tree data.
 */
public class JsonParser {

    /**
     * Parses raw porcelain CLI output into a tree structure. Each non-empty line that starts with
     * '{' and is not a log entry is treated as a JSON object.
     *
     * @param rawOutput raw CLI output (newline-separated JSON objects)
     * @param rootLabel label for root node
     * @return parsed tree node
     */
    public static TreeNode parseRawOutput(String rawOutput, String rootLabel) {
        final var root = new TreeNode(rootLabel, null);
        if (rawOutput == null || rawOutput.isEmpty()) {
            return root;
        }
        for (final var line : rawOutput.split("\\R")) {
            final var trimmed = line.trim();
            if (trimmed.isEmpty() || !trimmed.startsWith("{")
                    || trimmed.contains("\"ty\":\"log-v1\"")) {
                continue;
            }
            try (final var reader = Json.createReader(new StringReader(trimmed))) {
                final var value = reader.read();
                if (value instanceof JsonObject) {
                    parseJsonObject((JsonObject) value, root);
                }
            }
        }
        return root;
    }

    private static void parseJsonObject(JsonObject rootObject, TreeNode root) {
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
                String remark =
                        (remarks != null && !remarks.isEmpty()) ? " [" + remarks.getString(0) + "]"
                                : "";

                boolean isDownloaded = versionObject.getBoolean("is_installed", false);

                final var packageRef = String.format("%s(%s)", name, semver);
                final var versionNode = new TreeNode(semver + remark, null, packageRef);
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

    /**
     * Finds installed toolchain for board.
     *
     * @param boardName board name
     * @return toolchain path or null
     */
    public static String findInstalledToolchainForBoard(String boardName) {
        if (boardName == null || boardName.trim().isEmpty()) {
            return null;
        }

        String entity = boardName.startsWith("device:") ? boardName : "device:" + boardName;

        final var rawJson = RuyiCli.listRelatedToEntity(entity).replace("\n", "").replace("\r", "");
        if (rawJson.trim().isEmpty()) {
            return null; // No output from ruyi
        }
        // The output is a stream of JSON objects, wrap it to be a valid JSON array.
        String jsonData = "[" + rawJson.replace("}{", "},{") + "]";

        // Parse the JSON to find the toolchain
        try (JsonReader reader = Json.createReader(new StringReader(jsonData))) {
            JsonArray jsonArray = reader.readArray();
            for (JsonValue value : jsonArray) {
                if (value.getValueType() != JsonValue.ValueType.OBJECT) {
                    continue;
                }

                JsonObject pkgObject = (JsonObject) value;
                String category = pkgObject.getString("category", "");

                if ("toolchain".equals(category)) {
                    JsonArray versions = pkgObject.getJsonArray("vers");
                    if (versions == null) {
                        continue;
                    }

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

        return null; // No installed toolchain found
    }
}
