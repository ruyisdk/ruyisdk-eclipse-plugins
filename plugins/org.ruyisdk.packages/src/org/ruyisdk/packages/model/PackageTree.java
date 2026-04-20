package org.ruyisdk.packages.model;

import org.ruyisdk.packages.JsonParser;
import org.ruyisdk.ruyi.services.RuyiCli;

/**
 * Synchronous data-access object for the package tree. All methods block until the result is
 * available; the caller is responsible for running them off the UI thread.
 */
public class PackageTree {

    /**
     * Loads the package tree from the Ruyi CLI.
     *
     * @param entityId device entity ID to filter by, or {@code null} for all packages
     * @return the root {@link TreeNode} of the parsed tree
     */
    public static TreeNode loadPackages(String entityId) {
        final String output;
        if (entityId != null) {
            output = RuyiCli.listRelatedToEntity("device:" + entityId);
        } else {
            output = RuyiCli.listAllPackages();
        }
        final var rootLabel = entityId != null ? entityId : "All Packages";
        return JsonParser.parseRawOutput(output, rootLabel);
    }
}
