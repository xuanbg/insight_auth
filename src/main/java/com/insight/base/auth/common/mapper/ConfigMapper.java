package com.insight.base.auth.common.mapper;

import com.insight.base.auth.common.dto.ConfigDto;
import com.insight.base.auth.common.dto.ConfigListDto;
import com.insight.base.auth.common.entity.InterfaceConfig;
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
    @Select("<script>select id, name, method, url, auth_code, is_verify, is_limit from ibi_interface" +
            "<if test = 'key!=null'> where " +
            "name like concat('%',#{key},'%') or " +
            "url like concat('%',#{key},'%') or " +
            "auth_code like concat('%',#{key},'%')</if>" +
            "order by created_time desc</script>")
    List<ConfigListDto> getConfigs(@Param("key") String key);

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
    @Insert("INSERT ibi_interface(id, name, method, url, auth_code, limit_gap, limit_cycle, limit_max, message, remark, is_verify, is_limit) VALUES " +
            "(#{id}, #{name}, #{method}, #{url}, #{authCode}, #{limitGap}, #{limitCycle}, #{limitMax}, #{message}, #{remark}, #{isVerify}, #{isLimit});")
    void addConfig(InterfaceConfig config);

    /**
     * 在数据库中写入接口配置
     *
     * @param config 接口配置
     * @return 受影响行数
     */
    @Update("UPDATE ibi_interface SET " +
            "type = #{type}, name = #{name}, method = #{method}, url = #{url}, auth_code = #{authCode}, " +
            "limit_gap = #{limitGap}, limit_cycle = #{limitCycle}, limit_max = #{limitMax}, message = #{message}, " +
            "remark = #{remark}, is_verify = #{isVerify}, is_limit = #{isLimit} WHERE id = #{id};")
    int editConfig(InterfaceConfig config);

    /**
     * 删除接口配置
     *
     * @param id 接口配置ID
     * @return 受影响行数
     */
    @Delete("delete from ibi_interface where id = #{id};")
    int deleteConfig(String id);

    /**
     * 获取接口配置
     *
     * @return 接口配置表
     */
    @Select("select method, url, auth_code, limit_gap, limit_cycle, limit_max, message, is_verify, is_limit from ibi_interface;")
    List<ConfigDto> loadConfigs();
}
