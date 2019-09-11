package com.insight.base.auth.common;

import com.insight.base.auth.common.dto.ConfigDto;
import com.insight.base.auth.common.mapper.ConfigMapper;
import com.insight.util.Json;
import com.insight.util.Redis;
import com.insight.util.common.ApplicationContextHolder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

/**
 * @author 宣炳刚
 * @date 2019-09-11
 * @remark 初始化数据加载器
 */
public class TaskRunner implements ApplicationRunner {
    private final ConfigMapper mapper = ApplicationContextHolder.getContext().getBean(ConfigMapper.class);

    @Override
    public void run(ApplicationArguments args) {
        List<ConfigDto> configs = mapper.loadConfigs();
        if (configs == null || configs.isEmpty()) {
            return;
        }

        String json = Json.toJson(configs);
        Redis.set("Config:Interface", json);
    }
}
