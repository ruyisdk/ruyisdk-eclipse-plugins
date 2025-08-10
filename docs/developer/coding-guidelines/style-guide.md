# RuyiSDK 代码风格指南

本文档旨在为项目贡献者提供清晰的代码风格规范，并指导如何在 IDE 中启用代码格式化和静态检查。本规范基于 Google Java Style Guide，并进行了适当的定制化调整。

---

## 🧭 目录

- [1. 代码风格规范概述](#1-代码风格规范概述)
- [2. IDE 配置指南](#2-ide-配置指南)
  - [2.1 导入 Formatter 配置](#21-导入-formatter-配置)
  - [2.2 导入 Checkstyle 配置](#22-导入-checkstyle-配置)
  - [2.3 导入 Code Templates 配置](#23-导入-Code-Templates-配置)
- [3. Google Style 主要规则摘要](#3-google-style-主要规则摘要)
- [4. 定制化调整说明](#4-定制化调整说明)
- [5. 未解决的问题与欢迎贡献](#5-未解决的问题与欢迎贡献)
- [6. 完善与反馈](#6-完善与反馈)

---

## 1. 代码风格规范概述

本项目的代码风格规范以 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) 为基础，结合项目实际需求进行了定制.所有贡献者在提交代码前，应确保代码符合以下要求：

- 代码格式统一
- 命名规范清晰
- 注释简洁有效
- 避免常见代码坏味道

即使在没有 CI/CD 工具辅助的环境下，也建议在本地 IDE 中启用格式化和检查工具，以提升代码质量和协作效率。

---

## 2. IDE 配置指南

### 2.1 导入 Formatter 配置

**适用 IDE**：Eclipse

1. 下载 [`ruyisdk-eclipse-java-google-style.xml`](./ruyisdk-eclipse-java-google-style.xml) 文件。
2. 在 IDE 中导入该文件作为代码格式化模板：
   - **Eclipse**：
     - 进入 `Window` → `Preferences` → `Java` → `Code Style` → `Formatter`。
     - 点击 `Import` 并选择该文件。

### 2.2 导入 Checkstyle 配置

1. 下载 [`ruyisdk_ide_google_checks.xml`](./ruyisdk_ide_google_checks.xml) 文件。
2. 在 IDE 中启用 Checkstyle 检查：
   - **Eclipse**：
     - 安装 `Checkstyle` 插件。
     - 在 `Window` → `Preferences` → `Checkstyle` 中导入配置文件。

### 2.3 导入 Code Templates 配置

**适用 IDE**：Eclipse

1. 下载 [`ruyisdk-eclipse-java-codetemplates.xml`](./ruyisdk-eclipse-java-codetemplates.xml) 文件。
2. 在 IDE 中导入该文件作为代码格式化模板：
   - **Eclipse**：
     - 进入 `Window` → `Preferences` → `Java` → `Code Style` → `Code Templates`。
     - 点击 `Import` 并选择该文件。

---

## 3. RuyiSDK Google Style 主要规则摘要

以下是一些 RuyiSDK Google Java Style 的核心规则摘要，供快速参考：

- 缩进使用 **Space only**，不使用 Tab; 4个空格(默认2个空格,现修改为4个空格)。
- 类名使用 `UpperCamelCase`，方法和变量使用 `lowerCamelCase`。
- 常量使用 `UPPER_SNAKE_CASE`。
- 每行最多 **120 字符**(Google 默认100个字符,现修改为120字符)。
- 左大括号不换行。
- 注释使用 `//` 或 `/** ... */` 格式，避免使用 `/* ... */`（除非特殊场景）。
- 空行用于分隔逻辑块，避免连续空行。
- import导入语句应分为以下三个明确的组，每组之间用空行分隔,导入语句的顺序和规则如下:
    - 标准库导入：来自 `java.` 和 `javax.` 包的导入语句;
    - 第三方库导入：非标准库的第三方包导入语句,如 `eclipse.` 包的导入语句;
    - 本地项目导入：当前项目中的包导入语句。

其它未指出的完整规则请参考 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)。

---

## 4. 定制化调整说明
- 对 Google Java Style 调整如下:

|调整项|Google原始规则|RuyiSDK调整后规则|说明|
|------|--------------|------------------|----|
|Indentationsize|2|4|行缩进空格数|
|Tabsize|2|4|TAB空格数|
|Maximumlinewidth|100|120|最大行宽|
|Defaultindentationforwrappedlines|2|4|折行部分的默认缩进|
|Defaultindentationforarrayinitializers|2|4|数组初始化器的默认缩进|
|(import分组空格)|每组之间用空行分隔|不用空行分隔|import分组空格|


- 对 google_checks 调整如下:

|module|property|previous value|modified value|note|
|---|---|---|---|---|
|LineLength|max|100|120|单行长度|
|Indentation|basicOffset|2|4|基本缩进量，通常用于控制整个文件的缩进级别|
||braceAdjustment|2|0|大括号调整，用于控制大括号相对于其所在行的位置|
||caseIndent|2|4|case语句的缩进量|
||throwsIndent|4|4|throws子句的缩进量|
||lineWrappingIndentation|4|4|行包裹时的缩进量|
||arrayInitIndent|2|4|数组初始化时的缩进量|
|AbbreviationAsWordInName|allowedAbbreviations||I|连续大写字母|
|Javadoc|RequireEmptyLineBeforeBlockTagGroup|inherit|ignore|Javadoc标签前空行|
|MissingJavadocType|scope|protected|public|Javadoc注释检查范围|
||skipAnnotations|Generated|Generated,Override|Javadoc注释检查跳过|

- 对 Code Templates 调整如下:
- `First sentence of Javadoc is missing an ending period.`
- `Javadoc tag should be preceded with an empty line.`

---

## 5. 未解决的问题与欢迎贡献

当前 Checkstyle XML 配置文件尚未覆盖以下规则，欢迎贡献者提交补丁或建议：

- Formatter格式化的Javadoc注释的标签(如@param, @return等)是有一个“视觉”空行的，实际包含一个空格，但是Checkstyle会识别为非空行。计划修改Checkstyle来避免误报，但是暂未成功，因此临时先禁用 `RequireEmptyLineBeforeBlockTagGroup` 以避免大面积提示。
- 当前Checkstyle似乎不支持对"import分组空格"的检查，也就是无论是否有空格都不会检查出来。

如果你有相关经验或建议，欢迎提交 Issue 或 Pull Request 帮助完善配置文件。

---

## 6. 完善与反馈

本代码风格指南和配置文件仍在持续完善中.如果你发现以下问题，欢迎贡献：

- 格式化配置未生效
- Checkstyle 规则误报
- 新增规则建议
- 文档描述不清晰

请通过 GitHub Issues 或 Pull Request 与我们联系。

---

> 💡 提示：在提交代码前，请务必在本地 IDE 中启用格式化和检查工具，确保代码符合规范。即使没有 CI 辅助，良好的本地习惯也能显著提升协作效率。
