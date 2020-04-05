package com.insight.base.auth.common.mapper;

import com.insight.base.auth.common.entity.InterfaceConfig;
import com.insight.utils.common.JsonTypeHandler;
import com.insight.utils.pojo.InterfaceDto;
import com.insight.utils.pojo.Log;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author 宣炳刚
 * @date 2019-09-02
 * @remark 配置管理DAL
 */
@Mapper
public interface ConfigMapper {

    /**
     * 获取接口配置
     *
     * @param key 查询关键词
     * @return 接口配置表
     */
    @Select("<script>select * from ibi_interface " +
            "<if test = 'key!=null'>where name like concat('%',#{key},'%') or url like concat('%',#{key},'%') or auth_code = #{key}</if>" +
            "order by created_time</script>")
    List<InterfaceConfig> getConfigs(@Param("key") String key);

    /**
     * 获取接口配置详情
     *
     * @param id 接口配置ID
     * @return 接口配置详情
     */
    @Select("select * from ibi_interface where id = #{id};")
    InterfaceConfig getConfig(String id);

    /**
     * 在数据库中写入接口配置
     *
     * @param config 接口配置
     */
    @Insert("insert ibi_interface(id, name, method, url, auth_code, limit_gap, limit_cycle, limit_max, message, remark, need_token, is_verify, is_limit, is_log_result, created_time) values " +
            "(#{id}, #{name}, #{method}, #{url}, #{authCode}, #{limitGap}, #{limitCycle}, #{limitMax}, #{message}, #{remark}, #{needToken}, #{isVerify}, #{isLimit}, #{isLogResult}, #{createdTime});")
    void addConfig(InterfaceConfig config);

    /**
     * 在数据库中写入接口配置
     *
     * @param config 接口配置
     */
    @Update("update ibi_interface set name = #{name}, method = #{method}, url = #{url}, auth_code = #{authCode}, " +
            "limit_gap = #{limitGap}, limit_cycle = #{limitCycle}, limit_max = #{limitMax}, message = #{message}, " +
            "remark = #{remark}, need_token = #{needToken}, is_verify = #{isVerify}, is_limit = #{isLimit}, is_log_result = #{isLogResult} where id = #{id};")
    void editConfig(InterfaceConfig config);

    /**
     * 删除接口配置
     *
     * @param id 接口配置ID
     */
    @Delete("delete from ibi_interface where id = #{id};")
    void deleteConfig(String id);

    /**
     * 获取接口配置
     *
     * @return 接口配置表
     */
    @Select("select method, url, auth_code, limit_gap, limit_cycle, limit_max, message, need_token, is_verify, is_limit, is_log_result from ibi_interface;")
    List<InterfaceDto> loadConfigs();

    /**
     * 获取操作日志列表
     *
     * @param tenantId 租户ID
     * @param key      查询关键词
     * @return 操作日志列表
     */
    @Select("<script>select id, type, business, business_id, creator, creator_id, created_time from ibl_operate_log " +
            "where (tenant_id = #{tenantId} or tenant_id is null) " +
            "<if test = 'key!=null'>and type = #{key} or business = #{key} or business_id = #{key} or creator = #{key} or creator_id = #{key}</if>" +
            "order by created_time</script>")
    List<Log> getLogs(@Param("tenantId") String tenantId, @Param("key") String key);

    /**
     * 获取操作日志列表
     *
     * @param id 日志ID
     * @return 操作日志列表
     */
    @Results({@Result(property = "content", column = "content", javaType = Object.class, typeHandler = JsonTypeHandler.class)})
    @Select("select * from ibl_operate_log where id = #{id};")
    Log getLog(String id);

    /**
     * 记录操作日志
     *
     * @param log 日志DTO
     */
    @Insert("insert ibl_operate_log(id, tenant_id, type, business, business_id, content, creator, creator_id, created_time) values " +
            "(#{id}, #{tenantId}, #{type}, #{business}, #{businessId}, #{content, typeHandler = com.insight.utils.common.JsonTypeHandler}, " +
            "#{creator}, #{creatorId}, #{createdTime});")
    void addLog(Log log);
}
