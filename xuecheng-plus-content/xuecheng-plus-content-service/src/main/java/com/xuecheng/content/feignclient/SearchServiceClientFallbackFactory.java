package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SearchServiceClientFallbackFactory implements FallbackFactory<SearchServiceClient>{
    @Override
    public SearchServiceClient create(Throwable throwable) {
        return new SearchServiceClient() {
            @Override
            public Boolean add(CourseIndex courseIndex) {
                //降级方法
                log.debug("添加课程索引时发生熔断，异常信息:{}",throwable.toString(),throwable);
                return false;
            }
        };
    }
}
