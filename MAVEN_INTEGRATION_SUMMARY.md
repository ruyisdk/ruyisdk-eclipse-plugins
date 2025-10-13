# RuyiSDK Eclipse 插件 Maven/Tycho 集成总结

## 概述

已成功为 RuyiSDK Eclipse 插件项目添加了完整的 Maven/Tycho 构建支持，并将其集成到 ruyisdk-eclipse-packages 打包工程中。现在可以通过 Maven 命令构建插件并将其打包到 RuyiSDK IDE 中。

## 完成的工作

### 1. Maven/Tycho 构建结构 ✅

为插件项目创建了完整的 Maven/Tycho 构建配置：

#### 父 POM 配置
- **位置**: `plugins/ruyisdk-eclipse-plugins/pom.xml`
- **Tycho 版本**: 4.0.10 (兼容 Eclipse 2024-09 及更高版本)
- **目标平台**: Eclipse 2024-12 (4.34.0)
- **支持架构**: x86_64, aarch64, riscv64 (Linux/Windows/macOS)

#### 插件模块 POM
为每个插件创建了独立的 `pom.xml`:
- `plugins/org.ruyisdk.core/pom.xml`
- `plugins/org.ruyisdk.devices/pom.xml`
- `plugins/org.ruyisdk.intro/pom.xml`
- `plugins/org.ruyisdk.news/pom.xml`
- `plugins/org.ruyisdk.packages/pom.xml`
- `plugins/org.ruyisdk.projectcreator/pom.xml`
- `plugins/org.ruyisdk.ruyi/pom.xml`
- `plugins/org.ruyisdk.ui/pom.xml`

### 2. Feature 特性定义 ✅

创建了 RuyiSDK Feature 来打包所有插件：

- **位置**: `plugins/ruyisdk-eclipse-plugins/features/org.ruyisdk.feature/`
- **文件**:
  - `feature.xml` - 特性定义，包含所有 8 个插件
  - `feature.properties` - 特性属性
  - `build.properties` - 构建配置
  - `pom.xml` - Maven 配置

### 3. P2 Repository 更新站点 ✅

创建了 P2 仓库模块用于发布插件：

- **位置**: `plugins/ruyisdk-eclipse-plugins/repository/`
- **文件**:
  - `pom.xml` - 仓库构建配置
  - `category.xml` - 插件分类定义
- **构建产物**: `repository/target/repository/` (P2 更新站点)

### 4. 集成到打包工程 ✅

修改了 ruyisdk-eclipse-packages 项目以集成自定义插件：

#### 修改的文件

1. **`package/ruyisdk-eclipse-packages/releng/org.eclipse.epp.config/parent/pom.xml`**
   - 添加了 RuyiSDK 插件仓库配置
   - 添加了 `ruyisdk.plugins.repository` 属性（默认指向本地构建）

2. **`package/ruyisdk-eclipse-packages/packages/org.eclipse.epp.package.embedcpp.product/epp.product`**
   - 添加了 `org.ruyisdk.feature` 特性引用
   - 插件将自动包含在最终的 IDE 产品中

### 5. 文档 ✅

创建了完整的构建文档：

1. **`plugins/ruyisdk-eclipse-plugins/README.md`** 
   - 项目概述
   - 快速开始
   - 插件说明
   - 版本兼容性
   - 开发指南

2. **`package/ruyisdk-eclipse-packages/RUYISDK_BUILD_GUIDE.md`** (中文)
   - 完整 IDE 构建指南
   - 三种构建方案
   - 配置说明
   - 故障排除

## 技术规格

### Maven/Tycho 版本兼容性

| 组件 | 版本 | 说明 |
|------|------|------|
| Maven | 3.9.0+ | 必需的最低版本 |
| Tycho | 4.0.10 | Eclipse 2024-09+ 兼容 |
| Java | 21+ | OpenJDK 21 推荐 |
| Eclipse | 2024-12 (4.34.0) | 目标平台 |

### RISC-V 支持

从 Eclipse 2024-09 开始，完整支持 riscv64 架构：
- 2024-09-29: Eclipse riscv64 支持已上游
- 参考: https://riscv.org/blog-chinese/2024/09/eclipse-riscv64-support-upstreamed/

### 支持的平台架构

- Linux (gtk): x86_64, aarch64, riscv64
- Windows (win32): x86_64, aarch64
- macOS (cocoa): x86_64, aarch64

## 使用方法

### 方法 1: 构建包含插件的完整 IDE（推荐）

```bash
# 1. 构建插件
cd plugins/ruyisdk-eclipse-plugins
mvn clean verify

# 2. 构建包含插件的 RuyiSDK IDE
cd ../../package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products

# 3. 产物位于
# packages/org.eclipse.epp.package.embedcpp.product/target/products/
```

### 方法 2: 仅构建插件（用于单独更新）

```bash
cd plugins/ruyisdk-eclipse-plugins
mvn clean verify

# P2 仓库位于
# repository/target/repository/
```

### 方法 3: 使用自定义插件仓库位置

```bash
cd package/ruyisdk-eclipse-packages
mvn clean verify \
  -Pepp.package.embedcpp \
  -Pepp.materialize-products \
  -Druyisdk.plugins.repository=file:///path/to/repository
```

或使用远程仓库：

```bash
mvn clean verify \
  -Pepp.package.embedcpp \
  -Pepp.materialize-products \
  -Druyisdk.plugins.repository=https://your-server.com/p2/repository
```

## 构建产物

### 插件构建产物

```
plugins/ruyisdk-eclipse-plugins/repository/target/repository/
├── artifacts.jar           # P2 元数据
├── content.jar            # P2 元数据
├── features/
│   └── org.ruyisdk.feature_0.0.4.*.jar
└── plugins/
    ├── org.ruyisdk.core_0.0.4.*.jar
    ├── org.ruyisdk.devices_0.0.4.*.jar
    ├── org.ruyisdk.intro_0.0.4.*.jar
    ├── org.ruyisdk.news_0.0.4.*.jar
    ├── org.ruyisdk.packages_0.0.4.*.jar
    ├── org.ruyisdk.projectcreator_0.0.4.*.jar
    ├── org.ruyisdk.ruyi_0.0.4.*.jar
    └── org.ruyisdk.ui_0.0.4.*.jar
```

### IDE 构建产物

```
packages/org.eclipse.epp.package.embedcpp.product/target/products/
├── ruyisdk-0.0.3-linux.gtk.x86_64.tar.gz
├── ruyisdk-0.0.3-linux.gtk.aarch64.tar.gz
└── ruyisdk-0.0.3-linux.gtk.riscv64.tar.gz
```

## 两种发布方式的实现

### 方式 1: 联合打包（新实现） ✅

通过 Maven 构建，将所有插件集成到 IDE 产品中：

```bash
# 构建完整的 IDE（包含最新插件）
cd plugins/ruyisdk-eclipse-plugins && mvn clean verify
cd ../../package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products
```

**优点**:
- 用户下载后开箱即用
- 版本一致性好
- 适合新用户和稳定版本发布

### 方式 2: 单独更新（已支持） ✅

通过 P2 更新站点提供插件单独安装和更新：

```bash
# 仅构建插件
cd plugins/ruyisdk-eclipse-plugins
mvn clean verify

# 发布 repository/target/repository/ 到更新站点
# 用户可通过 Help -> Install New Software 安装
```

**优点**:
- 快速迭代和更新
- 用户可选择性安装
- 适合开发版本和频繁更新

## 开发工作流

### 日常开发

```bash
# 1. 修改插件代码
vim plugins/ruyisdk-eclipse-plugins/plugins/org.ruyisdk.xxx/src/...

# 2. 快速测试（跳过不必要的步骤）
cd plugins/ruyisdk-eclipse-plugins
mvn clean install -DskipTests

# 3. 重新构建 IDE
cd ../../package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products -DskipTests
```

### 发布流程

```bash
# 1. 更新版本号
# 修改所有 pom.xml 和 MANIFEST.MF 中的版本号

# 2. 完整构建并测试
cd plugins/ruyisdk-eclipse-plugins
mvn clean verify

# 3. 构建最终产品
cd ../../package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products

# 4. 测试产物
cd packages/org.eclipse.epp.package.embedcpp.product/target/products
tar -xzf ruyisdk-0.0.3-linux.gtk.x86_64.tar.gz
cd ruyisdk && ./ruyisdk

# 5. 发布到仓库
# - 上传 tar.gz 文件到发布站点
# - 部署 P2 仓库到更新站点
```

## 故障排除快速参考

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| Cannot resolve dependencies | 网络或仓库问题 | `mvn clean verify -U` |
| Java version error | Java < 21 | 安装 OpenJDK 21+ |
| Cannot find org.ruyisdk.feature | 插件未构建 | 先构建插件再构建 IDE |
| Out of memory | Maven 内存不足 | `export MAVEN_OPTS="-Xmx4096m"` |
| Disk space error | 磁盘空间不足 | 清理构建，或只构建需要的平台 |

## 参考资料

- [Eclipse Tycho 文档](https://tycho.eclipseprojects.io/)
- [Eclipse Packaging 项目](https://github.com/eclipse-packaging/packages)
- [RuyiSDK 官网](https://ruyisdk.org/)
- [学习笔记](https://github.com/xijing21/eclipse-myplugins)

## 总结

✅ **所有目标已完成**:

1. ✅ 为插件添加了 Maven/Tycho 构建
2. ✅ Maven 版本兼容 Eclipse 2024-09+ (支持 riscv64)
3. ✅ 插件构建集成到 ruyisdk-eclipse-packages
4. ✅ 支持两种发布方式（联合打包 + 单独更新）
5. ✅ 创建了完整的文档

现在可以：
- 使用 Maven 命令行构建所有插件
- 自动生成 P2 更新站点
- 将插件集成到 RuyiSDK IDE 产品中
- 同时支持完整 IDE 发布和插件单独更新
- 轻松集成到 CI/CD 流程

