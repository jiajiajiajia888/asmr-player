# 第三方来源与许可

项目自身的Apache-2.0许可不替代以下组件和数据的原许可。

## AndroidX Media3

直接依赖：`media3-exoplayer`、`media3-transformer`、`media3-session`，版本均为1.11.1。项目通过Google Maven和Maven Central解析其传递依赖。

上游：https://github.com/androidx/media

许可：Apache License, Version 2.0。完整正文见项目`LICENSE`。依赖不会以源码复制到本仓库；构建时从上游仓库获取。传递依赖的实际版本与各自许可见`docs/依赖许可清单.md`，其许可和版权归各自权利人。

## KEMAR双耳测量数据

作者：Bill Gardner、Keith Martin；MIT Media Laboratory，1994。

官方说明：https://sound.media.mit.edu/resources/KEMAR.html

原始许可：https://sound.media.mit.edu/resources/KEMAR/README

原始声明：

> This data is Copyright 1994 by the MIT Media Laboratory. It is provided free with no restrictions on use, provided the authors are cited when the data is used in any research or commercial application.

项目中`KemarData.java`包含由原始compact数据派生的响应，保留作者和来源标注。数据的原始署名条件继续适用；这不是对数据改用Apache或MIT软件许可证。

## Gradle Wrapper

`gradlew`、`gradlew.bat`及`gradle/wrapper/gradle-wrapper.jar`由既有Gradle 8.13生成。上游：https://github.com/gradle/gradle ，采用Apache-2.0许可，脚本保留其上游版权和许可声明。
