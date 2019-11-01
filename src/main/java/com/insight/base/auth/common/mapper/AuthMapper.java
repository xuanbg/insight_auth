package com.insight.base.auth.common.mapper;

import com.insight.base.auth.common.dto.*;
import com.insight.util.common.JsonTypeHandler;
import com.insight.util.pojo.Application;
import com.insight.util.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * @author 宣炳刚
 * @date 2017/9/13
 * @remark 权限相关DAL
 */
@Mapper
public interface AuthMapper {

    /**
     * 根据登录账号查询用户数据
     *
     * @param key 关键词(ID/账号/手机号/E-mail/微信unionId)
     * @return 用户实体
     */
    @Select("select * from ibu_user WHERE id =#{key} or account=#{key} or mobile=#{key} or email=#{key} or union_id=#{key} limit 1;")
    User getUser(String key);

    /**
     * 查询指定ID的应用信息
     *
     * @param appId 应用ID
     * @return 应用信息
     */
    @Select("SELECT * FROM ibs_application WHERE id=#{appId};")
    Application getApp(String appId);

    /**
     * 获取微信OpenID
     *
     * @param userId 用户ID
     * @return 微信OpenID
     */
    @Results({@Result(property = "openId", column = "open_id", javaType = Map.class, typeHandler = JsonTypeHandler.class)})
    @Select("select open_id from ibu_user where id = #{userId};")
    Map<String, Map<String, String>> getOpenId(String userId);

    /**
     * 记录用户绑定的微信OpenID
     *
     * @param userId 用户ID
     * @param openId 微信OpenID
     */
    @Update("update ibu_user set open_id = #{openId, typeHandler = com.insight.util.common.JsonTypeHandler} where id = #{userId};")
    void updateOpenId(@Param("userId") String userId, @Param("openId") Map openId);

    /**
     * 更新用户微信UnionID
     *
     * @param userId  用户ID
     * @param unionId 微信UnionID
     */
    @Update("update ibu_user set union_id = #{unionId} where id = #{userId};")
    void updateUnionId(@Param("userId") String userId, @Param("unionId") String unionId);

    /**
     * 获取用户可用的导航栏
     *
     * @param tenantId 租户ID
     * @param appId    应用程序ID
     * @param userId   用户ID
     * @param deptId   登录部门ID
     * @return Navigation对象集合
     */
    @Results({@Result(property = "moduleInfo", column = "module_info", javaType = ModuleInfo.class, typeHandler = JsonTypeHandler.class)})
    @Select("SELECT * FROM (SELECT DISTINCT g.id,g.parent_id,g.`type`,g.`index`,g.`name`,g.module_info FROM ibs_navigator g " +
            "JOIN ibs_navigator m ON m.parent_id=g.id JOIN ibs_function f ON f.nav_id=m.id " +
            "JOIN (SELECT DISTINCT a.function_id FROM ibr_role_func_permit a JOIN ibv_user_roles r ON r.role_id=a.role_id " +
            "WHERE user_id=#{userId} AND (tenant_id=#{tenantId} or tenant_id is null) AND (dept_id=#{deptId} OR dept_id IS NULL) " +
            "GROUP BY a.function_id HAVING min(a.permit)> 0) a ON a.function_id=f.id WHERE g.app_id=#{appId} UNION " +
            "SELECT m.id,m.parent_id,m.`type`,m.`index`,m.`name`,m.module_info FROM ibs_navigator m JOIN ibs_function f ON f.nav_id=m.id " +
            "JOIN (SELECT DISTINCT a.function_id FROM ibr_role_func_permit a JOIN ibv_user_roles r ON r.role_id=a.role_id " +
            "WHERE user_id=#{userId} AND (tenant_id=#{tenantId} or tenant_id is null) AND (dept_id=#{deptId} OR dept_id IS NULL) GROUP BY a.function_id " +
            "HAVING min(a.permit)> 0) a ON a.function_id=f.id WHERE m.app_id=#{appId}) l ORDER BY l.parent_id,l.`index`;")
    List<NavDto> getNavigators(@Param("tenantId") String tenantId, @Param("appId") String appId, @Param("userId") String userId, @Param("deptId") String deptId);

    /**
     * 获取指定模块的全部可用功能集合及对指定用户的授权情况
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param deptId   登录部门ID
     * @param moduleId 模块ID
     * @return Function对象集合
     */
    @Results({@Result(property = "iconInfo", column = "icon_info", javaType = IconInfo.class, typeHandler = JsonTypeHandler.class)})
    @Select("SELECT f.id,f.nav_id,f.`type`,f.`index`,f.`name`,f.auth_code,f.icon_info,a.permit FROM ibs_function f " +
            "LEFT JOIN (SELECT a.function_id,min(a.permit) AS permit FROM ibr_role_func_permit a JOIN ibv_user_roles r " +
            "ON r.role_id=a.role_id AND r.user_id=#{userId} AND r.tenant_id=#{tenantId} AND (r.dept_id=#{deptId} OR r.dept_id IS NULL) " +
            "GROUP BY a.function_id) a ON a.function_id=f.id WHERE f.nav_id = #{moduleId} ORDER BY f.`index`;")
    List<FuncDto> getModuleFunctions(@Param("tenantId") String tenantId, @Param("userId") String userId, @Param("deptId") String deptId, @Param("moduleId") String moduleId);

    /**
     * 获取用户授权信息
     *
     * @param appId    应用ID
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param deptId   登录部门ID
     * @return 授权信息集合
     */
    @Select("SELECT f.id,f.nav_id,f.auth_code,min(a.permit) permit " +
            "FROM ibs_function f JOIN ibs_navigator n ON n.id=f.nav_id AND n.app_id=#{appId} " +
            "JOIN ibr_role_func_permit a ON a.function_id=f.id JOIN ibv_user_roles r ON r.role_id=a.role_id " +
            "AND (r.tenant_id=#{tenantId} or r.tenant_id is null) AND r.user_id=#{userId} AND (r.dept_id IS NULL || r.dept_id=#{deptId}) " +
            "WHERE f.auth_code IS NOT NULL GROUP BY f.id,f.nav_id,f.auth_code")
    List<AuthDto> getAuthInfos(@Param("appId") String appId, @Param("userId") String userId, @Param("tenantId") String tenantId, @Param("deptId") String deptId);
}
