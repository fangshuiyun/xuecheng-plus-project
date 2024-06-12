package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;

public class MinioTest {

    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public  void upload(){
        try {
            //根据扩展名取出mimeType
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".jpg");
            String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
            if (extensionMatch!=null){
                mimeType = extensionMatch.getMimeType();
            }

            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .object("image/C001-P16_0.jpg")//添加子目录
                    .filename("D:\\pytorch\\Rec_HandWritten\\data\\test2\\image\\C001-P16_0.jpg")
                    .contentType(mimeType)//默认根据扩展名确定文件内容类型，也可以指定
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("上传失败");
        }

    }

    @Test
    public void delete(){
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket("testbucket").object("image/C001-P16_0.jpg").build());
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    @Test
    public void getFile() {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket("testbucket").object("image/C001-P16_0.jpg").build();
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            FileOutputStream outputStream = new FileOutputStream(new File("D:\\image.jpg"));
            IOUtils.copy(inputStream,outputStream);

            //校验文件的完整性对文件的内容进行md5
            String source_md5 = DigestUtils.md5Hex(new FileInputStream(new File("D:\\pytorch\\Rec_HandWritten\\data\\test2\\image\\C001-P16_0.jpg")));
            String local_md5 = DigestUtils.md5Hex(new FileInputStream(new File("D:\\image.jpg")));
            if (source_md5.equals(local_md5)){
                System.out.println("下载成功");
            }
        }catch (Exception e) {
            e.printStackTrace();
            System.out.println("下载失败");
        }
    }


}
