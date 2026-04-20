package org.ruyisdk.packages.viewmodel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.ruyisdk.packages.model.DeviceList;
import org.ruyisdk.packages.model.PackageOperation;
import org.ruyisdk.packages.model.PackageTree;
import org.ruyisdk.packages.model.TreeNode;
import org.ruyisdk.ruyi.model.DeviceEntityInfo;

/**
 * Main ViewModel for the Package Explorer view.
 *
 * <p>
 * Owns the {@link DeviceList} and {@link PackageTree}, coordinates asynchronous loading via Eclipse
 * {@link Job}s, and exposes observable properties that the view binds to. All property-change
 * events fire on the SWT UI thread.
 */
public class PackageExplorerViewModel extends BaseViewModel {

    // Property name constants
    public static final String PROP_PACKAGE_ROOT = "packageRoot";
    public static final String PROP_CHOSEN_DEVICE = "chosenDevice";
    public static final String PROP_DEVICE_INFO_TEXT = "deviceInfoText";
    public static final String PROP_DEVICES = "devices";
    public static final String PROP_DEVICE_LIST_ERROR = "deviceListErrorMessage";
    public static final String PROP_INFO_PANE_TEXT = "infoPaneText";
    public static final String PROP_PACKAGES_LOADING = "packagesLoading";
    public static final String PROP_ERROR = "error";

    private final CheckStateTracker checkState = new CheckStateTracker();

    /**
     * Creates a new ViewModel with the given UI-thread executor.
     *
     * @param uiExecutor posts a {@link Runnable} to the UI thread
     */
    public PackageExplorerViewModel(Consumer<Runnable> uiExecutor) {
        super(uiExecutor);
    }

    private TreeNode packageRoot;
    private DeviceEntityInfo chosenDevice;
    private String deviceInfoText = "Current device: <a>(none selected)</a>";
    private List<DeviceEntityInfo> devices = List.of();
    private String deviceListErrorMessage;
    private String infoPaneText = "Select a package to see details.";
    private boolean packagesLoading;

    public TreeNode getPackageRoot() {
        return packageRoot;
    }

    public DeviceEntityInfo getChosenDevice() {
        return chosenDevice;
    }

    public String getDeviceInfoText() {
        return deviceInfoText;
    }

    public List<DeviceEntityInfo> getDevices() {
        return devices;
    }

    public String getDeviceListErrorMessage() {
        return deviceListErrorMessage;
    }

    public String getInfoPaneText() {
        return infoPaneText;
    }

    public boolean isPackagesLoading() {
        return packagesLoading;
    }

    /** Returns whether the given node should appear checked. */
    public boolean isNodeChecked(TreeNode node) {
        return checkState.isEffectivelyChecked(node);
    }

    /** Returns whether the given non-leaf node should appear grayed (partially checked). */
    public boolean isNodeGrayed(TreeNode node) {
        return checkState.isEffectivelyGrayed(node);
    }

    /** Kick off initial data loading (packages + device list). */
    public void initialize() {
        loadPackagesAsync(() -> {
        });
        loadDevicesAsync();
    }

    /** Reload the package list for the currently chosen device. */
    public void refreshPackages() {
        loadPackagesAsync(() -> {
        });
    }

    /**
     * Set the chosen device, update the device-info label, and reload packages.
     *
     * @param device the new device (may be {@code null} for "all packages")
     * @param onLoaded called on the UI thread once the package list has been loaded (or aborted)
     */
    public void setChosenDeviceAndReload(@Nullable DeviceEntityInfo device, Runnable onLoaded) {
        final var old = this.chosenDevice;
        this.chosenDevice = device;
        computeDeviceInfoText();
        firePropertyChange(PROP_CHOSEN_DEVICE, old, device);
        loadPackagesAsync(onLoaded);
    }

    /** Update the info-pane text for the given tree node (may be {@code null}). */
    public void updateSelectedNode(TreeNode node) {
        final var old = this.infoPaneText;
        this.infoPaneText = computeInfoPaneText(node);
        firePropertyChange(PROP_INFO_PANE_TEXT, old, infoPaneText);
    }

    /** Propagate a check-state change recursively from {@code node} downward. */
    public void setNodeChecked(TreeNode node, boolean checked) {
        setSelectedRecursively(node, checked);
    }

    /**
     * Collect pending install/uninstall operations by comparing the user-requested check state against
     * the actual installed state of every leaf node.
     */
    public List<PackageOperation> collectPendingOperations() {
        final var ops = new ArrayList<PackageOperation>();
        for (final var leaf : collectAllLeafNodes()) {
            if (checkState.isEffectivelyChecked(leaf) && !leaf.isDownloaded()) {
                ops.add(new PackageOperation(leaf.getPackageRef(), false));
            } else if (leaf.isDownloaded() && !checkState.isEffectivelyChecked(leaf)) {
                ops.add(new PackageOperation(leaf.getPackageRef(), true));
            }
        }
        return ops;
    }

    /** Build a human-readable confirmation message for a list of pending operations. */
    public String getConfirmationMessage(List<PackageOperation> operations) {
        final var toInstall = operations.stream().filter(op -> !op.uninstall()).toList();
        final var toUninstall = operations.stream().filter(PackageOperation::uninstall).toList();

        final var sb = new StringBuilder();
        if (!toInstall.isEmpty()) {
            sb.append("Install ").append(toInstall.size()).append(" package(s):\n");
            for (final var op : toInstall) {
                sb.append("  + ").append(op.packageRef()).append("\n");
            }
        }
        if (!toUninstall.isEmpty()) {
            if (!toInstall.isEmpty()) {
                sb.append("\n");
            }
            sb.append("Uninstall ").append(toUninstall.size()).append(" package(s):\n");
            for (final var op : toUninstall) {
                sb.append("  - ").append(op.packageRef()).append("\n");
            }
        }
        return sb.toString();
    }

    /** Resolve the Ruyi cache directory for compressed package downloads. */
    public String getPackageDownloadDir() {
        final var cacheHome = System.getenv("XDG_CACHE_HOME");
        if (cacheHome != null && !cacheHome.isEmpty()) {
            return cacheHome + "/ruyi/distfiles";
        }
        return System.getProperty("user.home") + "/.cache/ruyi/distfiles";
    }

    /** Resolve the Ruyi data directory for image/blob downloads. */
    public String getImagesDownloadDir() {
        final var dataHome = System.getenv("XDG_DATA_HOME");
        if (dataHome != null && !dataHome.isEmpty()) {
            return dataHome + "/ruyi/blobs";
        }
        return System.getProperty("user.home") + "/.local/share/ruyi/blobs";
    }

    private void loadPackagesAsync(Runnable onFinished) {
        final var entityId = chosenDevice != null ? chosenDevice.getEntityId() : null;
        final var jobLabel = chosenDevice != null ? "Loading packages for " + chosenDevice.getLabel()
                        : "Loading all packages";

        setPackagesLoading(true);

        final var job = Job.create(jobLabel, monitor -> {
            if (monitor.isCanceled()) {
                uiExecutor.accept(() -> {
                    setPackagesLoading(false);
                    onFinished.run();
                });
                return Status.CANCEL_STATUS;
            }

            try {
                final var root = PackageTree.loadPackages(entityId);

                if (monitor.isCanceled()) {
                    uiExecutor.accept(() -> {
                        setPackagesLoading(false);
                        onFinished.run();
                    });
                    return Status.CANCEL_STATUS;
                }

                uiExecutor.accept(() -> {
                    try {
                        setPackageRoot(root);
                    } finally {
                        setPackagesLoading(false);
                        onFinished.run();
                    }
                });

                return Status.OK_STATUS;
            } catch (Exception e) {
                final var msg = e.getMessage();
                uiExecutor.accept(() -> {
                    setPackagesLoading(false);
                    firePropertyChange(PROP_ERROR, null, "Failed to load packages: " + msg);
                    onFinished.run();
                });
                return Status.CANCEL_STATUS; // avoid Eclipse error dialog
            }
        });
        job.schedule();
    }

    private void loadDevicesAsync() {
        final var job = Job.create("Loading device entities", monitor -> {
            try {
                final var fetched = DeviceList.loadDevices();
                uiExecutor.accept(() -> {
                    setDevices(fetched);
                    setDeviceListErrorMessage(null);
                });
                return Status.OK_STATUS;
            } catch (Exception e) {
                final var msg = e.getMessage();
                uiExecutor.accept(() -> setDeviceListErrorMessage(msg));
                return Status.error("Failed to load device data: " + msg, e);
            }
        });
        job.schedule();
    }

    private void setPackageRoot(TreeNode root) {
        final var old = this.packageRoot;
        this.packageRoot = root;
        checkState.clear();
        firePropertyChange(PROP_PACKAGE_ROOT, old, root);
        updateSelectedNode(null);
    }

    private void setPackagesLoading(boolean loading) {
        final var old = this.packagesLoading;
        this.packagesLoading = loading;
        firePropertyChange(PROP_PACKAGES_LOADING, old, loading);
    }

    private void setDevices(List<DeviceEntityInfo> newDevices) {
        final var old = this.devices;
        this.devices = List.copyOf(newDevices);
        firePropertyChange(PROP_DEVICES, old, this.devices);
    }

    private void setDeviceListErrorMessage(String msg) {
        final var old = this.deviceListErrorMessage;
        this.deviceListErrorMessage = msg;
        firePropertyChange(PROP_DEVICE_LIST_ERROR, old, msg);
    }

    private void computeDeviceInfoText() {
        final var old = this.deviceInfoText;
        if (chosenDevice == null) {
            deviceInfoText = "Current device: <a>(none selected)</a>";
        } else {
            var label = chosenDevice.getLabel();
            final var variantCount = chosenDevice.getRelatedRefs().size();
            if (variantCount > 0) {
                label += " (" + variantCount + " variant" + (variantCount > 1 ? "s" : "") + ")";
            }
            label = label.replace("&", "&amp;").replace("<", "&lt;");
            deviceInfoText = "Current device: <a>" + label + "</a>";
        }
        firePropertyChange(PROP_DEVICE_INFO_TEXT, old, deviceInfoText);
    }

    private String computeInfoPaneText(TreeNode node) {
        if (node == null) {
            return "Select a package to see details.";
        }

        final var sb = new StringBuilder();
        if (node.isLeaf()) {
            sb.append("Version: ").append(node.getName()).append("\n");
            if (node.getPackageRef() != null) {
                sb.append("Reference: ").append(node.getPackageRef()).append("\n");
            }
            sb.append("Status: ").append(node.isDownloaded() ? "Installed" : "Not installed").append("\n");
        } else {
            sb.append("Name: ").append(node.getName()).append("\n");
            final var children = node.getChildren();
            if (children != null && !children.isEmpty()) {
                sb.append("Children: ").append(children.size()).append("\n\n");
                for (final var child : children) {
                    sb.append("  - ").append(child.getName());
                    if (child.isLeaf()) {
                        sb.append(child.isDownloaded() ? "  [installed]" : "");
                    }
                    sb.append("\n");
                }
            }
        }
        if (node.getDetails() != null) {
            sb.append("\nDetails: ").append(node.getDetails());
        }
        return sb.toString();
    }

    private List<TreeNode> collectAllLeafNodes() {
        if (packageRoot == null) {
            return List.of();
        }

        final var result = new ArrayList<TreeNode>();
        final var stack = new ArrayDeque<TreeNode>();
        stack.push(packageRoot);
        while (!stack.isEmpty()) {
            final var node = stack.pop();
            if (node.isLeaf()) {
                result.add(node);
            } else {
                final var children = node.getChildren();
                for (final var child : children) {
                    stack.push(child);
                }
            }
        }
        return result;
    }

    private void setSelectedRecursively(TreeNode node, boolean checked) {
        if (node.isLeaf()) {
            checkState.setSelected(node, checked);
        } else {
            final var children = node.getChildren();
            for (final var child : children) {
                setSelectedRecursively(child, checked);
            }
        }
    }
}
