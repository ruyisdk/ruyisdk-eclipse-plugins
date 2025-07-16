# custom-plugins

## 目录结构说明

```
├── features
├── plugins
│   ├── org.ruyisdk.core
│   ├── org.ruyisdk.devices
│   ├── org.ruyisdk.packages
│   ├── org.ruyisdk.projectcreator
│   ├── org.ruyisdk.intro
│   └── org.ruyisdk.ruyi
└── README.md

```

* org.ruyisdk.core:基础类，供其它插件调用的公共部分
* org.ruyisdk.ruyi:ruyi包管理器工具插件，提供ruyi的安装检测、版本检测；安装和更新向导；配置管理等；
* org.ruyisdk.packages:包资源管理器
* org.ruyisdk.intro:定制化欢迎界面
* org.ruyisdk.projectcreator:[实验性]新建项目向导，预置开发板项目模板和自定义构建器用于一键在ruyi虚拟环境内构建项目
* org.ruyisdk.devices:RISC-V设备管理（将整合废弃）


