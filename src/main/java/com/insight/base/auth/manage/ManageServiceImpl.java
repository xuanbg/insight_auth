package com.insight.base.auth.manage;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.insight.base.auth.common.dto.ConfigDto;
import com.insight.base.auth.common.dto.ConfigListDto;
import com.insight.base.auth.common.entity.InterfaceConfig;
import com.insight.base.auth.common.mapper.ConfigMapper;
import com.insight.util.Generator;
import com.insight.util.Json;
import com.insight.util.Redis;
import com.insight.util.ReplyHelper;
import com.insight.util.pojo.Log;
import com.insight.util.pojo.LoginInfo;
import com.insight.util.pojo.OperateType;
import com.insight.util.pojo.Reply;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 宣炳刚
 * @date 2019-09-02
 * @remark 配置管理服务
 */
@Service
public class ManageServiceImpl implements ManageService {
    private final ConfigMapper mapper;

    /**
     * 构造函数
     *
     * @param mapper ConfigMapper
     */
    public ManageServiceImpl(ConfigMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 获取接口配置列表
     *
     * @param key  查询关键词
     * @param page 分页页码
     * @param size 每页记录数
     * @return Reply
     */
    @Override
    public Reply getConfigs(String key, int page, int size) {
        PageHelper.startPage(page, size);
        List<ConfigListDto> configs = mapper.getConfigs(key);
        PageInfo<ConfigListDto> pageInfo = new PageInfo<>(configs);

        return ReplyHelper.success(configs, pageInfo.getTotal());
    }

    /**
     * 获取接口配置详情
     *
     * @param id 接口配置ID
     * @return Reply
     */
    @Override
    public Reply getConfig(String id) {
        InterfaceConfig config = mapper.getConfig(id);

        return ReplyHelper.success(config);
    }

    /**
     * 新增接口配置
     *
     * @param info 用户关键信息
     * @param dto  接口配置
     * @return Reply
     */
    @Override
    public Reply newConfig(LoginInfo info, InterfaceConfig dto) {
        String id = Generator.uuid();
        dto.setId(id);
        dto.setCreatedTime(new Date());
        mapper.addConfig(dto);
        writeLog(info, OperateType.INSERT, id, dto);

        Reply reply = loadConfigs();
        if (reply.getSuccess()) {
            return ReplyHelper.created(id);
        }

        reply.setData(id);
        return reply;
    }

    /**
     * 编辑接口配置
     *
     * @param info 用户关键信息
     * @param dto  接口配置DTO
     * @return Reply
     */
    @Override
    public Reply editConfig(LoginInfo info, InterfaceConfig dto) {
        String id = dto.getId();
        InterfaceConfig config = mapper.getConfig(id);
        if (config == null) {
            return ReplyHelper.fail("ID不存在,未更新数据");
        }

        writeLog(info, OperateType.UPDATE, id, dto);
        mapper.editConfig(dto);

        return loadConfigs();
    }

    /**
     * 删除接口配置
     *
     * @param info 用户关键信息
     * @param id   接口配置ID
     * @return Reply
     */
    @Override
    public Reply deleteConfig(LoginInfo info, String id) {
        InterfaceConfig config = mapper.getConfig(id);
        if (config == null) {
            return ReplyHelper.fail("ID不存在,未删除数据");
        }

        writeLog(info, OperateType.DELETE, id, config);
        mapper.deleteConfig(id);

        return loadConfigs();
    }

    /**
     * 获取日志列表
     *
     * @param key  查询关键词
     * @param page 分页页码
     * @param size 每页记录数
     * @return Reply
     */
    @Override
    public Reply getLogs(String key, int page, int size) {
        PageHelper.startPage(page, size);
        List<Log> logs = mapper.getLogs(key);
        PageInfo<Log> pageInfo = new PageInfo<>(logs);

        return ReplyHelper.success(logs, pageInfo.getTotal());
    }

    /**
     * 获取日志详情
     *
     * @param id 日志ID
     * @return Reply
     */
    @Override
    public Reply getLog(String id) {
        Log log = mapper.getLog(id);

        return ReplyHelper.success(log);
    }

    /**
     * 加载接口配置到缓存
     *
     * @return Reply
     */
    @Override
    public Reply loadConfigs() {
        List<ConfigDto> configs = mapper.loadConfigs();
        if (configs == null || configs.isEmpty()) {
            return ReplyHelper.fail("读取数据失败,请重新加载");
        }

        String json = Json.toJson(configs);
        Redis.set("Config:Interface", json);

        return ReplyHelper.success();
    }

    /**
     * 记录操作日志
     *
     * @param info    用户关键信息
     * @param type    操作类型
     * @param id      业务ID
     * @param content 日志内容
     */
    private void writeLog(LoginInfo info, OperateType type, String id, Object content) {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        threadPool.submit(() -> {
            Log log = new Log();
            log.setId(Generator.uuid());
            log.setType(type);
            log.setBusiness("接口配置管理");
            log.setBusinessId(id);
            log.setContent(content);
            log.setDeptId(info.getDeptId());
            log.setCreator(info.getUserName());
            log.setCreatorId(info.getUserId());
            log.setCreatedTime(new Date());

            mapper.addLog(log);
        });
    }
}
