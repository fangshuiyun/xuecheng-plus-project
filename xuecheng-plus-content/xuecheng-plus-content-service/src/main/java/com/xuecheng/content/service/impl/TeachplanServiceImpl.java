package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }


    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        //课程计划id
        Long id = teachplanDto.getId();
        //修改课程计划
        if(id!=null){
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }else{
            //取出同父同级别的课程计划数量
            int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());
            Teachplan teachplanNew = new Teachplan();
            //设置排序号
            teachplanNew.setOrderby(count+1);
            BeanUtils.copyProperties(teachplanDto,teachplanNew);

            teachplanMapper.insert(teachplanNew);

        }
    }

    private int getTeachplanCount(Long courseId,Long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        queryWrapper.eq(Teachplan::getParentid,parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }

    @Transactional
    @Override
    public void deleteTeachplan(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);

        if (teachplan == null) {
            XueChengPlusException.cast("无法找到该章节");
        }

        LambdaQueryWrapper<Teachplan> queryWrapper  = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getParentid, id);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        if (count > 0){
            XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
        }
        teachplanMapper.deleteById(id);

        LambdaQueryWrapper<TeachplanMedia> query = new LambdaQueryWrapper<>();
        query.eq(TeachplanMedia::getTeachplanId, id);
        Integer mediaCount = teachplanMediaMapper.selectCount(query);
        if (mediaCount > 0){
            teachplanMediaMapper.delete(query);
        }
    }

    @Override
    public void moveup(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        Long parentid = teachplan.getParentid();
        Long courseId = teachplan.getCourseId();
        Integer targetOrderby  = teachplan.getOrderby();
        if (targetOrderby == 1){
            XueChengPlusException.cast("已经是第一个了，无法继续上移");
        }

        LambdaQueryWrapper<Teachplan> query = new LambdaQueryWrapper<>();
        query.eq(Teachplan::getCourseId, courseId);
        query.eq(Teachplan::getParentid, parentid);
        query.eq(Teachplan::getOrderby, targetOrderby-1);
        //找到上一个，更新order
        Teachplan preTeachplan = teachplanMapper.selectOne(query);
        preTeachplan.setOrderby(preTeachplan.getOrderby()+1);
        teachplanMapper.updateById(preTeachplan);

        teachplan.setOrderby(targetOrderby-1);
        teachplanMapper.updateById(teachplan);
    }

    @Override
    public void movedown(Long id) {
        Teachplan teachplan = teachplanMapper.selectById(id);
        Long parentid = teachplan.getParentid();
        Long courseId = teachplan.getCourseId();
        Integer targetOrderby  = teachplan.getOrderby();

        LambdaQueryWrapper<Teachplan> query = new LambdaQueryWrapper<>();
        query.eq(Teachplan::getCourseId, courseId);
        query.eq(Teachplan::getParentid, parentid);
        Integer count = teachplanMapper.selectCount(query);
        if (count == targetOrderby){
            XueChengPlusException.cast("已经是最后一个了，无法继续下移");
        }
        query.eq(Teachplan::getOrderby, targetOrderby+1);
        Teachplan nextTeachplan  = teachplanMapper.selectOne(query);
        nextTeachplan.setOrderby(nextTeachplan.getOrderby()-1);
        teachplanMapper.updateById(nextTeachplan);

        teachplan.setOrderby(targetOrderby+1);
        teachplanMapper.updateById(teachplan);
    }
}