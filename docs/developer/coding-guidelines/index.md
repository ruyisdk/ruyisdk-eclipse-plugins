# 代码风格指南

本目录包含 RuyiSDK 项目中关于代码风格规范的说明与配置文件，旨在统一团队代码风格，提升代码可读性与维护性。

---

## 目录结构

```
developer/
└── coding-guidelines/   # 编码规范
├── index.md # 本文档：代码风格指南总览
├── style-guide.md # 主文档：代码风格指南与配置说明
├── ruyisdk_ide_google_checks.xml # Checkstyle 配置文件
├── ruyisdk-eclipse-java-google-style.xml # Eclipse Formatter 配置文件
└── ruyisdk-eclipse-java-codetemplates.xml # Eclipse Code Templates 配置文件
```

---

## 文件说明

### 📄 `style-guide.md`
- **用途**：详细说明代码风格规范，包括命名规则、缩进、注释、格式化规则等。
- **适用人群**：所有参与代码编写的开发者。
- **内容亮点**：
  - 代码风格最佳实践
  - 与 Google Java Style Guide 的对齐说明
  - 集成方式与 IDE 配置指引

### ⚙️ `ruyisdk_ide_google_checks.xml`
- **用途**：Checkstyle 配置文件，用于静态代码检查。
- **适用工具**：支持 Checkstyle 的 IDE 或构建工具（如 Maven、Gradle）。
- **功能**：
  - 自动检查代码是否符合规范
  - 集成到 CI/CD 流程中实现自动化代码审查

### 🎨 `ruyisdk-eclipse-java-google-style.xml`
- **用途**：Eclipse 代码格式化配置文件。
- **适用工具**：Eclipse IDE。
- **功能**：
  - 一键格式化代码，符合项目风格
  - 与 Google Java Style Guide 对齐
  - 可导入 Eclipse 的 Formatter 配置中直接使用

### 🎨 `ruyisdk-eclipse-java-codetemplates.xml`
- **用途**：Eclipse 代码和注释模板配置文件。
- **适用工具**：Eclipse IDE。
- **功能**：
  - 自动生成标准化的文件头注释（如版权信息、作者）。
  - 为新建的类、接口、枚举、方法等提供统一的注释模板。
  - 自动生成 `catch` 块、构造函数、getter/setter 等常用代码模板。
  - 确保所有新创建的代码元素都包含符合项目规范的必要注释，提升代码文档的一致性和完整性。

---

## 如何使用

### 1. 阅读风格指南
请首先阅读 [`style-guide.md`](./style-guide.md)，了解代码风格规范与最佳实践。

### 2. 配置 IDE
- **Eclipse 用户**：
  - 导入 `ruyisdk-eclipse-java-google-style.xml` 至 Eclipse 的 Formatter 配置。
  - 导入 `ruyisdk_ide_google_checks.xml` 至 Checkstyle 插件。
  - 导入 `ruyisdk-eclipse-java-codetemplates.xml`至 Eclipse 的 Code Templates 配置。

### 3. 集成到 CI/CD
可将 `ruyisdk_ide_google_checks.xml` 配置到项目的构建流程中，在提交或合并代码时自动检查风格合规性。

---

## 扩展说明

- 本项目风格指南主要参考 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)，并结合团队实际需求进行了适当调整。
- 欢迎提交 Issue 或 Pull Request，对风格规范或配置文件提出改进建议。

---

**维护者**：RuyiSDK 开发团队  
**最后更新**：2025-08-11
