package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 文件上传控制器
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@Api(tags = "文件上传管理")
public class FileUploadController {

    // 从配置文件读取上传路径，如果没有配置则使用默认值
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    // 从配置文件读取访问URL前缀
    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    // 支持的图片格式
    private static final String[] ALLOWED_IMAGE_TYPES = {
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    };

    // 最大文件大小（10MB）
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @PostMapping("/violation-photos")
    @ApiOperation("上传违规照片")
    public Result<Map<String, Object>> uploadViolationPhotos(
            @ApiParam("上传的文件") @RequestParam("file") MultipartFile file,
            @ApiParam("违规类型") @RequestParam(required = false) String type,
            @ApiParam("用户ID") @RequestParam(required = false) String userId,
            @ApiParam("时间戳") @RequestParam(required = false) String timestamp,
            HttpServletRequest request) {
        
        return uploadFile(file, "violation-photos", request);
    }

    @PostMapping("/evidence-files")
    @ApiOperation("上传证据文件")
    public Result<Map<String, Object>> uploadEvidenceFiles(
            @ApiParam("上传的文件") @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        
        return uploadFile(file, "evidence-files", request);
    }

    @PostMapping("/avatars")
    @ApiOperation("上传头像")
    public Result<Map<String, Object>> uploadAvatars(
            @ApiParam("上传的文件") @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        
        return uploadFile(file, "avatars", request);
    }

    @PostMapping("/files")
    @ApiOperation("通用文件上传")
    public Result<Map<String, Object>> uploadGeneralFiles(
            @ApiParam("上传的文件") @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        
        return uploadFile(file, "general", request);
    }

    /**
     * 通用文件上传方法
     * @param file 上传的文件
     * @param category 文件分类（violation-photos, evidence-files, avatars, general）
     * @param request HTTP请求
     * @return 上传结果
     */
    private Result<Map<String, Object>> uploadFile(MultipartFile file, String category, HttpServletRequest request) {
        try {
            // 验证文件
            Result<String> validationResult = validateFile(file, category);
            if (!"0".equals(validationResult.getCode())) {
                return Result.error(validationResult.getMsg());
            }

            // 生成文件名和路径
            String fileName = generateFileName(file.getOriginalFilename());
            String relativePath = buildRelativePath(category, fileName);
            // 使用File构造函数确保路径正确拼接
            File fullPathFile = new File(uploadPath, relativePath);
            String fullPath = fullPathFile.getAbsolutePath();

            // 确保目录存在
            File directory = new File(fullPath).getParentFile();
            if (!directory.exists()) {
                log.info("目录不存在，正在创建: {}", directory.getAbsolutePath());
                boolean created = directory.mkdirs();
                if (!created) {
                    log.error("创建目录失败: {}", directory.getAbsolutePath());
                    log.error("检查路径权限和磁盘空间。当前上传路径配置: {}", uploadPath);
                    return Result.error("创建上传目录失败，请检查服务器配置");
                } else {
                    log.info("目录创建成功: {}", directory.getAbsolutePath());
                }
            } else {
                log.debug("目录已存在: {}", directory.getAbsolutePath());
            }

            // 保存文件
            File destFile = new File(fullPath);
            file.transferTo(destFile);

            // 构建返回的URL
            String url = buildFileUrl(relativePath, request);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("fileName", fileName);
            result.put("originalName", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("category", category);
            result.put("uploadTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), url);
            return Result.success(result);

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传出现未知错误: {}", e.getMessage(), e);
            return Result.error("文件上传失败");
        }
    }

    /**
     * 验证上传的文件
     */
    private Result<String> validateFile(MultipartFile file, String category) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.error("上传的文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过10MB");
        }

        // 检查文件名
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            return Result.error("文件名不能为空");
        }

        // 如果是图片分类，检查文件类型
        if ("violation-photos".equals(category) || "avatars".equals(category)) {
            String extension = getFileExtension(originalName).toLowerCase();
            boolean isValidImage = false;
            for (String allowedType : ALLOWED_IMAGE_TYPES) {
                if (allowedType.equals(extension)) {
                    isValidImage = true;
                    break;
                }
            }
            if (!isValidImage) {
                return Result.error("只支持上传图片文件（jpg, jpeg, png, gif, bmp, webp）");
            }
        }

        return Result.success("验证通过");
    }

    /**
     * 生成唯一的文件名
     */
    private String generateFileName(String originalName) {
        String extension = getFileExtension(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestamp + "_" + uuid + extension;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(lastDot) : "";
    }

    /**
     * 构建相对路径
     */
    private String buildRelativePath(String category, String fileName) {
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 使用File.separator确保路径分隔符正确
        return category + File.separator + dateFolder.replace("/", File.separator) + File.separator + fileName;
    }

    /**
     * 构建文件访问URL
     */
    private String buildFileUrl(String relativePath, HttpServletRequest request) {
        // 获取请求的基础URL
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        
        // 只有在非标准端口时才添加端口号
        if (("http".equals(scheme) && serverPort != 80) || 
            ("https".equals(scheme) && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }
        
        baseUrl.append(contextPath);
        
        // 确保URL中使用正确的"/"分隔符，而不是系统的文件分隔符
        String urlPath = relativePath.replace(File.separator, "/");
        return baseUrl.toString() + urlPrefix + "/" + urlPath;
    }
} 