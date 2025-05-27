package org.ruyisdk.ruyi.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.ruyisdk.core.ruyi.model.RepoConfig;
import org.ruyisdk.core.ruyi.model.RuyiReleaseInfo;
import org.ruyisdk.core.ruyi.model.RuyiVersion;
import org.ruyisdk.core.ruyi.model.SystemInfo;
import org.ruyisdk.ruyi.services.RuyiProperties.TelemetryStatus;
import org.ruyisdk.ruyi.ui.RuyiInstallWizard.InstallationListener;
import org.ruyisdk.ruyi.util.PathUtils;
import org.ruyisdk.ruyi.util.RuyiFileUtils;
import org.ruyisdk.ruyi.util.RuyiLogger;
import org.ruyisdk.ruyi.util.RuyiNetworkUtils;

public class RuyiInstallManager {
	private final RuyiLogger logger;

	private static final Path DEFAULT_INSTALL_PATH = RuyiFileUtils.getDefaultInstallPath(); // 经过处理的绝对路径path
	// System.getProperty("user.home") + File.separator + ".ruyi";
	private String installPath; // UI界面用户自定义的path

	// ruyi 下载相关参数
//	private String installedVersion; // 本地已安装ruyi版本
	private RuyiReleaseInfo latestRelease; // 接口获取的 ruyi Release信息，含版本和下载url
//	private String latestVersion;

	// ruyi 配置相关参数
	private RepoConfig[] repoUrls; // 存储库地址，支持多个
//	private String repositoryUrl;
	private TelemetryStatus telemetryStatus;

	public RuyiInstallManager(RuyiLogger logger) {
		this.logger = logger;
		this.installPath = DEFAULT_INSTALL_PATH.toString();
		this.telemetryStatus = TelemetryStatus.ON;
	}

	// ========== 安装管理方法 ==========
	public void install(IProgressMonitor monitor, InstallationListener listener) throws Exception {
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Installing Ruyi", 100);

		try {
			// 阶段1: 准备安装 
			prepareInstallation(listener);

			// 清理/备份旧文件

			// 阶段2: 下载Ruyi (100%)	
			downloadRuyi(monitor, listener);
			
			// 阶段3: 完成安装 
//	         // 文件预处理
//			listener.logMessage("Rename ruyi file ...");
//          prepareExecutable(Paths.get(installPath, latestRelease.getFilename()), 
//          		Paths.get(installPath,  "ruyi"),
//          		listener);
			
			// 环境变量配置
//			listener.logMessage("Setting up environment...");
//			addToPathIfNeeded(installPath, listener);
			// 为ruyi设置可执行权限
			listener.logMessage("Setting executable permissions...");
			setExecutablePermissions(installPath, listener);
			listener.logMessage("Ruyi " + getVersionFromPackage() + " installed successfully");


			// 阶段4: 验证安装 
			validateInstallation(installPath, listener);
			listener.logMessage("Installation completed successfully");

			// 阶段5: 安装后设置 
			ruyiConfig(installPath, listener);

		} catch (Exception e) {
			listener.logMessage("Installation failed: " + e.getMessage());
			throw e;
		} finally {
			subMonitor.done();
		}
	}

	private void prepareInstallation(InstallationListener listener) throws Exception {
		listener.logMessage("Verifying installation directory: " + installPath);
		Path path = Paths.get(installPath);
		System.out.println("Ruyi包管理器安装地址：" + path);

		// 1. 确保目录存在（不存在则创建）
		if (!Files.exists(path)) {
			listener.logMessage("Directory does not exist, attempting to create...");
			try {
				Files.createDirectories(path); // 递归创建所有缺失的父目录
				listener.logMessage("Directory created successfully.");
			} catch (IOException e) {
				throw new Exception("Failed to create installation directory: " + e.getMessage(), e);
			}
		} else {
			listener.logMessage("Directory already exists.");
		}
		listener.progressChanged(5, "Directory ready");

		// 2. 检查磁盘空间（使用 NIO API）
		listener.logMessage("Checking available disk space...");
		FileStore store;
		try {
			store = Files.getFileStore(path);
		} catch (IOException e) {
			throw new Exception("Cannot access filesystem: " + e.getMessage(), e);
		}

		long freeSpaceBytes = store.getUsableSpace();
		long requiredSpaceBytes = 500L * 1024 * 1024;

		// 数值比较
		if (freeSpaceBytes < requiredSpaceBytes) {
			BigDecimal requiredMB = BigDecimal.valueOf(requiredSpaceBytes).divide(BigDecimal.valueOf(1024L * 1024L), 1,
					RoundingMode.HALF_UP);
			BigDecimal freeMB = BigDecimal.valueOf(freeSpaceBytes).divide(BigDecimal.valueOf(1024L * 1024L), 1,
					RoundingMode.HALF_UP);

			throw new Exception(String.format("需要至少 %s MB 空间，当前可用 %s MB",
					requiredMB.stripTrailingZeros().toPlainString(), freeMB.stripTrailingZeros().toPlainString()));
		}

		// 构造安全日志消息
		BigDecimal freeMB = BigDecimal.valueOf(freeSpaceBytes).divide(BigDecimal.valueOf(1024L * 1024L), 1,
				RoundingMode.HALF_UP);
		String msg = "磁盘空间充足 (可用: " + freeMB.toPlainString() + " MB)";
		listener.logMessage(msg);
		listener.progressChanged(10, "准备完成");
	}
	
	private void downloadRuyi(IProgressMonitor monitor, InstallationListener listener) throws Exception {
		String archSuffix = SystemInfo.detectArchitecture().getSuffix();
		latestRelease = RuyiAPI.getLatestRelease(archSuffix);
		
//		String ruyiInstallPath = Paths.get(installPath, latestRelease.getFilename()).toString();
//		Path ruyiInstallPath = Paths.get(RuyiFileUtils.getInstallPath().toString(), "ruyi");
		Path ruyiInstallPath = Paths.get(installPath, "ruyi");
//		Path parent = ruyiInstallPath.getParent();
//		if (!Files.exists(parent)) {
//			Files.createDirectories(parent);
//		}

		// 定义下载源数组（按优先级排序）
		String[] downloadSources = { latestRelease.getMirrorUrl(), // 优先尝试镜像地址
				latestRelease.getGithubUrl() // 镜像失败后尝试GitHub
		};

		Exception lastException = null;
		int[] lastPercent = {0}; // 用于记录上次进度（数组形式以便在lambda内修改）
		
		// 初始化进度为0
	    monitor.beginTask("Downloading Ruyi", 100);
	    monitor.worked(0);

		for (int i = 0; i < downloadSources.length; i++) {
			String sourceName = i == 0 ? "镜像源" : "GitHub源";
			String ruyiDownloadUrl = downloadSources[i];
			System.out.println("ruyiDownloadUrl===" + ruyiDownloadUrl);
			System.out.println("ruyiInstallPath===" + ruyiInstallPath);

			try {
				listener.logMessage(String.format("尝试从%s下载: %s", sourceName, ruyiDownloadUrl));

				RuyiNetworkUtils.downloadFile(ruyiDownloadUrl, ruyiInstallPath.toString(), monitor,
						(transferred, total) -> {
							
							if (total <= 0) return; // 防止除零错误
							
							int percent = (int) ((double) transferred / total * 100);
							percent = Math.min(percent, 100); // 确保不超过100%
							
//							System.out.println(transferred+"/"+total+"="+percent+"||"+lastPercent[0]);
							
							 // 仅当进度实际变化时更新
		                    if (percent > lastPercent[0]) {
		                    	monitor.worked(percent - lastPercent[0]); // 更新增量进度
		                        lastPercent[0] = percent;
		                    }
							listener.progressChanged(percent,
									String.format("从%s下载中 (%d/%d KB)", sourceName, transferred / 1024, total / 1024));
						});
				
				  // 下载完成后确保进度为100%
	            if (lastPercent[0] < 100) {
	                monitor.worked(100 - lastPercent[0]);
	                listener.progressChanged(100, "下载完成");
	            }
	            
				listener.logMessage("下载成功");		
				return; // 下载成功则直接返回

			} catch (Exception e) {
				lastException = e;
				listener.logMessage(String.format("%s下载失败: %s", sourceName, e.getMessage()));

				// 如果是最后一次尝试，不再删除文件（保留可能的部分下载）
				if (i < downloadSources.length - 1) {
					try {
						Files.deleteIfExists(ruyiInstallPath);
						listener.logMessage("已清理失败下载的部分文件");
					} catch (IOException ioEx) {
						listener.logMessage("清理部分文件失败: " + ioEx.getMessage());
					}
				}
			}
		}

		// 所有下载源均失败
		throw new Exception(
				String.format("所有下载尝试均失败。最后错误: %s", lastException != null ? lastException.getMessage() : "未知错误"),
				lastException);
	}

	private void prepareExecutable(Path source, Path target, InstallationListener listener) throws Exception {
		// 1. 复制/重命名文件
		listener.logMessage("Preparing executable...");
//        Files.deleteIfExists(target); // 删除已存在的旧文件
//        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		// 2. 设置可执行权限 (Unix-like系统)
		try {
			if (!target.toFile().setExecutable(true)) {
				throw new Exception("Failed to set executable permission");
			}
		} catch (SecurityException e) {
			throw new Exception("Permission denied when setting executable flag", e);
		}

		listener.logMessage("Executable prepared: " + target);
	}

	private void addToPathIfNeeded(String path, InstallationListener listener) throws Exception {
		Path ruyiExecutable = Paths.get(path, "ruyi");

		// 1. 检查是否已配置
		if (PathUtils.isPathConfigured(path) && Files.isExecutable(ruyiExecutable)) {
			listener.logMessage("PATH already contains: " + path);
			return;
		}

		// 2. 弹出图形化确认
		if (!showConfirmationDialog("PATH Configuration", "需要添加安装目录到PATH变量并设置可执行权限")) {
			throw new Exception("User declined configuration");
		}

		// 3. 准备配置命令（包含PATH设置和权限设置）
		String command = String.format("echo 'export PATH=\"%s:$PATH\"' >> ~/.bashrc && " + // 添加PATH
				"chmod +x %s && " + // 设置可执行权限
				"source ~/.bashrc", // 立即生效
				path, ruyiExecutable.toString());

		// 4. 执行特权命令
		executeWithPrivilege(command, listener);

		// 5. 验证权限
		if (!Files.isExecutable(ruyiExecutable)) {
			// 详细错误诊断
			String perms = Files.exists(ruyiExecutable)
					? "Current permissions: " + Files.getPosixFilePermissions(ruyiExecutable)
					: "File does not exist";
			throw new Exception(String.format("Permission verification failed for: %s\n%s", ruyiExecutable, perms));
		}
//        listener.logMessage("Successfully set executable permission for: " + ruyiExecutable);
		listener.logMessage("验证成功: " + ruyiExecutable + " 已获得可执行权限");
	}
	
	private void setExecutablePermissions(String path, InstallationListener listener) throws Exception {
		Path ruyipath = Paths.get(path, "ruyi");
		File file = new File(ruyipath.toString());
	    
	    // 首先检查文件是否存在
	    if (!file.exists()) {
	        throw new FileNotFoundException("File not found: " + path);
	    }
	    
	    // 检查是否已经是可执行文件
	    if (file.canExecute()) {
	        if (listener != null) {
	            listener.logMessage(path + " is already executable");
	        }
	        return;
	    }
	    
	    // 尝试使用Java标准方法设置可执行权限
	    boolean success = file.setExecutable(true, false); // false表示不限制只有所有者可以设置
	    
	    if (!success) {
	        // 如果标准方法失败，尝试使用POSIX权限设置（适用于Linux/Unix）
	        try {
	            Path filePath = file.toPath();
	            Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(filePath));
	            perms.add(PosixFilePermission.OWNER_EXECUTE);
	            perms.add(PosixFilePermission.GROUP_EXECUTE);
	            perms.add(PosixFilePermission.OTHERS_EXECUTE);
	            Files.setPosixFilePermissions(filePath, perms);
	            
	            if (listener != null) {
	                listener.logMessage("Successfully set executable permissions using POSIX API: " + path);
	            }
	        } catch (UnsupportedOperationException e) {
	            // 非POSIX文件系统（如Windows）
	            throw new Exception("Failed to set executable permissions on non-POSIX system: " + path);
	        }
	    } else {
	        if (listener != null) {
	            listener.logMessage("Successfully set executable permissions: " + path);
	        }
	    }
	    
	    // 最终确认权限是否设置成功
	    if (!file.canExecute()) {
	        throw new Exception("Failed to set executable permissions for: " + path + 
	                          ". You may need root/sudo privileges.");
	    }
	}

	private boolean showConfirmationDialog(String title, String message) {
		try {
			// 尝试使用zenity图形对话框
			return new ProcessBuilder("zenity", "--question", "--title=" + title, "--text=" + message, "--width=300")
					.start().waitFor() == 0;
		} catch (Exception e) {
			// 回退到控制台确认
			System.out.printf("%s (y/N): ", message);
			return new Scanner(System.in).nextLine().equalsIgnoreCase("y");
		}
	}

	private void executeWithPrivilege(String command, InstallationListener listener) throws Exception {
//        String[] cmd = {
//            "pkexec",
//            "bash",
//            "-c",
//            String.format("su $SUDO_USER -c '%s'", command)
//        };

		// 先扩展所有~符号
		String expandedCmd = command.replace("~", System.getProperty("user.home"));

		String[] cmd = { "pkexec", "bash", "-c",
				// 直接执行命令（不再通过su，因为pkexec已经提权）
				expandedCmd };

		try {
			Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();

			// 读取输出
			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					listener.logMessage(line);
				}
			}

			int exitCode = proc.waitFor();
			if (exitCode != 0) {
				throw new Exception(String.format("Command failed (exit code %d): %s\nOutput: %s", exitCode, command,
						output.toString()));
			}
		} catch (IOException | InterruptedException e) {
			throw new Exception("Failed to execute privileged command: " + e.getMessage(), e);
		}
	}

	private void validateInstallation(String ruyiPath, InstallationListener listener) throws Exception {
		listener.logMessage("Validating installation...");
		
		// 执行 ruyi -V 命令验证
		RuyiVersion version = RuyiCommand.getInstalledVersion(ruyiPath);
		if (version == null) {
			listener.logMessage("There are problems in the operation ruyi -V.");
		} 

		listener.logMessage("Ruyi "+version.toString()+" install successful.");
	}
	
	// 安装后设置
	private void ruyiConfig(String ruyiPath, InstallationListener listener) throws Exception {
		listener.logMessage("Ruyi Config...");

		// 1.设置存储库：ruyi config set repo.remote <url>
		RuyiCommand.setRepoRemote(ruyiPath, repoUrls[0].getUrl() );
		listener.logMessage("ruyi config set repo.remote successful. \n repo.remote is:"+RuyiCommand.getRepoRemote(ruyiPath));
		
		// 2.ruyi update：更新存储库软件包索引到本地
		RuyiCommand.updateRuyi(ruyiPath);
		listener.logMessage("ruyi update successful");
		
		// 3.遥测设置：ruyi telemetry consent/ ruyi telemetry optout 遥测开启/拒绝（数据已匿名化，建议开启）
		RuyiCommand.setTelemetry(ruyiPath, telemetryStatus);
		listener.logMessage("ruyi telemetry set successful:" + RuyiCommand.getTelemetryStatus(ruyiPath));
		
		listener.logMessage("Config successful");
	}
	

	private void cleanFailedDownload(Path file, InstallationListener listener) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			listener.logMessage("Warning: Failed to clean up temporary file - " + e.getMessage());
		}
	}

	// ========== 版本管理方法 ==========

//	public boolean isInstalled() throws Exception {
//		Path ruyiBin = Paths.get(installPath, "bin", "ruyi");
//		return RuyiFileUtils.isExecutable(ruyiBin.toString());
//	}
//
//	public String getInstalledVersion() throws Exception {
//		if (installedVersion != null) {
//			return installedVersion;
//		}
//
//		if (!isInstalled()) {
//			return null;
//		}
//
//		Path versionFile = Paths.get(installPath, "VERSION");
//		if (Files.exists(versionFile)) {
//			installedVersion = RuyiFileUtils.readFileContent(versionFile.toString()).trim();
//			return installedVersion;
//		}
//		return "unknown";
//	}


//	public void setInstalledVersion(String version) {
//		this.installedVersion = version;
//	}
//	public void setLatestRelease(RuyiReleaseInfo version) {
//		this.latestRelease = version;
//	}
//	public void setLatestVersion(String version) {
//		this.latestVersion = version;
//	}

//	public String getLatestVersion() throws Exception {
//		if (latestVersion != null) {
//			return latestVersion;
//		}
//
//		String versionUrl = repositoryUrl + "/version/latest";
//		latestVersion = RuyiNetworkUtils.fetchStringContent(versionUrl, null);
//		return latestVersion;
//	}
//
//	public String getChangelog(String version) throws Exception {
//		String changelogUrl = repositoryUrl + "/changelog/" + version;
//		return RuyiNetworkUtils.fetchStringContent(changelogUrl, null);
//	}

	private String getVersionFromPackage() throws Exception {
		// 从安装包中提取版本信息
		Path versionFile = Paths.get(installPath+ "/ruyi","VERSION");
		if (Files.exists(versionFile)) {
			return RuyiFileUtils.readFileContent(versionFile.toString()).trim();
		}
		return "unknown";
	}

	// ========== 配置管理方法 ==========

	public String getDefaultInstallPath() {
		return DEFAULT_INSTALL_PATH.toString();
	}

	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}

	public String getInstallPath() {
		return installPath;
	}

	public void setRepoUrls(RepoConfig[] selectedRepoConfigs) {
		this.repoUrls = selectedRepoConfigs;
	}

//	public String getRepositoryUrl() {
//		return repositoryUrl;
//	}
//    public void setRepositoryUrl(String repositoryType) {
//        switch (repositoryType.toLowerCase()) {
//            case "mirror":
//                this.repositoryUrl = MIRROR_REPO_URL;
//                break;
//            case "custom":
//                // 自定义仓库URL应从首选项获取
//                this.repositoryUrl = Activator.getDefault().getPreferenceStore()
//                    .getString(RuyiPreferenceConstants.P_CUSTOM_REPOSITORY);
//                break;
//            default:
//                this.repositoryUrl = OFFICIAL_REPO_URL;
//        }
//    }

	public void setTelemetryStatus(TelemetryStatus status) {
		this.telemetryStatus = status;
	}

	public TelemetryStatus getTelemetryStatus() {
		return telemetryStatus;
	}

	// ========== 辅助方法 ==========

	public void cleanup() throws Exception {
		logger.logInfo("Cleaning up installation files");
		Path installDir = Paths.get(installPath);
		if (Files.exists(installDir)) {
			RuyiFileUtils.deleteRecursively(installDir);
		}
	}
}