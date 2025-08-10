# 贡献指南
## 1. 项目简介
本项目是基于 Eclipse 平台开发的 RuyiSDK 插件集合，旨在为 RISC-V 开发者提供更便捷的开发服务。通过集成多种 Eclipse 插件，我们致力于提升 RISC-V 开发环境的易用性与扩展性，助力开发者更高效地进行 RISC-V 软件开发。
欢迎开发者通过 Pull Request 的方式参与贡献，共同完善 RuyiSDK 插件生态。

## 2. 目录介绍
以下是本项目的目录结构及其简要说明：
```
.
├── docs
│   ├── codestyle        # 代码风格规范
│   └── developer        # 开发者文档（如构建、测试、调试指南）
├── plugins              # Eclipse 插件模块
│   ├── org.ruyisdk.core         # 核心功能模块
│   ├── org.ruyisdk.devices      # 设备支持模块
│   ├── org.ruyisdk.intro       # 欢迎页与引导模块
│   ├── org.ruyisdk.news        # 新闻与动态模块
│   ├── org.ruyisdk.packages    # 包管理模块
│   ├── org.ruyisdk.projectcreator # 项目创建向导模块
│   ├── org.ruyisdk.ruyi        # RuyiSDK包管理器检测与安装/更新模块
│   └── org.ruyisdk.ui          # 用户界面模块
├── LICENSE               # EPL-2.0 开源许可证
└── README.md             # 项目简介与快速上手
```
- `docs` 目录下存放文档，分为代码风格与开发者文档两部分；
- `plugins` 目录下为各个 Eclipse 插件模块，每个模块对应 Eclipse 的一个功能或扩展点；(变化中，如未及时更新文档，请以实际代码为准)
- `LICENSE` 本项目采用 [Eclipse Public License-v2.0](https://www.eclipse.org/legal/epl-2.0/) 开源许可证;
- `README.md` 为项目入口文档，包含项目概述、快速入门等内容。
---
## 3. 贡献操作指南
### 3.1 贡献流程
我们欢迎所有形式的贡献，包括但不限于代码提交、文档完善、问题反馈与修复。以下是标准的代码贡献流程：
#### （1）Fork 仓库
访问 [ruyisdk/ruyisdk-eclipse-plugins](https://github.com/ruyisdk/ruyisdk-eclipse-plugins.git)，点击右上角的 **Fork** 按钮，将仓库复制到你的 GitHub 账号下。
#### （2）克隆本地仓库
```bash
git clone https://github.com/<your-username>/ruyisdk-eclipse-plugins.git
cd ruyisdk-eclipse-plugins
```
#### （3）创建新分支
为每个新功能或修复创建独立分支，建议命名格式为 `feature/xxx` 或 `fix/xxx`：
```bash
git checkout -b feature/your-feature-name
```
#### （4）开发与提交
- **DCO 约定**：所有提交需遵守 DCO（Developer Certificate of Origin）协议。请确保您知晓并同意 DCO 条款，提交时使用 `-s` 或 `--signoff` 参数签名提交，例如：  
  ```bash
  git commit -s -m "feat: add new feature"
  ```
  签名即表示您确认该贡献符合 DCO 要求（详见 [DCO 1.1 文本](https://developercertificate.org/)）。
- 开发前请阅读 `docs/developer/coding-guidelines/` 中的代码规范；
- **提交颗粒度建议**：建议以“功能独立、自测通过、信息清晰”为基准，以“自测通过的最小功能单元”为颗粒度参考。初期若无法精确划分，请至少确保每次提交逻辑内聚、不混杂无关修改，便于后续审查、回滚和追溯。例如，一个 bug 修复、一个小功能点或一次代码重构，应独立提交，避免将多个任务合并为一次提交。
- 确保代码能正常编译并通过测试；
- 提交信息应清晰简洁，**建议使用英语**，格式如下：
  参考 [Conventional Commits](https://www.conventionalcommits.org/) 规范,提交信息应使用 **语义化提交格式**，如：
  ```
  <type>(<scope>): <subject>
  <body>
  <footer>
  ```
  其中 `<type>` 包括：
  - `feat`: 新功能
  - `fix`: 修复 bug
  - `docs`: 文档更新
  - `style`: 代码格式调整
  - `refactor`: 代码重构
  - `test`: 测试相关
  - `chore`: 构建或辅助工具变动


    ```plaintext
    类型: 具体改动描述
    例如：
    fix: 修复设备列表加载异常
    feat: 新增项目创建向导支持
    ```
  提交代码：
  ```bash
  git add .
  git commit -s -m "类型: 具体改动描述"
  ```
#### （5）推送分支

```bash
git push origin feature/your-feature-name
```
#### （6）发起 Pull Request
- 在你的 GitHub 仓库页面点击 **Compare & pull request**；
- 填写 PR 描述，说明改动内容、测试情况及关联 issue；
- 提交 PR，等待项目维护者审核。
#### （7）代码审核与合并
- 维护者会对代码进行审核，可能会提出修改建议；
- 所有 PR 必须经过至少一位维护者审查；
- 请确保 CI 检查全部通过(如有)；
- 请及时根据反馈修改代码并更新 PR；
- 审核通过后，代码将被合并到主分支。

---

### 3.2 开发环境搭建
请参考 `docs/developer` 目录下的相关文档，了解如何配置 Eclipse 开发环境、编译插件、运行测试等。

---

## 4. 欢迎参与贡献
RuyiSDK 插件项目目前仍处于初级阶段，发展可能相对缓慢，但我们非常欢迎社区开发者的参与和贡献。
### 如何参与？
- 查看 [Issues 列表](https://github.com/ruyisdk/ruyisdk-eclipse-plugins/issues)，了解当前需要帮助的任务；
- 提交新的 [issue](https://github.com/ruyisdk/ruyisdk-eclipse-plugins/issues) 报告问题或提出建议；
- 直接提交 Pull Request 贡献代码或文档。
### 实习生招聘
我们正在招募插件开发实习生，如果你对 Eclipse 插件开发、RISC-V 工具链或 IDE 开发感兴趣，欢迎加入我们！
- **J159 RuyiSDK IDE 开发实习生**  
  招募详情请见：[PLCT的开放实习岗位](https://github.com/plctlab/weloveinterns/blob/master/open-internships.md#j159-ruyisdk-ide-%E5%BC%80%E5%8F%91%E5%AE%9E%E4%B9%A0%E7%94%9F20250325-%E5%BC%80%E6%94%BE-5-%E5%90%8D)
期待你的加入，一起为 RISC-V 开发者打造更强大的开发工具！

