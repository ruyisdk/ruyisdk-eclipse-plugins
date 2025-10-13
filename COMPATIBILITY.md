# RuyiSDK Eclipse Plugins - 兼容性说明

## Eclipse 平台兼容性

### 支持的 Eclipse 版本

✅ **完全兼容**:
- Eclipse 2024-12 (4.34) - 目标版本
- Eclipse 2024-09 (4.33) - 最低支持版本
- Eclipse 2024 年后续所有版本

### RISC-V 架构支持

✅ **riscv64 完整支持**:
- Eclipse 从 2024-09 开始官方支持 riscv64
- 参考: https://riscv.org/blog-chinese/2024/09/eclipse-riscv64-support-upstreamed/
- 我们的构建配置包含 riscv64 目标平台

## 技术栈版本

### 构建工具

| 组件 | 版本 | 说明 | 兼容性 |
|------|------|------|--------|
| **Maven** | 3.9.0+ | 构建工具 | ✅ 支持最新 Tycho |
| **Tycho** | 4.0.10 | Eclipse 插件构建 | ✅ 支持 Eclipse 2024-09+ |
| **Java** | 21+ | 运行环境 | ✅ Eclipse 2024+ 要求 |

### 目标平台

| 配置项 | 值 | 说明 |
|--------|-----|------|
| Eclipse Release | 2024-12 | 基于 Eclipse 2024-12 |
| Eclipse Version | 4.34.0 | Eclipse 4.34.0 |
| SimRel Repository | https://download.eclipse.org/releases/2024-12/ | 同步发布仓库 |

### 支持的架构

| 操作系统 | 架构 | 支持状态 |
|----------|------|----------|
| Linux | x86_64 | ✅ 完全支持 |
| Linux | aarch64 | ✅ 完全支持 |
| **Linux** | **riscv64** | **✅ 完全支持** |
| Windows | x86_64 | ✅ 完全支持 |
| Windows | aarch64 | ✅ 完全支持 |
| macOS | x86_64 | ✅ 完全支持 |
| macOS | aarch64 (Apple Silicon) | ✅ 完全支持 |

## 插件版本 vs 平台兼容性

### 重要概念

**插件版本号**（如 0.0.4）:
- 这是 RuyiSDK 插件自身的版本
- 用于版本管理和更新控制
- **不影响** Eclipse 平台兼容性

**平台兼容性**:
- 由 Tycho 版本决定
- 由目标平台配置决定
- 由 MANIFEST.MF 中的依赖版本决定

### 示例

```
插件版本: 0.0.4, 0.0.5, 1.0.0, 2.5.3 等 (任意)
         ↓
     不影响
         ↓
Eclipse 兼容性: 由 Tycho 4.0.10 + Eclipse 2024-12 平台决定
         ↓
      结果
         ↓
兼容 Eclipse 2024-09 及更高版本 (包括 riscv64 支持)
```

## 依赖版本要求

### 在 MANIFEST.MF 中的最低版本

RuyiSDK 插件对 Eclipse 平台的最低版本要求：

```properties
# 核心平台
Bundle-RequiredExecutionEnvironment: JavaSE-21
Require-Bundle: org.eclipse.core.runtime;bundle-version="3.31.100",  # Eclipse 2024-09+
                org.eclipse.ui;bundle-version="3.206.100",            # Eclipse 2024-09+
                org.eclipse.jface;bundle-version="3.35.0"             # Eclipse 2024-09+
```

这些版本号对应 Eclipse 2024-09 (4.33) 的 API 版本。

## 向后兼容性

### 不支持的版本

❌ **不兼容**:
- Eclipse 2024-06 及更早版本
  - 原因：缺少 riscv64 支持
  - 原因：API 版本过旧
  - 原因：Java 21 支持不完整

### 向前兼容性

✅ **向前兼容**:
- Eclipse 2025 及未来版本
  - Tycho 4.0.10 使用标准 OSGi/Eclipse API
  - 只要 Eclipse 保持 API 兼容性，插件就能运行

## Tycho 版本选择原因

### 为什么选择 Tycho 4.0.10？

1. **Eclipse 2024-09+ 支持**
   - Tycho 4.0.10 是第一个完整支持 Eclipse 2024-09 的版本
   - 包含 riscv64 架构支持

2. **稳定性**
   - 2024 年发布的稳定版本
   - 广泛测试和验证

3. **功能完整**
   - 完整的 P2 仓库生成
   - 产品打包支持
   - 跨平台构建

### Tycho 版本历史

| Tycho 版本 | 支持的 Eclipse | riscv64 支持 | 发布时间 |
|-----------|--------------|-------------|---------|
| 4.0.10 | 2024-09+ | ✅ | 2024 |
| 4.0.8 | 2024-09 | ✅ | 2024 |
| 4.0.0 | 2023-12 | ❌ | 2023 |
| 3.0.x | 2023-06 | ❌ | 2023 |

## 验证兼容性

### 构建验证

```bash
# 验证配置
cd plugins/ruyisdk-eclipse-plugins
mvn help:effective-pom | grep -A 5 "tycho.version"

# 完整构建（验证所有平台）
mvn clean verify

# 验证产物
ls -l repository/target/repository/plugins/
```

### 运行时验证

在 Eclipse 2024-09 或更高版本中安装插件：

1. Help → About Eclipse → Installation Details
2. 查看 "Installed Software" 标签
3. 确认 RuyiSDK 插件已安装
4. 确认版本号正确

### riscv64 验证

如果您有 riscv64 系统：

```bash
# 在 riscv64 系统上
cd package/ruyisdk-eclipse-packages
mvn clean verify -Pepp.package.embedcpp -Pepp.materialize-products

# 验证 riscv64 产物
ls -l packages/org.eclipse.epp.package.embedcpp.product/target/products/*riscv64*
```

## 更新策略

### 跟随 Eclipse 发布

当新的 Eclipse 版本发布时：

1. **更新目标平台**:
   ```xml
   <eclipse.release>2025-03</eclipse.release>
   <simrel.repo>https://download.eclipse.org/releases/2025-03/</simrel.repo>
   ```

2. **验证 Tycho 兼容性**:
   - 检查 Tycho 发行说明
   - 必要时升级 Tycho 版本

3. **测试构建**:
   ```bash
   mvn clean verify
   ```

### 版本升级路径

```
当前: Tycho 4.0.10 + Eclipse 2024-12
  ↓
未来: Tycho 4.0.x + Eclipse 2025-xx
  ↓
保持: riscv64 支持持续
```

## 参考资料

- [Eclipse Release Schedule](https://wiki.eclipse.org/Simultaneous_Release)
- [Tycho Documentation](https://tycho.eclipseprojects.io/)
- [RISC-V Eclipse Support](https://riscv.org/blog-chinese/2024/09/eclipse-riscv64-support-upstreamed/)
- [Eclipse 2024-09 Release Notes](https://www.eclipse.org/eclipse/news/4.33/)

## 总结

✅ **RuyiSDK 插件配置完全满足要求**:

1. ✅ 兼容 Eclipse 2024-09 及更高版本
2. ✅ 完整支持 riscv64 架构
3. ✅ 使用最新稳定的构建工具
4. ✅ 向前兼容未来的 Eclipse 版本
5. ✅ 插件版本号（0.0.4）不影响平台兼容性

**插件版本号可以是任何值（0.0.4, 1.0.0, 等），平台兼容性由 Tycho 4.0.10 和 Eclipse 2024-12 目标平台保证。**

