package org.example.springboot.service;


import io.micrometer.common.util.StringUtils;
import org.example.springboot.common.Result;
import org.example.springboot.common.ResultUtils;
import org.example.springboot.common.ResultCode;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.enumClass.FileType;
import org.example.springboot.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);
    
    public Result<?> upLoad(MultipartFile file, FileType fileType) {
        try {
            if (file == null || com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(file.getOriginalFilename())) {
                LOGGER.warn("文件上传失败，文件为空或文件名为空");
                return ResultUtils.paramError("文件不存在或文件名为空");
            }
            
            if (fileType == null) {
                LOGGER.warn("文件上传失败，文件类型为空");
                return ResultUtils.paramError("文件类型不能为空");
            }
            
            LOGGER.info("开始上传文件：{}，文件类型：{}", file.getOriginalFilename(), fileType.getTypeName());
            String path = FileUtil.saveFile(file, null, fileType.getTypeName());
            
            if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(path)) {
                LOGGER.info("文件上传成功：{}，保存路径：{}", file.getOriginalFilename(), path);
                return ResultUtils.success(path);
            } else {
                LOGGER.warn("文件上传失败，保存路径为空，文件名：{}", file.getOriginalFilename());
                throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "文件上传失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("文件上传异常，文件名：{}，错误：{}", 
                file != null ? file.getOriginalFilename() : "null", e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "文件上传失败：" + e.getMessage());
        }
    }
    public Result<?> fileRemove(String filename) {
        try {
            if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(filename)) {
                LOGGER.warn("删除文件失败，文件名为空");
                return ResultUtils.paramError("文件名不能为空");
            }
            
            String filePath = "\\img\\" + filename;
            LOGGER.info("开始删除文件：{}", filePath);
            
            boolean res = FileUtil.deleteFile(filePath);
            
            if (res) {
                LOGGER.info("文件删除成功：{}", filePath);
                return ResultUtils.success();
            } else {
                LOGGER.warn("文件删除失败，文件路径：{}", filePath);
                return ResultUtils.dataNotFound("文件不存在或删除失败");
            }
        } catch (Exception e) {
            LOGGER.error("文件删除异常，文件名：{}，错误：{}", filename, e.getMessage(), e);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "删除文件失败：" + e.getMessage());
        }
    }

    public List<String> uploadMultiple(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            LOGGER.warn("批量上传文件失败，文件列表为空");
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件列表不能为空");
        }

        List<String> successPaths = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        LOGGER.info("开始批量上传文件，文件数量：{}", files.size());
        
        for (MultipartFile file : files) {
            try {
                if (file == null || StringUtils.isEmpty(file.getOriginalFilename())) {
                    String fileName = file != null ? file.getOriginalFilename() : "null";
                    failedFiles.add(fileName + ": 文件不存在");
                    LOGGER.warn("批量上传文件，文件名为空，跳过：{}", fileName);
                    continue;
                }
                
                LOGGER.debug("上传文件：{}", file.getOriginalFilename());
                String path = FileUtil.saveFile(file, null, FileType.COMMON.getTypeName());
                
                if (StringUtils.isNotBlank(path)) {
                    successPaths.add(path);
                    LOGGER.debug("文件上传成功：{}，路径：{}", file.getOriginalFilename(), path);
                } else {
                    failedFiles.add(file.getOriginalFilename() + ": 文件上传失败");
                    LOGGER.warn("文件上传失败，保存路径为空：{}", file.getOriginalFilename());
                }
            } catch (Exception e) {
                String fileName = file != null ? file.getOriginalFilename() : "null";
                LOGGER.error("文件上传异常，文件名：{}，错误：{}", fileName, e.getMessage(), e);
                failedFiles.add(fileName + ": 文件上传时发生异常");
            }
        }

        // 检查是否所有文件都成功上传
        if (!failedFiles.isEmpty()) {
            LOGGER.warn("批量上传文件部分失败，成功：{}，失败：{}", successPaths.size(), failedFiles.size());
            
            // 如果有文件上传失败，删除所有成功上传的文件
            for (String path : successPaths) {
                try {
                    File uploadedFile = new File(path);
                    if (uploadedFile.exists() && uploadedFile.isFile()) {
                        if (uploadedFile.delete()) {
                            LOGGER.info("已删除成功上传的文件：{}", path);
                        } else {
                            LOGGER.warn("删除文件失败：{}", path);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("删除文件异常，路径：{}，错误：{}", path, e.getMessage(), e);
                }
            }

            // 抛出异常，包含失败的文件信息
            String errorMessage = "批量上传文件失败，失败的文件：" + String.join(", ", failedFiles);
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), errorMessage);
        } else {
            // 如果全部成功，则只返回成功路径
            LOGGER.info("批量上传文件全部成功，文件数量：{}", successPaths.size());
            return successPaths;
        }
    }
}
