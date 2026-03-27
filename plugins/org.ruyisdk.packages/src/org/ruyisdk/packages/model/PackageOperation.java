package org.ruyisdk.packages.model;

/**
 * A single package install or uninstall operation.
 *
 * @param packageRef the package reference (e.g. "name(version)")
 * @param uninstall {@code true} for uninstall, {@code false} for install
 */
public record PackageOperation(String packageRef, boolean uninstall) {
}
