package com.insight;

import com.insight.base.auth.common.TaskRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * @author 宣炳刚
 * @date 2017/9/15
 * @remark 应用入口程序
 */
@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
public class AuthApplication {

    /**
     * 应用入口方法
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    /**
     * 加载初始化数据
     *
     * @return TaskRunner
     */
    @Bean
    public TaskRunner taskRunner() {
        return new TaskRunner();
    }
}
