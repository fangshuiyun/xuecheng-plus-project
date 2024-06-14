package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    /**
     * @description 查询课程计划树型结构
     * @param courseId  课程id
     * @return List<TeachplanDto>
     * @author Mr.M
     * @date 2022/9/9 11:13
     */
    List<TeachplanDto> findTeachplanTree(Long courseId);

    void saveTeachplan(SaveTeachplanDto teachplanDto);

    void deleteTeachplan(Long id);

    void moveup(Long id);

    void movedown(Long id);

    void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
