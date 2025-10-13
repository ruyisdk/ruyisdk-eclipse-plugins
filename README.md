# RuyiSDK Eclipse Plugins

RuyiSDK Eclipse IDE 的自定义插件集合，专为 RISC-V 开发环境设计。

## 项目结构

```
├── features/                  # Eclipse 特性定义
│   └── org.ruyisdk.feature/  # RuyiSDK 主特性
├── plugins/                   # 插件模块
│   ├── org.ruyisdk.core/            # 核心功能插件
│   ├── org.ruyisdk.devices/         # RISC-V 设备管理
│   ├── org.ruyisdk.intro/           # 欢迎界面定制
│   ├── org.ruyisdk.news/            # 新闻和更新
│   ├── org.ruyisdk.packages/        # 包资源管理器
│   ├── org.ruyisdk.projectcreator/  # 项目创建向导
│   ├── org.ruyisdk.ruyi/            # Ruyi 包管理器集成
│   └── org.ruyisdk.ui/              # UI 组件
├── repository/                # P2 更新站点
├── pom.xml                    # Maven 父项目配置
├── BUILD.md                   # 详细构建指南
└── README.md                  # 本文件
```

## 插件说明

* **org.ruyisdk.core**: 基础类库，提供其它插件调用的公共功能
* **org.ruyisdk.devices**: RISC-V 设备管理和配置
* **org.ruyisdk.packages**: 包资源管理器，管理 SDK 包和依赖
* **org.ruyisdk.intro**: 定制化欢迎界面
* **org.ruyisdk.projectcreator**: 项目创建向导，支持多种 RISC-V 开发板
* **org.ruyisdk.ruyi**: Ruyi 包管理器集成，自动安装和更新工具链
* **org.ruyisdk.news**: 显示 RuyiSDK 新闻和更新
* **org.ruyisdk.ui**: 共享 UI 组件和样式

## 快速开始

### 构建要求

- Java 21 或更高版本
- Apache Maven 3.9.0 或更高版本
- 足够的磁盘空间 (建议 20GB+)

### 构建插件

```bash
# 克隆仓库
git clone https://github.com/ruyisdk/ruyisdk-eclipse-plugins.git
cd ruyisdk-eclipse-plugins

# 构建所有插件
mvn clean verify

# 构建结果位于
# repository/target/repository/
```

### 集成到 IDE 打包

```bash
# 1. 先构建插件（如上所示）

# 2. 构建包含插件的 RuyiSDK IDE
cd ../ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products
```

详细的构建说明和故障排除，请参阅 [BUILD.md](BUILD.md)。

## 版本兼容性

| RuyiSDK Plugins | Eclipse 版本 | Tycho 版本 | 支持架构 |
|----------------|-------------|-----------|---------|
| 0.0.4          | 2024-12     | 4.0.10    | x86_64, aarch64, riscv64 |
| 0.0.3          | 2024-09     | 4.0.8     | x86_64, aarch64, riscv64 |

## 开发

### 导入到 Eclipse IDE

1. 打开 Eclipse IDE (建议使用 Eclipse for RCP and RAP Developers)
2. File → Import → Maven → Existing Maven Projects
3. 选择 `ruyisdk-eclipse-plugins` 目录
4. 选择所有项目并点击 Finish

### 运行和调试

1. 右键点击插件项目
2. Run As → Eclipse Application
3. 将启动一个包含您的插件的新 Eclipse 实例

## 贡献

欢迎贡献！请参阅 [CONTRIBUTING.md](docs/developer/CONTRIBUTING.md) 了解详细信息。

### 编码规范

- 遵循 [Google Java Style Guide](docs/developer/coding-guidelines/style-guide.md)
- 使用提供的代码模板和格式化配置

## 文档

- [构建指南](BUILD.md) - 详细的构建说明
- [开发者文档](docs/developer/) - 开发和贡献指南
- [学习笔记](https://github.com/xijing21/eclipse-myplugins) - Eclipse 插件开发学习资料

## 相关仓库

- **打包工程**: https://github.com/ruyisdk/ruyisdk-eclipse-packages/
- **插件仓库**: https://github.com/ruyisdk/ruyisdk-eclipse-plugins/
- **学习笔记**: https://github.com/xijing21/eclipse-myplugins

## 许可证

Copyright (c) 2024 RuyiSDK and others.

本项目采用 Eclipse Public License v2.0 许可。
详见 [LICENSE](LICENSE) 文件。

## 支持

- 提交问题: https://github.com/ruyisdk/ruyisdk-eclipse-plugins/issues
- 讨论区: https://github.com/ruyisdk/ruyisdk-eclipse-plugins/discussions

