# custom-plugins

## 目录结构说明

```
├── features
├── plugins
│   ├── org.ruyisdk.core
│   ├── org.ruyisdk.devices
│   ├── org.ruyisdk.news
│   ├── org.ruyisdk.packages
|   ├── org.ruyisdk.projectcreator 
│   ├── org.ruyisdk.intro
│   ├── org.ruyisdk.ruyi
│   └── org.ruyisdk.ui
└── README.md

```

## 插件说明

* **org.ruyisdk.core**: 基础类库，提供其它插件调用的公共功能
* **org.ruyisdk.devices**: RISC-V 设备管理和配置
* **org.ruyisdk.packages**: 包资源管理器，管理 SDK 包和依赖
* **org.ruyisdk.intro**: 定制化欢迎界面
* **org.ruyisdk.projectcreator**: 项目创建向导
* **org.ruyisdk.ruyi**: Ruyi 包管理器集成，自动安装和更新工具链
* **org.ruyisdk.news**: 新闻与动态模块
* **org.ruyisdk.ui**: 用户界面模块

## 快速开始

### 在线安装（使用 RuyiSDK IDE）

1. 打开 RuyiSDK IDE
2. `Help` → `Install New Software...`
3. 从下拉列表中选择：`RuyiSDK Updates (GitHub) - https://ruyisdk.github.io/ruyisdk-eclipse-plugins/`
4. 选择 `RuyiSDK IDE` 并安装

### 离线安装（使用 zip 包）

从 [Releases](https://github.com/ruyisdk/ruyisdk-eclipse-plugins/releases) 下载 `ruyisdk-eclipse-plugins.site.zip`，然后：

1. 打开 Eclipse IDE
2. `Help` → `Install New Software...` → `Add...`
3. 输入：
   - **Name**: `RuyiSDK Plugins (Local)`
   - **Location**: `file:///path/to/ruyisdk-eclipse-plugins.site.zip`
4. 选择 `RuyiSDK IDE` 并安装

### 构建插件

```bash
# 克隆仓库
git clone https://github.com/ruyisdk/ruyisdk-eclipse-plugins.git
cd ruyisdk-eclipse-plugins

# 构建所有插件
mvn clean verify

# 构建结果位于
# sites/repository/target/repository/
# 后续参考离线安装步骤
```

## 贡献

欢迎贡献！请参阅 [CONTRIBUTING.md](docs/developer/CONTRIBUTING.md) 了解详细信息。

编码规范遵循 [Google Java Style Guide](docs/developer/coding-guidelines/style-guide.md)

## 相关仓库

- **RuyiSDK IDE 工程**: https://github.com/ruyisdk/ruyisdk-eclipse-packages/
- **插件仓库**: https://github.com/ruyisdk/ruyisdk-eclipse-plugins/

## 许可证

Copyright (C) Institute of Software, Chinese Academy of Sciences (ISCAS). All rights reserved.

版权所有 © 中国科学院软件研究所（ISCAS）。保留所有权利。

本项目采用 Eclipse Public License v2.0 许可。
详见 [LICENSE](LICENSE) 文件。

This project is licensed under the EPL-2.0 License.
See [LICENSE](LICENSE) for details.

