package me.dreamvoid.apkrenamer;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import net.dongliu.apk.parser.bean.CertificateMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.*;

public class ApkRenamer {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. 获取目录路径
        System.out.print("请输入包含 APK/XAPK/APKS 文件的目录路径 (留空则使用当前目录): ");
        String inputPath = scanner.nextLine();
        String targetDirectoryPath = inputPath.isEmpty() ? System.getProperty("user.dir") : inputPath;
        File targetDir = new File(targetDirectoryPath);

        if (!targetDir.isDirectory()) {
            System.err.println("错误: 提供的路径不是一个有效的目录: " + targetDirectoryPath);
            return;
        }

        System.out.println("将在目录 '" + targetDir.getAbsolutePath() + "' 中查找安装包文件...");

        // 2. 遍历文件
        File[] files = targetDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".apk") ||
                name.toLowerCase().endsWith(".xapk") || // 初步包含，但解析逻辑可能需要区分
                name.toLowerCase().endsWith(".apks")   // 初步包含，但解析逻辑可能需要区分
        );

        if (files == null || files.length == 0) {
            System.out.println("在指定目录中未找到任何 .apk, .xapk, 或 .apks 文件。");
            return;
        }

        System.out.println("找到 " + files.length + " 个安装包文件。准备处理...");
        Map<File, String> renameMap = new LinkedHashMap<>(); // 使用 LinkedHashMap 保持处理顺序

        // 3 & 4. 解析并生成新文件名 (目前仅处理 APK)
        for (File file : files) {
            String originalFilename = file.getName();
            String extension = getFileExtension(originalFilename);

            if (!extension.equalsIgnoreCase("apk")) {
                System.out.println("跳过非 APK 文件 (XAPK/APKS 暂不支持详细解析): " + originalFilename);
                continue; // 暂时跳过 XAPK/APKS
            }

            try (ApkFile apkFile = new ApkFile(file)) {
                 // 设置优先语言为中文
                apkFile.setPreferredLocale(Locale.CHINA);
                ApkMeta apkMeta = apkFile.getApkMeta();
                
                // 获取应用名称 (优先中文)
                String appName = apkMeta.getLabel(); // getLabel 会根据 preferredLocale 获取
                if (appName == null || appName.trim().isEmpty()) {
                     // 如果没有获取到中文名，尝试获取默认的应用名
                     apkMeta = apkFile.getApkMeta(); // 尝试默认 locale
                     appName = apkMeta.getName(); // getName 通常是 AndroidManifest里的 application name
                     if (appName == null || appName.trim().isEmpty()){
                         appName = apkMeta.getLabel(); // fallback to label again even if it's not Chinese
                     }
                }
                // 如果仍然为空，使用原始文件名（不含扩展名）作为备用
                 if (appName == null || appName.trim().isEmpty()) {
                    appName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
                 }


                String versionName = apkMeta.getVersionName();
                if (versionName == null) versionName = "未知版本名";

                long versionCode = apkMeta.getVersionCode();

                // 检查签名
                boolean isSigned = false;
                try {
                    // getCertificateMetas 会在未签名时抛出异常或返回空列表
                    List<CertificateMeta> certs = apkFile.getCertificateMetaList();
                    isSigned = certs != null && !certs.isEmpty();
                } catch (CertificateException e) {
                    // 认为未签名
                    isSigned = false; 
                } catch (IOException e) {
                    System.err.println("检查签名时出错: " + originalFilename + " - " + e.getMessage());
                    // 可以在这里决定是跳过还是标记为未签名
                    isSigned = false;
                }


                String signedStatus = isSigned ? "signed" : "";

                // 清理文件名中的非法字符 (简单替换)
                String cleanAppName = sanitizeFilename(appName);
                String cleanVersionName = sanitizeFilename(versionName);

                // 组合新文件名
                String newFilename = String.format("%s-%s-%d-%s.%s",
                        cleanAppName,
                        cleanVersionName,
                        versionCode,
                        signedStatus,
                        extension);

                // 检查新旧文件名是否相同
                 if (!originalFilename.equals(newFilename)) {
                    renameMap.put(file, newFilename);
                    System.out.println("\n计划重命名:");
                    System.out.println("  原始: " + originalFilename);
                    System.out.println("  新的: " + newFilename);
                 } else {
                     System.out.println("\n文件已符合命名规范，无需重命名: " + originalFilename);
                 }


            } catch (IOException e) {
                System.err.println("处理文件时出错: " + originalFilename + " - " + e.getMessage());
            }
        }

        if (renameMap.isEmpty()) {
            System.out.println("\n没有需要重命名的文件。");
            return;
        }

        // 5. 用户确认
        System.out.print("\n是否执行以上所有计划的重命名操作? (y/n): ");
        String confirmation = scanner.nextLine();

        // 6. 执行重命名
        if (confirmation.equalsIgnoreCase("y")) {
            System.out.println("\n开始执行重命名...");
            int successCount = 0;
            int failCount = 0;
            for (Map.Entry<File, String> entry : renameMap.entrySet()) {
                File oldFile = entry.getKey();
                String newFilename = entry.getValue();
                File newFile = new File(oldFile.getParent(), newFilename);

                // 再次检查目标文件是否已存在 (以防万一)
                 if (newFile.exists()) {
                    System.err.println("  失败: 目标文件名已存在，跳过 '" + oldFile.getName() + "' -> '" + newFilename + "'");
                    failCount++;
                    continue;
                 }

                try {
                    Path sourcePath = oldFile.toPath();
                    Path targetPath = newFile.toPath();
                    Files.move(sourcePath, targetPath);
                    System.out.println("  成功: '" + oldFile.getName() + "' -> '" + newFilename + "'");
                    successCount++;
                } catch (IOException e) {
                    System.err.println("  失败: 重命名 '" + oldFile.getName() + "' 到 '" + newFilename + "' 时出错 - " + e.getMessage());
                    failCount++;
                }
            }
            System.out.println("\n重命名操作完成。成功: " + successCount + ", 失败: " + failCount);
        } else {
            System.out.println("\n用户取消了重命名操作。");
        }

        scanner.close();
    }

    // 辅助方法：获取文件扩展名
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return ""; // 没有扩展名
    }

    // 辅助方法：清理文件名中的非法字符
    private static String sanitizeFilename(String name) {
        // 移除或替换 Windows 和 Linux 文件名中不允许的字符
        // 包括: \ / : * ? " < > |
        // 这里简单替换为空字符串，可以根据需要替换为其他字符，如'_'
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
} 