package org.ruyisdk.packages.model;

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
        final var root = new TreeNode(rootLabel, null);
        final var tree = RuyiCli.parsePackageTreeFromString(output);
        for (final var category : tree) {
            final var categoryNode = new TreeNode(category.getName(), null);
            root.addChild(categoryNode);

            for (final var pkg : category.getPackages()) {
                final var packageNode = new TreeNode(pkg.getName(), null);
                categoryNode.addChild(packageNode);

                for (final var version : pkg.getVersions()) {
                    final var versionNode =
                            new TreeNode(version.getDisplayName(), null, version.getPackageRef());
                    versionNode.setLeaf(true);
                    versionNode.setDownloaded(version.isInstalled());
                    packageNode.addChild(versionNode);
                }
            }
        }
        return root;
    }
}
