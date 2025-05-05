# APK Renamer

一个简单的使用 AI 生成的小工具，用于根据 Android 安装包（APK）的元数据（应用名、版本名、版本号、签名状态）批量重命名 APK 文件。

## 功能

- 扫描指定目录（或当前目录）下的 `.apk` 文件。
- 解析每个 APK 文件，提取以下信息：
    - 应用名称（优先使用中文名称，若无则使用通用名称）
    - 版本名称 (`versionName`)
    - 版本号 (`versionCode`)
    - 签名状态
- 根据提取的信息生成新的文件名，格式为：`应用名称-版本名称-版本号-签名状态.apk`。
    - 如果文件已签名，`签名状态` 为 `signed`，否则为空字符串。
    - 文件名中的非法字符将被替换为下划线 `_`。
- 在执行重命名之前，显示原始文件名和新的文件名，并请求用户确认。
- **注意：** 目前仅支持处理 `.apk` 文件，`.xapk` 和 `.apks` 文件会被跳过。

## 构建

这是一个标准的 Maven 项目。您可以使用以下命令进行构建：

1.  **编译项目:**
    ```bash
    mvn compile
    ```
2.  **(可选) 打包成可执行 JAR:**
    ```bash
    mvn package
    ```
    这将在 `target` 目录下生成一个 JAR 文件（例如 `APKRenamer-1.0-SNAPSHOT.jar`）以及一个包含所有依赖的 JAR 文件（例如 `APKRenamer-1.0-SNAPSHOT-jar-with-dependencies.jar`）。推荐使用带依赖的 JAR 文件来运行。

## 使用

您可以通过以下方式运行此工具：

**方式一：使用 Maven 执行 (需要安装 Maven)**

```bash
mvn exec:java -Dexec.mainClass="me.dreamvoid.apkrenamer.ApkRenamer"
```

**方式二：运行打包好的 JAR 文件 (推荐)**

首先，使用 `mvn package` 命令打包（确保生成了带依赖的 JAR）。然后运行：

```bash
java -jar target/APKRenamer-1.0-SNAPSHOT-jar-with-dependencies.jar 
# 注意：请将 JAR 文件名替换为您实际生成的文件名
```

**运行流程：**

1.  程序启动后，会提示您输入包含 APK 文件的目录路径。
2.  直接按回车键将使用程序运行的当前目录。
3.  输入您存放 APK 文件的目录路径，然后按回车。
4.  程序将扫描目录，解析 APK 文件，并显示计划重命名的文件列表（原始文件名 -> 新文件名）。
5.  程序会询问您是否执行重命名操作，输入 `y` 并按回车确认，输入其他任何字符或直接回车将取消操作。
6.  程序执行重命名（如果确认），并报告成功和失败的数量。

## 依赖

- [net.dongliu:apk-parser](https://github.com/hsiafan/apk-parser): 用于解析 APK 文件元数据。

## 注意事项

- 请确保您的 Java 环境已正确安装（推荐 Java 11 或更高版本，根据 `pom.xml` 中的设置）。
- 程序会尝试获取应用的中文名称。如果 APK 没有提供中文名称，则会尝试使用其默认的应用名称。
- 文件名中的特殊字符（如 `\ / : * ? " < > |`）会被替换为下划线 `_`，以确保文件名的有效性。
