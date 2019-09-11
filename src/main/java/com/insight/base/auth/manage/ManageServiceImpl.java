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
import com.insight.util.pojo.Reply;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * @param dto 接口配置
     * @return Reply
     */
    @Override
    public Reply newConfig(InterfaceConfig dto) {
        String id = Generator.uuid();
        dto.setId(id);
        mapper.addConfig(dto);

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
     * @param dto 接口配置DTO
     * @return Reply
     */
    @Override
    public Reply editConfig(InterfaceConfig dto) {
        int count = mapper.editConfig(dto);

        return count == 0 ? ReplyHelper.fail("ID不存在,未更新数据") : loadConfigs();
    }

    /**
     * 删除接口配置
     *
     * @param id 接口配置ID
     * @return Reply
     */
    @Override
    public Reply deleteConfig(String id) {
        int count = mapper.deleteConfig(id);

        return count == 0 ? ReplyHelper.fail("ID不存在,未删除数据") : loadConfigs();
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
}
