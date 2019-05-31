package com.insight.base.auth.common.mapper;

import com.insight.base.auth.common.dto.AuthInfo;
import com.insight.base.auth.common.dto.FuncDTO;
import com.insight.base.auth.common.dto.NavDTO;
import com.insight.base.auth.common.entity.ModuleInfo;
import com.insight.util.common.JsonTypeHandler;
import com.insight.util.pojo.Application;
import com.insight.util.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.function.Function;

/**
 * @author 宣炳刚
 * @date 2017/9/13
 * @remark 权限相关DAL
 */
@Mapper
public interface AuthMapper extends Mapper {

    /**
     * 根据登录账号查询用户数据
     *
     * @param key 关键词(ID/账号/手机号/E-mail/微信unionId)
     * @return 用户实体
     */
    @Results({@Result(property = "builtin", column = "is_builtin"),
            @Result(property = "invalid", column = "is_invalid")})
    @Select("select * from ibu_user WHERE id =#{key} or account=#{key} or mobile=#{key} or email=#{key} or union_id=#{key} limit 1;")
    User getUser(String key);

    /**
     * 根据ID获取租户数量
     *
     * @param id 租户ID
     * @return 符合条件的租户数量
     */
    @Select("select count(*) from ibt_tenant where id =#{id}")
    Integer getTenantCount(String id);

    /**
     * 查询指定ID的应用信息
     *
     * @param appId 应用ID
     * @return 应用信息
     */
    @Results({@Result(property = "signinOne", column = "is_signin_one"),
            @Result(property = "autoRefresh", column = "is_auto_refresh")})
    @Select("SELECT * FROM ibs_application WHERE id=#{appId};")
    Application getApp(String appId);

    /**
     * 查询指定ID的应用的令牌生命周期小时数
     *
     * @param appId 应用ID
     * @return 应用的令牌生命周期小时数
     */
    @Select("SELECT token_life FROM ibs_application WHERE id=#{appId};")
    Long getTokenLife(String appId);

    /**
     * 获取用户全部可用功能集合
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param deptId   登录部门ID
     * @return Function对象集合
     */
    @Select("SELECT f.id,f.auth_code,ifnull(f.interfaces,'') AS interfaces FROM ibs_function f JOIN ibr_role_func_permit a ON a.function_id=f.id " +
            "JOIN (SELECT DISTINCT role_id FROM ibv_user_roles WHERE user_id=#{userId} AND tenant_id=#{tenantId} " +
            "AND (dept_id=#{deptId} OR dept_id IS NULL)) r ON r.role_id=a.role_id GROUP BY f.id HAVING min(a.permit)> 0;")
    List<Function> getAllFunctions(@Param("tenantId") String tenantId, @Param("userId") String userId, @Param("deptId") String deptId);

    /**
     * 获取用户可用的导航栏
     *
     * @param tenantId 租户ID
     * @param appId    应用程序ID
     * @param userId   用户ID
     * @param deptId   登录部门ID
     * @return Navigation对象集合
     */
    @Results({@Result(property = "module_info", column = "module_info", javaType = ModuleInfo.class, typeHandler = JsonTypeHandler.class)})
    @Select("SELECT * FROM (SELECT DISTINCT g.id,g.parent_id,g.`type`,g.`index`,g.`name`,g.module_info FROM ibs_navigator g " +
            "JOIN ibs_navigator m ON m.parent_id=g.id JOIN ibs_function f ON f.nav_id=m.id " +
            "JOIN (SELECT DISTINCT a.function_id FROM ibr_role_func_permit a JOIN ibv_user_roles r ON r.role_id=a.role_id " +
            "WHERE user_id=#{userId} AND tenant_id=#{tenantId} AND (dept_id=#{deptId} OR dept_id IS NULL) " +
            "GROUP BY a.function_id HAVING min(a.permit)> 0) a ON a.function_id=f.id WHERE g.app_id=#{appId} UNION " +
            "SELECT m.id,m.parent_id,m.`type`,m.`index`,m.`name`,m.module_info FROM ibs_navigator m JOIN ibs_function f ON f.nav_id=m.id " +
            "JOIN (SELECT DISTINCT a.function_id FROM ibr_role_func_permit a JOIN ibv_user_roles r ON r.role_id=a.role_id " +
            "WHERE user_id=#{userId} AND tenant_id=#{tenantId} AND (dept_id=#{deptId} OR dept_id IS NULL) GROUP BY a.function_id " +
            "HAVING min(a.permit)> 0) a ON a.function_id=f.id WHERE m.app_id=#{appId}) l ORDER BY l.parent_id,l.`index`;")
    List<NavDTO> getNavigators(@Param("tenantId") String tenantId, @Param("appId") String appId, @Param("userId") String userId, @Param("deptId") String deptId);

    /**
     * 获取指定模块的全部可用功能集合及对指定用户的授权情况
     *
     * @param tenantId 租户ID
     * @param moduleId 模块ID
     * @param userId   用户ID
     * @param deptId   登录部门ID
     * @return Function对象集合
     */
    @Select("SELECT f.id,f.nav_id,f.`type`,f.code,f.`index`,f.`name`,f.icon,f.url,a.permit,f.begin_group,f.hide_text FROM ucs_function f " +
            "LEFT JOIN (SELECT a.function_id,min(a.permit) AS permit FROM ucr_role_func_permit a JOIN ucv_user_roles r " +
            "ON r.role_id=a.role_id AND r.user_id=#{userId} AND r.tenant_id=#{tenantId} AND (r.dept_id=#{deptId} OR r.dept_id IS NULL) " +
            "GROUP BY a.function_id) a ON a.function_id=f.id WHERE f.nav_id=#{moduleId} AND f.is_invisible=0 ORDER BY f.`index`;")
    List<Function> getModuleFunctions(@Param("tenantId") String tenantId, @Param("moduleId") String moduleId, @Param("userId") String userId, @Param("deptId") String deptId);

    /**
     * 获取指定模块的全部可用功能集合及对指定用户的授权情况
     *
     * @param tenantId  租户ID
     * @param moduleIds 模块ID集合
     * @param userId    用户ID
     * @param deptId    登录部门ID
     * @return Function对象集合
     */
    @Select("<script>SELECT f.id,f.nav_id,f.`type`,f.code,f.`index`,f.`name`,f.icon,f.url,a.permit,f.begin_group,f.hide_text FROM ucs_function f " +
            "LEFT JOIN (SELECT a.function_id,min(a.permit) AS permit FROM ucr_role_func_permit a JOIN ucv_user_roles r " +
            "ON r.role_id=a.role_id AND r.user_id=#{userId} AND r.tenant_id=#{tenantId} AND (r.dept_id=#{deptId} OR r.dept_id IS NULL) " +
            "GROUP BY a.function_id) a ON a.function_id=f.id WHERE f.nav_id in " +
            "<foreach collection = \"list\" item = \"item\" index = \"index\" open=\"(\" close=\")\" separator = \",\"> " +
            "#{item}</foreach> " +
            "AND f.is_invisible=0 ORDER BY f.`index`;</script>")
    List<FuncDTO> getAllModuleFunctions(@Param("tenantId") String tenantId, @Param("list") List<String> moduleIds, @Param("userId") String userId, @Param("deptId") String deptId);

    /**
     * 查询指定账号、手机号、E-mail的用户数量
     *
     * @param account 登录账号
     * @param mobile  手机号
     * @param unionId 微信unionId
     * @param email   E-mail
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM ucb_user WHERE account=#{account} OR mobile=#{mobile} OR union_id=#{unionId} OR email=#{email}  ;")
    Integer getExistedUserCount(@Param("account") String account, @Param("mobile") String mobile, @Param("unionId") String unionId, @Param("email") String email);

    /**
     * 根据ID查询用户数据
     *
     * @param userId 用户ID
     * @param appId  微信AppID
     * @return 用户实体
     */
    @Results({@Result(property = "builtin", column = "is_builtin"),
            @Result(property = "invalid", column = "is_invalid")})
    @Select("SELECT u.id,u.user_type,u.`code`,u.`name`,u.account,u.mobile,o.id AS open_id,u.email,u.head_img,u.setting,u.remark,u.is_builtin,u.is_invalid,u.created_time " +
            "FROM ucb_user u LEFT JOIN (SELECT id,user_id,max(created_time) FROM ucb_user_openid " +
            "WHERE app_id=#{appId} GROUP BY user_id) o ON o.user_id=u.id WHERE u.id=#{userId};")
    User getUserWithAppId(@Param("userId") String userId, @Param("appId") String appId);

    /**
     * 查询指定租户是否绑定了指定的应用
     *
     * @param tenantId 租户ID
     * @param appId    应用ID
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM ucb_tenant_app WHERE tenant_id=#{tenantId} AND app_id=#{appId};")
    Integer containsApp(@Param("tenantId") String tenantId, @Param("appId") String appId);

    /**
     * 根据角色id查询功能集合
     *
     * @param roleId 角色id
     * @return 功能集合
     */
    @Select("SELECT f.`id`,f.`alias`,f.`interfaces`,r.`permit` FROM ucr_role_func_permit r INNER JOIN ucs_function f ON r.`function_id`=f.`id` WHERE r.role_id=#{roleId} ")
    List<Function> getRoleFunctionByRoleIds(String roleId);

    /**
     * 查询模块功能集合
     *
     * @param list 模块功能id集合
     * @return 受影响行数
     */
    @Select("<script>select * FROM ucs_function WHERE id in " +
            "<foreach collection = \"list\" item = \"item\" index = \"index\" open=\"(\" close=\")\" separator = \",\"> " +
            "#{item} " +
            "</foreach>;</script>")
    List<Function> getFunctions(List<String> list);

    /**
     * 获取用户授权信息
     *
     * @param appId    应用ID
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param deptId   登录部门ID
     * @return 授权信息集合
     */
    @Select("SELECT f.id,f.nav_id,f.alias,f.interfaces,min(a.permit) permit " +
            "FROM ucs_function f JOIN ucs_navigator n ON n.id=f.nav_id AND n.app_id=#{appId} " +
            "JOIN ucr_role_func_permit a ON a.function_id=f.id JOIN ucv_user_roles r ON r.role_id=a.role_id " +
            "AND r.tenant_id=#{tenantId} AND r.user_id=#{userId} AND (r.dept_id IS NULL || r.dept_id=#{deptId}) " +
            "WHERE f.alias IS NOT NULL OR f.interfaces IS NOT NULL " +
            "GROUP BY f.id,f.nav_id,f.alias,f.interfaces")
    List<AuthInfo> getAuthInfos(@Param("appId") String appId, @Param("userId") String userId, @Param("tenantId") String tenantId, @Param("deptId") String deptId);
}
