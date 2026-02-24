# RuyiSDK Eclipse Plugins

## Summary (简介)

**RuyiSDK Eclipse Plugins** provide basic RISC-V C/C++ development support for Eclipse IDE on Linux. These plugins leverage the [Ruyi package manager](https://github.com/ruyisdk/ruyi) to streamline toolchain installation and updates, and offer intuitive UI for managing RISC-V virtual environment.

Don't miss our VS Code extension: https://github.com/ruyisdk/ruyisdk-vscode-extension

**RuyiSDK Eclipse 插件**为 Linux 上的 Eclipse IDE 提供了基础的 RISC-V C/C++ 开发支持。本插件借助 [Ruyi 包管理器](https://github.com/ruyisdk/ruyi)简化了工具链的安装与更新，提供了直观的用户界面以管理 RISC-V 虚拟环境。

别错过我们的 VS Code 扩展: https://github.com/ruyisdk/ruyisdk-vscode-extension

## Plugin List (插件列表)

| Plugin | Description | 描述 |
| - | - | - |
| org.ruyisdk.core | Core library | 基础类库 |
| org.ruyisdk.devices | RISC-V device management and configuration | RISC-V 设备管理和配置 |
| org.ruyisdk.intro | Customized welcome page | 定制化欢迎界面 |
| org.ruyisdk.news | News module | 新闻模块 |
| org.ruyisdk.packages | Package resource manager | 包资源管理器 |
| org.ruyisdk.projectcreator | Project creation wizard | 项目创建向导 |
| org.ruyisdk.ruyi | Ruyi program integration | Ruyi 程序集成 |
| org.ruyisdk.ui | User interface module | 用户界面模块 |
| org.ruyisdk.venv | Virtual environment management module | 虚拟环境管理模块 |

## How to Install (如何安装)

### Online (在线安装)

<!-- based on: https://marketplace.eclipse.org/content/ruyisdk#external-install-button -->
<a href="https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=7323110" class="drag" title="Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client">
<img style="width:80px;" typeof="foaf:Image" class="img-responsive" src="https://marketplace.eclipse.org/modules/custom/eclipsefdn/eclipsefdn_marketplace/images/btn-install.svg" alt="Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client" />
</a>

<br>

1. Launch the IDE, open the "Help" menu at the top menu bar, and click "Eclipse Marketplace...".
1. Enter "ruyisdk" in the search box and click the "Go" button to find this plugin.
1. After confirming that the name, version, and author are correct, click the "Install" button to begin installation.

<br>

1. 启动 IDE，打开顶部菜单栏的 "Help" ，单击 "Install Marketplace..."。
1. 在搜索框中输入 "ruyisdk" 并单击 "Go" 按钮找到本插件。
1. 确认名称、版本和作者无误后，单击 "Install" 按钮开始安装。

### Offline (离线安装)

1. Download `ruyisdk-eclipse-plugins-X.Y.Z.zip` from the [Releases](https://github.com/ruyisdk/ruyisdk-eclipse-plugins/releases/latest) page. No need to extract it.
1. Launch the IDE, open the "Help" menu at the top menu bar, and click "Install New Software...".
1. In the opened window, click the "Add..." button to open the "Add Repository" dialog.
1. Click the "Archive..." button and locate the downloaded zip file.
1. Go back to the "Install" window, and choose the newly added site from the "Work with" dropdown box.
1. After the information is loaded, expand "RuyiSDK IDE" and check "RuyiSDK IDE Feature", then click the "Next >" button to start the installation.

<br>

1. 从 [Releases](https://github.com/ruyisdk/ruyisdk-eclipse-plugins/releases/latest) 页面下载 `ruyisdk-eclipse-plugins-X.Y.Z.zip`。不需要解压缩。
1. 启动 IDE，打开顶部菜单栏的 "Help" ，单击 "Install New Software..."。
1. 在打开的窗口中单击 "Add..." 按钮打开 "Add Repository" 对话框。
1. 单击 "Archive..." 按钮，在打开的窗口中找到刚才下载的 zip 文件。
1. 回到 "Install" 窗口，在 "Work with" 下拉框中选择刚添加的站点。
1. 待信息载入完毕后，展开 "RuyiSDK IDE" 并勾选 "RuyiSDK IDE Feature" 后单击 "Next >" 按钮开始安装。

## Development & Contribution (开发与贡献)

Contributions are welcome! Please refer to [CONTRIBUTING.md](docs/developer/CONTRIBUTING.md) for details.

欢迎贡献！请参阅 [CONTRIBUTING.md](docs/developer/CONTRIBUTING.md) 了解详细信息。

## See Also (另请参阅)

RuyiSDK IDE: https://github.com/ruyisdk/ruyisdk-eclipse-packages/

本项目与实习培养计划相关，请参考 [甲辰计划实习生岗位](https://github.com/plctlab/weloveinterns/blob/master/open-internships.md)并在文中搜索 "J159 RuyiSDK IDE 开发实习生"。

## License (许可证)

Copyright (C) Institute of Software, Chinese Academy of Sciences (ISCAS). All rights reserved.

This project is licensed under the EPL-2.0 License. See [LICENSE](LICENSE) for details.

版权所有 (C) 中国科学院软件研究所（ISCAS）。保留所有权利。

本项目采用 Eclipse Public License v2.0 许可。详见 [LICENSE](LICENSE) 文件。

