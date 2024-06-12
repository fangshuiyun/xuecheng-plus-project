package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    CourseTeacherMapper courseTeacherMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Override
    public List<CourseTeacher> getCourseTeacherInfo(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        CourseTeacher teacher = courseTeacherMapper.selectOne(queryWrapper);
        List<CourseTeacher> teacherList = new ArrayList<>();
        if (teacher == null){
            teacherList.add(new CourseTeacher());
        }else {
            teacherList.add(teacher);
        }
        return teacherList;
    }

    @Transactional
    @Override
    public CourseTeacher saveCourseTeacher(Long companyId, CourseTeacher teacher) {
        Long CourseId = teacher.getCourseId();

        CourseBase courseBase = courseBaseMapper.selectById(CourseId);
        if (!courseBase.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("只允许向机构自己的课程中添加老师");
        }

        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, CourseId);
        Integer count = courseTeacherMapper.selectCount(queryWrapper);
        CourseTeacher courseTeacher = new CourseTeacher();

        if (count < 1){
            BeanUtils.copyProperties(teacher, courseTeacher);
            courseTeacherMapper.insert(courseTeacher);
        }else {
            XueChengPlusException.cast("该课程已有教师已存在");
        }
        return courseTeacherMapper.selectOne(queryWrapper);
    }


    @Override
    public CourseTeacher updateCourseTeacher(CourseTeacher teacher) {
        Long id = teacher.getId();
        CourseTeacher courseTeacher = new CourseTeacher();
        BeanUtils.copyProperties(teacher, courseTeacher);
        courseTeacher.setId(id);
        courseTeacherMapper.updateById(courseTeacher);
        return courseTeacherMapper.selectById(id);
    }

    @Transactional
    @Override
    public void deleteCourseTeacher(Long courseId, Long id) {
        courseTeacherMapper.deleteById(id);
    }
}
