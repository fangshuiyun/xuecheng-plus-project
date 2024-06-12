package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

public interface CourseTeacherService {
    List<CourseTeacher> getCourseTeacherInfo(Long courseId);

    CourseTeacher saveCourseTeacher(Long companyId, CourseTeacher teacher);

    CourseTeacher updateCourseTeacher(CourseTeacher teacher);

    void deleteCourseTeacher(Long courseId, Long id);
}
