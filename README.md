# SPW Lyrics Island Studio

为 `SPW-Lyrics-Island` 提供可独立测试、可独立版本化的桌面歌词增强模块。

本仓库只包含 **GPL-3.0 的清洁实现**，不复制 AGPL 项目的源码；行为参考自公开的插件功能说明，界面与数据结构按本项目的模块边界重新设计。

## 模块

| 包 | 内容 |
| --- | --- |
| `preset` | 预设导入 / 导出、差异预览、软删除、重命名与内置预设保护 |
| `font` | 宿主内置字体发现与加载，不注册全局字体表 |
| `color` | RGB / HSV / HEX 转换与取色器状态 |
| `clock` | 12 / 24 时制与七段、点阵、像素字形映射 |
| `playback` | 播放控件状态、悬停与进度拖动计算 |
| `pacing` | 目标帧率、帧间隔与掉帧统计 |

## 与主仓库的关系

主仓库 `SPW-Lyrics-Island` 通过 `git subtree` 把本仓库放到 `modules/studio`。它仍然是同一个 `.spmod` 的第四个源码模块，不会额外注册插件：

```text
modules/island    ← Rimliya/SPW-island
modules/lyrics    ← Rimliya/SPW-Lyrics
modules/combined  ← 生命周期与播放回调适配
modules/studio    ← 本仓库，可选的桌面歌词增强
```

## 构建

```powershell
./gradlew --no-daemon test build
```

需要 JDK 25。CI 使用 `windows-latest`，只运行测试与打包，不修改主插件。

## 许可证

[GPL-3.0](LICENSE)。本项目不包含 CDL-SPW 的代码、资源或配置键表。
