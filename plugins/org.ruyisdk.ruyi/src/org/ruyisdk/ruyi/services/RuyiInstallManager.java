package org.ruyisdk.ruyi.services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.ruyisdk.core.exception.PluginException;
import org.ruyisdk.core.ruyi.model.RuyiReleaseInfo;
import org.ruyisdk.core.ruyi.model.SystemInfo;
import org.ruyisdk.core.util.PluginLogger;
import org.ruyisdk.ruyi.Activator;
import org.ruyisdk.ruyi.model.TelemetryMode;
import org.ruyisdk.ruyi.ui.RuyiInstallWizard.InstallationListener;
import org.ruyisdk.ruyi.util.RuyiNetworkUtils;

/**
 * Utility methods for Ruyi installation operations.
 */
public final class RuyiInstallManager {
    private static final PluginLogger LOGGER = Activator.getLogger();

    private RuyiInstallManager() {}

    /**
     * Installs Ruyi.
     *
     * @param destinationDirectory destination directory for the installation
     * @param repoUrl selected remote repository URL
     * @param telemetryMode telemetry setting to apply after install
     * @param monitor progress monitor
     * @param listener installation listener
     */
    public static void install(String destinationDirectory, String repoUrl, TelemetryMode telemetryMode,
                    IProgressMonitor monitor, InstallationListener listener) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, "Installing Ruyi", 100);

        try {
            prepareInstallation(destinationDirectory, listener);

            final var ruyiExecutablePath = downloadRuyi(destinationDirectory, monitor, listener);

            listener.logMessage("Setting executable permissions...");
            setExecutablePermissions(ruyiExecutablePath, listener);

            validateInstallation(listener);
            listener.logMessage("Installation completed successfully");

            configureInstalledRuyi(repoUrl, telemetryMode, listener);
        } finally {
            subMonitor.done();
        }
    }

    private static void prepareInstallation(String destinationDirectory, InstallationListener listener) {
        final var destinationPath = Paths.get(destinationDirectory);
        listener.logMessage("Verifying installation directory: " + destinationPath);
        LOGGER.logInfo("Ruyi package manager install path: " + destinationPath);

        if (!Files.exists(destinationPath)) {
            listener.logMessage("Directory does not exist, attempting to create...");
            try {
                Files.createDirectories(destinationPath);
                listener.logMessage("Directory created successfully.");
            } catch (IOException e) {
                throw RuyiInstallException.directoryCreationFailed(destinationPath.toString(), e);
            }
        } else {
            listener.logMessage("Directory already exists.");
        }
        listener.progressChanged(5, "Directory ready");

        listener.logMessage("Checking available disk space...");
        FileStore store;
        try {
            store = Files.getFileStore(destinationPath);
        } catch (IOException e) {
            throw RuyiInstallException.filesystemError("Cannot access filesystem", e);
        }

        long freeSpaceBytes;
        try {
            freeSpaceBytes = store.getUsableSpace();
        } catch (IOException e) {
            throw RuyiInstallException.filesystemError("Cannot read disk space", e);
        }
        long requiredSpaceBytes = 500L * 1024 * 1024;
        if (freeSpaceBytes < requiredSpaceBytes) {
            BigDecimal requiredMb = BigDecimal.valueOf(requiredSpaceBytes).divide(BigDecimal.valueOf(1024L * 1024L), 1,
                            RoundingMode.HALF_UP);
            BigDecimal freeMb = BigDecimal.valueOf(freeSpaceBytes).divide(BigDecimal.valueOf(1024L * 1024L), 1,
                            RoundingMode.HALF_UP);

            throw RuyiInstallException.insufficientDiskSpace(requiredMb.stripTrailingZeros().toPlainString(),
                            freeMb.stripTrailingZeros().toPlainString());
        }

        BigDecimal freeMb = BigDecimal.valueOf(freeSpaceBytes).divide(BigDecimal.valueOf(1024L * 1024L), 1,
                        RoundingMode.HALF_UP);
        listener.logMessage("磁盘空间充足 (可用: " + freeMb.toPlainString() + " MB)");
        listener.progressChanged(10, "准备完成");
    }

    private static Path downloadRuyi(String destinationDirectory, IProgressMonitor monitor,
                    InstallationListener listener) {
        String archSuffix = SystemInfo.detectArchitecture().getSuffix();
        RuyiReleaseInfo latestRelease = RuyiApi.getLatestRelease(archSuffix);
        Path ruyiExecutablePath = Paths.get(destinationDirectory, "ruyi");

        String[] downloadSources = {latestRelease.getMirrorUrl(), latestRelease.getGithubUrl()};
        Exception lastException = null;
        int[] lastPercent = {0};

        monitor.beginTask("Downloading Ruyi", 100);
        monitor.worked(0);

        for (int i = 0; i < downloadSources.length; i++) {
            String sourceName = i == 0 ? "镜像源" : "GitHub源";
            String ruyiDownloadUrl = downloadSources[i];
            LOGGER.logInfo("Downloading Ruyi from: " + ruyiDownloadUrl + " to: " + ruyiExecutablePath);

            try {
                listener.logMessage(String.format("尝试从%s下载: %s", sourceName, ruyiDownloadUrl));

                RuyiNetworkUtils.downloadFile(ruyiDownloadUrl, ruyiExecutablePath.toString(), monitor,
                                (transferred, total) -> {
                                    if (total <= 0) {
                                        return;
                                    }

                                    int percent = (int) ((double) transferred / total * 100);
                                    percent = Math.min(percent, 100);

                                    if (percent > lastPercent[0]) {
                                        monitor.worked(percent - lastPercent[0]);
                                        lastPercent[0] = percent;
                                    }
                                    listener.progressChanged(percent, String.format("从%s下载中 (%d/%d KB)", sourceName,
                                                    transferred / 1024, total / 1024));
                                });

                if (lastPercent[0] < 100) {
                    monitor.worked(100 - lastPercent[0]);
                    listener.progressChanged(100, "下载完成");
                }

                listener.logMessage("下载成功");
                return ruyiExecutablePath;
            } catch (PluginException e) {
                lastException = e;
                listener.logMessage(String.format("%s下载失败: %s", sourceName, e.getMessage()));

                if (i < downloadSources.length - 1) {
                    try {
                        Files.deleteIfExists(ruyiExecutablePath);
                        listener.logMessage("已清理失败下载的部分文件");
                    } catch (IOException ioEx) {
                        listener.logMessage("清理部分文件失败: " + ioEx.getMessage());
                    }
                }
            }
        }

        throw RuyiInstallException.downloadFailed(String.format("所有下载尝试均失败。最后错误: %s",
                        lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    private static void setExecutablePermissions(Path executablePath, InstallationListener listener) {
        File file = executablePath.toFile();

        if (!file.exists()) {
            throw RuyiInstallException.fileNotFound(executablePath.toString());
        }

        if (file.canExecute()) {
            if (listener != null) {
                listener.logMessage(executablePath + " is already executable");
            }
            return;
        }

        boolean success = file.setExecutable(true, false);
        if (!success) {
            try {
                Path filePath = file.toPath();
                Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(filePath));
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(filePath, perms);

                if (listener != null) {
                    listener.logMessage("Successfully set executable permissions using POSIX API: " + executablePath);
                }
            } catch (IOException | UnsupportedOperationException e) {
                throw RuyiInstallException.permissionFailed(executablePath.toString(), e);
            }
        } else if (listener != null) {
            listener.logMessage("Successfully set executable permissions: " + executablePath);
        }

        if (!file.canExecute()) {
            throw RuyiInstallException.permissionVerifyFailed(executablePath.toString());
        }
    }

    private static void validateInstallation(InstallationListener listener) {
        listener.logMessage("Validating installation...");

        final var version = RuyiCli.getInstalledVersion();
        if (version == null) {
            throw RuyiInstallException.validationFailed("There are problems in the operation ruyi -V.");
        }

        listener.logMessage("Ruyi " + version.toString() + " install successful.");
    }

    private static void configureInstalledRuyi(String repoUrl, TelemetryMode telemetryMode,
                    InstallationListener listener) {
        listener.logMessage("Ruyi Config...");

        Objects.requireNonNull(repoUrl, "No repository configuration selected");
        RuyiCli.setRepoRemote(repoUrl);
        listener.logMessage("ruyi config set repo.remote successful. \n repo.remote is:" + RuyiCli.getRepoRemote());

        RuyiCli.updatePackageIndex();
        listener.logMessage("ruyi update successful");

        final var effectiveTelemetry = telemetryMode == null ? TelemetryMode.ON : telemetryMode;
        RuyiCli.setTelemetry(effectiveTelemetry);
        listener.logMessage("ruyi telemetry set successful: " + RuyiCli.getTelemetryMode());

        listener.logMessage("Config successful");
    }
}
