# RuyiSDK IDE 构建指南

本文档说明如何构建包含 RuyiSDK 自定义插件的 Eclipse IDE。

## 前置要求

### 必需软件

- **Java 17+**: OpenJDK 17 或更高版本
- **Maven 3.9.0+**: Apache Maven 3.9.0 或更高版本

### 环境设置

```bash
# 安装 OpenJDK 21 (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-21-jdk

# 设置环境变量
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export MAVEN_HOME=/opt/apache-maven-3.9.9
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH

# 为 Maven 设置内存
export MAVEN_OPTS="-Xmx2048m"

# 验证版本
java -version   # 应显示 21.x.x
mvn -version    # 应显示 3.9.x
```

## 构建步骤

### 方案一：构建包含插件的完整 IDE

这是推荐的方式，将自定义插件集成到 RuyiSDK IDE 中。

#### 1. 构建插件

```bash
cd plugins/ruyisdk-eclipse-plugins
mvn clean verify
```

构建产物位于: `repository/target/repository/`

#### 2. 构建 IDE 包

```bash
cd ../../package/ruyisdk-eclipse-packages

# 构建嵌入式 C/C++ 开发 IDE (包含 RuyiSDK 插件)
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products
```

构建产物位于:
```
packages/org.eclipse.epp.package.embedcpp.product/target/products/
├── ruyisdk-0.0.3-linux.gtk.x86_64.tar.gz
├── ruyisdk-0.0.3-linux.gtk.aarch64.tar.gz
└── ruyisdk-0.0.3-linux.gtk.riscv64.tar.gz
```

#### 3. 测试构建的 IDE

```bash
cd packages/org.eclipse.epp.package.embedcpp.product/target/products
tar -xzf ruyisdk-0.0.3-linux.gtk.x86_64.tar.gz
cd ruyisdk
./ruyisdk
```

### 方案二：仅构建打包工程（不含自定义插件）

如果只需要基础的 Eclipse 打包：

```bash
cd package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products
```

### 方案三：使用远程插件仓库

如果插件已经发布到远程 P2 仓库：

```bash
cd package/ruyisdk-eclipse-packages
mvn clean verify \
  -Pepp.package.embedcpp \
  -Pepp.materialize-products \
  -Druyisdk.plugins.repository=https://your-server.com/p2/repository
```

## 构建配置

### 支持的平台和架构

默认配置构建以下平台：

- Linux (gtk)
  - x86_64
  - aarch64  
  - riscv64 ✓ (RISC-V 64位)

要启用其他平台，编辑 `releng/org.eclipse.epp.config/parent/pom.xml`。

### RuyiSDK 插件集成

插件通过以下机制集成：

1. **插件仓库配置** (`releng/org.eclipse.epp.config/parent/pom.xml`):
   ```xml
   <ruyisdk.plugins.repository>
     file://${project.basedir}/../../../plugins/ruyisdk-eclipse-plugins/repository/target/repository
   </ruyisdk.plugins.repository>
   ```

2. **特性包含** (`packages/org.eclipse.epp.package.embedcpp.product/epp.product`):
   ```xml
   <feature id="org.ruyisdk.feature" installMode="root"/>
   ```

### 自定义插件仓库位置

如果插件位于不同位置，可以通过命令行参数覆盖：

```bash
mvn clean verify \
  -Druyisdk.plugins.repository=file:///absolute/path/to/repository
```

## 开发工作流

### 修改插件后重新构建

```bash
# 1. 修改插件代码
cd plugins/ruyisdk-eclipse-plugins/plugins/org.ruyisdk.xxx
# ... 进行修改 ...

# 2. 重新构建插件
cd ../..
mvn clean verify

# 3. 重新构建 IDE
cd ../../package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products
```

### 增量构建（跳过测试）

```bash
# 插件
cd plugins/ruyisdk-eclipse-plugins
mvn clean install -DskipTests

# IDE
cd ../../package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products -DskipTests
```

## 构建特定包

### 仅构建特定架构

修改 `releng/org.eclipse.epp.config/parent/pom.xml` 中的 `environments` 部分，
注释掉不需要的平台。

例如，仅构建 x86_64:

```xml
<environments>
  <environment>
    <os>linux</os>
    <ws>gtk</ws>
    <arch>x86_64</arch>
  </environment>
  <!-- 注释掉其他平台 -->
</environments>
```

### 构建其他 IDE 包

可用的包配置文件：

- `-Pepp.package.cpp` - C/C++ 开发 IDE
- `-Pepp.package.embedcpp` - 嵌入式 C/C++ 开发 IDE (推荐用于 RuyiSDK)
- `-Pepp.package.java` - Java 开发 IDE
- `-Pepp.package.jee` - Java EE 开发 IDE

## 故障排除

### 问题：构建失败 "Cannot resolve project dependencies"

**原因**: Maven 无法访问 Eclipse 仓库

**解决**:
1. 检查网络连接
2. 验证 SimRel 仓库 URL
3. 使用 `-U` 强制更新: `mvn clean verify -U`

### 问题：磁盘空间不足

**原因**: 构建需要大量磁盘空间

**解决**:
1. 清理之前的构建: `mvn clean`
2. 仅构建需要的平台
3. 释放磁盘空间

### 问题：内存不足

**原因**: Maven 运行内存不足

**解决**:
```bash
export MAVEN_OPTS="-Xmx4096m -XX:MaxPermSize=1024m"
mvn clean verify
```

### 问题：找不到 org.ruyisdk.feature

**原因**: 插件未先构建

**解决**:
```bash
# 确保先构建插件
cd plugins/ruyisdk-eclipse-plugins
mvn clean install

# 然后构建 IDE
cd ../../package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products
```

### 问题：Java 版本不兼容

**原因**: 使用了 Java 17 以下版本

**解决**:
```bash
# 安装 Java 21
sudo apt install openjdk-21-jdk

# 更新环境变量
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 验证
java -version
```

## 技术细节

### Maven Tycho 版本

- **Tycho 4.0.10**: 与 Eclipse 2024-09 (4.33) 及更高版本兼容
- 完整支持 riscv64 架构

### Eclipse 版本

- **目标平台**: Eclipse 2024-12 (4.34.0)
- **SimRel 仓库**: https://download.eclipse.org/releases/2024-12/

### RuyiSDK 插件

包含的插件：
- `org.ruyisdk.core` - 核心功能
- `org.ruyisdk.devices` - RISC-V 设备管理
- `org.ruyisdk.intro` - 欢迎界面
- `org.ruyisdk.news` - 新闻和更新--规划中
- `org.ruyisdk.packages` - 包管理器
- `org.ruyisdk.projectcreator` - 项目创建
- `org.ruyisdk.ruyi` - Ruyi 集成
- `org.ruyisdk.ui` - UI 组件--规划中


## 相关文档

- [插件构建指南](../../plugins/ruyisdk-eclipse-plugins/README.md)
- [Eclipse Packaging 文档](https://github.com/eclipse-packaging/packages)
- [学习笔记](https://github.com/xijing21/eclipse-myplugins)

## 支持

- 提交问题: https://github.com/ruyisdk/ruyisdk-eclipse-packages/issues

## 许可证

Copyright (c) 2024 RuyiSDK and others.

本项目采用 Eclipse Public License v2.0 许可。
详见 [LICENSE](LICENSE) 文件。

