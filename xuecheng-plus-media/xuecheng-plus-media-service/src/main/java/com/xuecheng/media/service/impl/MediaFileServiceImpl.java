package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
 @Service
 @Slf4j
public class MediaFileServiceImpl implements MediaFileService {

 @Autowired
 MinioClient minioClient;

 @Autowired
 MediaFilesMapper mediaFilesMapper;

 @Autowired
 MediaFileService currentProxy;

 @Autowired
 MediaProcessMapper mediaProcessMapper;

 //普通文件桶
 @Value("${minio.bucket.files}")
 private String bucket_files;

 //普通文件桶
 @Value("${minio.bucket.videofiles}")
 private String bucket_video;

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
  
  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }


 //获取文件默认存储目录路径 年/月/日
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/")+"/";
  return folder;
 }

 //获取文件的md5
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }


 private String getMimeType(String extension){
  if(extension==null)
   extension = "";
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
  //通用mimeType，字节流
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
  if(extensionMatch!=null){
   mimeType = extensionMatch.getMimeType();
  }
  return mimeType;
 }


 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName) {
  try {
   UploadObjectArgs testbucket = UploadObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .filename(localFilePath)
           .contentType(mimeType)
           .build();
   minioClient.uploadObject(testbucket);
   log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
   System.out.println("上传成功");
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage(),e);
   XueChengPlusException.cast("上传文件到文件系统失败");
  }
  return false;
 }


 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath, String objectName) {
  //文件名称
  String filename = uploadFileParamsDto.getFilename();
  //文件扩展名
  String extension = filename.substring(filename.lastIndexOf("."));
  //文件mimeType
  String mimeType = getMimeType(extension);
  //文件的md5值
  String fileMd5 = getFileMd5(new File(localFilePath));
  //文件的默认目录
  String defaultFolderPath = getDefaultFolderPath();
  if(StringUtils.isEmpty(objectName)){
   //存储到minio中的对象名(带目录)
   objectName =  defaultFolderPath + fileMd5 + extension;
  }

  //将文件上传到minio
  boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_files, objectName);
  if (!result){
   XueChengPlusException.cast("上传文件失败");
  }

  //将文件信息存储到数据库
  MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_files, objectName);
  //准备返回数据
  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
  return uploadFileResultDto;
 }

 @Transactional
 public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
  //从数据库查询文件
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles == null) {
   mediaFiles = new MediaFiles();
   //拷贝基本信息
   BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
   mediaFiles.setId(fileMd5);
   mediaFiles.setFileId(fileMd5);
   mediaFiles.setCompanyId(companyId);
   mediaFiles.setUrl("/" + bucket + "/" + objectName);
   mediaFiles.setBucket(bucket);
   mediaFiles.setFilePath(objectName);
   mediaFiles.setCreateDate(LocalDateTime.now());
   mediaFiles.setAuditStatus("002003");
   mediaFiles.setStatus("1");
   //保存文件信息到文件表
   int insert = mediaFilesMapper.insert(mediaFiles);
   if (insert < 0) {
    log.error("保存文件信息到数据库失败,{}",mediaFiles.toString());
    XueChengPlusException.cast("保存文件信息失败");
   }
   //添加到待处理任务表
   addWaitingTask(mediaFiles);
   log.debug("保存文件信息到数据库成功,{}",mediaFiles.toString());

  }
  return mediaFiles;
 }

 private void addWaitingTask(MediaFiles mediaFiles) {
  //文件名称
  String filename = mediaFiles.getFilename();
  //文件扩展名
  String extension = filename.substring(filename.lastIndexOf("."));
  //文件mimeType
  String mimeType = getMimeType(extension);
  //如果是avi视频添加到视频待处理表
  if(mimeType.equals("video/x-msvideo")) {
   MediaProcess mediaProcess = new MediaProcess();
   BeanUtils.copyProperties(mediaFiles,mediaProcess);
   mediaProcess.setStatus("1");//未处理
   mediaProcess.setFailCount(0);//失败次数默认为0
   mediaProcess.setCreateDate(LocalDateTime.now());
   mediaProcess.setUrl(null);
   mediaProcessMapper.insert(mediaProcess);

  }
 }

 @Override
 public RestResponse<Boolean> checkFile(String fileMd5) {
  //查询文件信息
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles != null){
   //桶
   String bucket = mediaFiles.getBucket();
   //存储目录
   String filePath = mediaFiles.getFilePath();
   GetObjectArgs getObjectArgs = GetObjectArgs.builder()
           .bucket(bucket)
           .object(filePath)
           .build();
   try {
    FilterInputStream stream = minioClient.getObject(getObjectArgs);
    if (stream != null) {
     //文件已存在
     return RestResponse.success(true);
    }
   }catch (Exception e) {
     e.printStackTrace();
   }
  }
  //文件不存在
  return RestResponse.success(false);
 }

 @Override
 public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

  //得到分块文件目录
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  //得到分块文件的路径
  String chunkFilePath = chunkFileFolderPath + chunkIndex;

  //文件流
  FilterInputStream fileInputStream = null;
  try {
   fileInputStream = minioClient.getObject(
           GetObjectArgs.builder()
                   .bucket(bucket_video)
                   .object(chunkFilePath)
                   .build());

   if (fileInputStream != null) {
    //分块已存在
    return RestResponse.success(true);
   }
  } catch (Exception e) {

  }
  //分块未存在
  return RestResponse.success(false);
 }

 //得到分块文件的目录
 private String getChunkFileFolderPath(String fileMd5) {
  return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
 }


 @Override
 public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
  //得到分块文件的目录路径
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  //得到分块文件的路径
  String chunkFilePath = chunkFileFolderPath + chunk;
  //mimeType
  String mimeType = getMimeType(null);
  //将文件存储至minIO
  boolean b = addMediaFilesToMinIO(localChunkFilePath, mimeType, bucket_video, chunkFilePath);
  if (!b) {
   log.debug("上传分块文件失败:{}", chunkFilePath);
   return RestResponse.validfail(false, "上传分块失败");
  }
  log.debug("上传分块文件成功:{}",chunkFilePath);
  return RestResponse.success(true);
 }

 @Override
 public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
  //=====获取分块文件路径=====
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  //组成将分块文件路径组成 List<ComposeSource>
  List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
          .limit(chunkTotal)
          .map(i -> ComposeSource.builder()
                  .bucket(bucket_video)
                  .object(chunkFileFolderPath.concat(Integer.toString(i)))
                  .build())
          .collect(Collectors.toList());

  //文件名称
  String fileName = uploadFileParamsDto.getFilename();
  //文件扩展名
  String extName = fileName.substring(fileName.lastIndexOf("."));
  //合并文件路径
  String mergeFilePath = getFilePathByMd5(fileMd5, extName);

  try {
   ObjectWriteResponse response = minioClient.composeObject(
           ComposeObjectArgs.builder()
                   .bucket(bucket_video)
                   .object(mergeFilePath)
                   .sources(sourceObjectList)
                   .build());
   log.debug("合并文件成功:{}",mergeFilePath);
  } catch (Exception e) {
   e.printStackTrace();
   log.debug("合并文件失败,fileMd5:{},异常:{}",fileMd5,e.getMessage());
   return RestResponse.validfail(false, "合并文件失败。");
  }
  //下载合并后的文件
  File minioFile = downloadFileFromMinIO(bucket_video,mergeFilePath);
  if(minioFile == null){
   log.debug("下载合并后文件失败,mergeFilePath:{}",mergeFilePath);
   return RestResponse.validfail(false, "下载合并后文件失败。");
  }
  try (FileInputStream fileInputStream = new FileInputStream(minioFile)) {
   //minio上文件的md5值
   String md5Hex = DigestUtils.md5Hex(fileInputStream);
   //比较md5值，不一致则说明文件不完整
   if(!fileMd5.equals(md5Hex)){
    return RestResponse.validfail(false, "文件合并校验失败，最终上传失败。");
   }
   uploadFileParamsDto.setFileSize(minioFile.length());
  }catch (Exception e){
   log.debug("校验文件失败,fileMd5:{},异常:{}",fileMd5,e.getMessage());
   return RestResponse.validfail(false, "文件合并校验失败，最终上传失败。");
  }

  //文件入库
  currentProxy.addMediaFilesToDb(companyId,fileMd5,uploadFileParamsDto,bucket_video,mergeFilePath);
  //=====清除分块文件=====
  clearChunkFiles(chunkFileFolderPath,chunkTotal);
  return RestResponse.success(true);

 }


 private String getFilePathByMd5(String fileMd5,String fileExt){
  return   fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
 }


 /**
  * 从minio下载文件
  * @param bucket 桶
  * @param objectName 对象名称
  * @return 下载后的文件
  */
 public File downloadFileFromMinIO(String bucket,String objectName){
  //临时文件
  File minioFile = null;
  FileOutputStream outputStream = null;
  try{
   InputStream stream = minioClient.getObject(GetObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .build());
   //创建临时文件
   minioFile=File.createTempFile("minio", ".merge");
   outputStream = new FileOutputStream(minioFile);
   IOUtils.copy(stream,outputStream);
   return minioFile;
  } catch (Exception e) {
   e.printStackTrace();
  }finally {
   if(outputStream!=null){
    try {
     outputStream.close();
    } catch (IOException e) {
     e.printStackTrace();
    }
   }
  }
  return null;
 }

 /**
  * 清除分块文件
  * @param chunkFileFolderPath 分块文件路径
  * @param chunkTotal 分块文件总数
  */
 private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){

  try {
   List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
           .limit(chunkTotal)
           .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
           .collect(Collectors.toList());

   RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(deleteObjects).build();
   Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
   results.forEach(r->{
    DeleteError deleteError = null;
    try {
     deleteError = r.get();
    } catch (Exception e) {
     e.printStackTrace();
     log.error("清楚分块文件失败,objectname:{}",deleteError.objectName(),e);
    }
   });
  } catch (Exception e) {
   e.printStackTrace();
   log.error("清楚分块文件失败,chunkFileFolderPath:{}",chunkFileFolderPath,e);
  }
 }

 @Override
 public MediaFiles getFileById(String mediaId) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
  return mediaFiles;
 }
}
