package com.cuit.interviewsystem.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.cuit.interviewsystem.config.AliOSSConfig;
import com.cuit.interviewsystem.exception.BusinessException;
import com.cuit.interviewsystem.exception.ErrorEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class AliOSSUtil {
    @Resource
    private OSS ossClient;
    @Resource
    private AliOSSConfig aliOSSConfig;

    // 允许的图片格式
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif");

    /**
     * 上传文件, 若file为空则返回null
     * @param file 需上传的文件
     * @param prefix 文件名前缀
     * @return 上传后的文件地址
     * @throws IOException
     */
    public String uploadFile(MultipartFile file, @Deprecated String prefix) throws IOException {
        if (file == null) {
            return null;
        }
        String fileName = file.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString().replace("-", "");
//        file.transferTo(new File("../../templates" + newFileName + suffix));
        //获取文件的后缀，产生以为不重复的名称
        //调用阿里云的SDK上传至OSS
        PutObjectRequest putObjectRequest = new PutObjectRequest(aliOSSConfig.getBucketName(), newFileName + suffix,
                file.getInputStream());

        // 上传文件。
        ossClient.putObject(putObjectRequest);
        return "https://" + aliOSSConfig.getBucketName() + "." + aliOSSConfig.getEndpoint() + "/" + newFileName + suffix;
    }

    /**
     * 上传图片并校验
     * @param picture 需上传的图片
     * @param prefix 文件名前缀
     * @return 上传后的图片地址
     * @throws IOException
     */
    public String uploadPictureAndCheck(MultipartFile picture, String prefix) throws IOException {
        // 校验图片是否为空
        if (picture == null || picture.isEmpty()) {
            throw new BusinessException(ErrorEnum.NOT_FOUND_ERROR, "图片不能为空");
        }

        // 校验图片类型
        String contentType = picture.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR, "不支持的图片格式，仅支持jpg、jpeg、png、gif格式");
        }

        // 调用uploadFile方法上传图片
        return uploadFile(picture, prefix);
    }

    public void deleteFile(String fileUrl) throws OSSException, ClientException {
        //判断fileUrl是否为空字符串
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        //提取出文件名
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        //删除文件
        ossClient.deleteObject(aliOSSConfig.getBucketName(), fileName);
    }
}
