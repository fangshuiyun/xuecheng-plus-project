package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CourseTeacherController {

    @Autowired
    CourseTeacherService courseTeacherService;


    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> selectCourseTeacher(@PathVariable Long courseId){
        return courseTeacherService.getCourseTeacherInfo(courseId);
    }

    @PostMapping("/courseTeacher")
    public CourseTeacher saveCourseTeacher(@RequestBody @Validated CourseTeacher teacher){
        //机构id，由于认证系统没有上线暂时硬编码
        Long companyId = 1232141425L;
        return courseTeacherService.saveCourseTeacher(companyId, teacher);
    }

    @PutMapping("/courseTeacher")
    public CourseTeacher updateCourseTeacher(@RequestBody @Validated CourseTeacher teacher){
        return courseTeacherService.updateCourseTeacher(teacher);
    }

    @DeleteMapping("/courseTeacher/course/{courseId}/{id}")
    public void deleteCourseTeacher(@PathVariable Long courseId, @PathVariable Long id){
        courseTeacherService.deleteCourseTeacher(courseId, id);
    }
}
