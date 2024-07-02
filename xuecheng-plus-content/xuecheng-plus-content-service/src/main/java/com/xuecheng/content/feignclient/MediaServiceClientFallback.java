package com.xuecheng.content.feignclient;

import org.springframework.web.multipart.MultipartFile;

public class MediaServiceClientFallback implements MediaServiceClient{

    @Override
    public String uploadFile(MultipartFile upload, String objectName) {
        return null;
    }
}
