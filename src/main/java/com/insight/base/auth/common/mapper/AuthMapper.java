package com.insight.base.auth.common.mapper;

import com.insight.base.auth.common.dto.FuncDto;
import com.insight.base.auth.common.dto.NavDto;
import com.insight.base.auth.common.entity.TenantApp;
import com.insight.utils.pojo.app.Application;
import com.insight.utils.pojo.app.FuncInfo;
import com.insight.utils.pojo.app.ModuleInfo;
import com.insight.utils.pojo.base.BaseVo;
import com.insight.utils.pojo.base.JsonTypeHandler;
import com.insight.utils.pojo.user.MemberDto;
import com.insight.utils.pojo.user.User;
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
    @Select("select * from ibu_user WHERE account = #{key} or mobile = #{key} or email = #{key} or union_id = #{key} limit 1;")
    User getUser(String key);

    /**
     * 获取用户的租户ID集合
     *
     * @param userId 用户ID
     * @return 租户ID集合
     */
    @Select("select t.* from ibt_tenant t join ibt_tenant_user r on r.tenant_id = t.id where r.user_id = #{userId};")
    List<BaseVo> getTenantIds(Long userId);

    /**
     * 获取指定ID的租户
     *
     * @param id 租户ID
     * @return 租户名称
     */
    @Select("select name from ibt_tenant where id = #{id};")
    String getTenant(Long id);

    /**
     * 查询指定ID的应用信息
     *
     * @param appId 应用ID
     * @return 应用信息
     */
    @Select("select * from ibs_application where id = #{appId};")
    Application getApp(Long appId);

    /**
     * 查询指定应用ID的应用信息
     *
     * @param tenantId 租户ID
     * @param appId    应用ID
     * @return 应用信息
     */
    @Select("select * from ibt_tenant_app where tenant_id = #{tenantId} and app_id = #{appId};")
    TenantApp getApps(Long tenantId, Long appId);

    /**
     * 获取微信OpenID
     *
     * @param userId 用户ID
     * @return 微信OpenID
     */
    @Results({@Result(property = "openId", column = "open_id", javaType = Map.class, typeHandler = JsonTypeHandler.class)})
    @Select("select open_id from ibu_user where id = #{userId};")
    Map<String, Map<String, String>> getOpenId(Long userId);

    /**
     * 记录用户绑定的微信OpenID
     *
     * @param userId 用户ID
     * @param openId 微信OpenID
     */
    @Update("update ibu_user set open_id = #{openId, typeHandler = com.insight.utils.pojo.base.JsonTypeHandler} where id = #{userId};")
    void updateOpenId(Long userId, Map openId);

    /**
     * 更新用户微信UnionID
     *
     * @param userId   用户ID
     * @param unionId  微信UnionID
     * @param nickname 微信昵称
     */
    @Update("update ibu_user set union_id = #{unionId}, nickname = #{nickname} where id = #{userId};")
    void updateUnionId(Long userId, String unionId, String nickname);

    /**
     * 获取用户可用的导航栏
     *
     * @param tenantId 租户ID
     * @param appId    应用程序ID
     * @param userId   用户ID
     * @return Navigation对象集合
     */
    @Results({@Result(property = "moduleInfo", column = "module_info", javaType = ModuleInfo.class, typeHandler = JsonTypeHandler.class)})
    @Select("""
            select * from (
              select distinct g.id, g.parent_id, g.`type`, g.`index`, g.`name`, g.module_info
              from ibs_navigator g
                join ibs_navigator m on m.parent_id = g.id join ibs_function f on f.nav_id = m.id
                join (select distinct a.function_id
                  from ibr_role_permit a
                    join ibv_user_roles r on r.role_id = a.role_id
                  where user_id = #{userId} and (tenant_id is null or tenant_id = #{tenantId})
                  group by a.function_id having min(a.permit)> 0) a on a.function_id = f.id
              where g.app_id = #{appId} union
              select m.id, m.parent_id, m.`type`, m.`index`, m.`name`, m.module_info
              from ibs_navigator m
                join ibs_function f on f.nav_id = m.id
                join (select distinct a.function_id
                  from ibr_role_permit a
                    join ibv_user_roles r on r.role_id = a.role_id
                  where user_id = #{userId} and (tenant_id is null or tenant_id = #{tenantId})
                  group by a.function_id
                  having min(a.permit)> 0) a on a.function_id = f.id
              where m.app_id = #{appId}) t
            order by t.parent_id, t.`index`;
            """)
    List<NavDto> getNavigators(Long appId, Long tenantId, Long userId);

    /**
     * 获取指定模块的全部可用功能集合及对指定用户的授权情况
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param moduleId 模块ID
     * @return Function对象集合
     */
    @Results({@Result(property = "funcInfo", column = "func_info", javaType = FuncInfo.class, typeHandler = JsonTypeHandler.class)})
    @Select("""
            select f.id, f.nav_id, f.`type`, f.`index`, f.`name`, f.auth_codes, f.func_info, a.permit
            from ibs_function f
              left join (select a.function_id, min(a.permit) as permit
                from ibr_role_permit a
                  join ibv_user_roles r on r.role_id = a.role_id and r.user_id = #{userId} and (r.tenant_id is null or r.tenant_id = #{tenantId})
                group by a.function_id) a on a.function_id = f.id
            where f.nav_id = #{moduleId}
            order by f.`index`;
            """)
    List<FuncDto> getModuleFunctions(Long moduleId, Long tenantId, Long userId);

    /**
     * 获取用户授权信息
     *
     * @param appId    应用ID
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 授权信息集合
     */
    @Select("""
            <script>select substring_index(substring_index(f.auth_codes, ',', h.help_topic_id + 1), ',', - 1) as auth_code
            from ibs_function f
              join ibs_navigator n on n.id = f.nav_id and n.app_id = #{appId}
              join ibr_role_permit p on p.function_id = f.id
              join ibv_user_roles r on r.role_id = p.role_id and r.user_id = #{userId}
              <if test = 'tenantId != null'>and r.tenant_id = #{tenantId}</if>
              <if test = 'tenantId == null'>and r.tenant_id is null</if>
              join mysql.help_topic h on h.help_topic_id &lt; (length(f.auth_codes) - length(replace(f.auth_codes, ',', '')) + 1)
            group by n.app_id, f.nav_id, auth_code
            having min(p.permit) > 0;</script>
            """)
    List<String> getAuthInfos(Long appId, Long tenantId, Long userId);

    /**
     * 获取用户可选租户
     *
     * @param appId  应用程序ID
     * @param userId 用户ID
     * @return 用户绑定租户集合
     */
    @Select("""
            select t.id, t.`name` from ibt_tenant t
              join ibt_tenant_app a on a.tenant_id = t.id and a.app_id = #{appId}
              join ibt_tenant_user u on u.tenant_id = t.id and u.user_id = #{userId};
            """)
    List<MemberDto> getTenants(Long appId, Long userId);

    /**
     * 获取用户登录的机构
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 机构DTO
     */
    @Select("""
            with recursive org as (select o.id, o.tenant_id, o.type, o.parent_id, o.code, o.name
              from ibo_organize o
              join ibo_organize_member m on m.post_id = o.id and m.user_id = #{userId}
              where o.tenant_id = #{tenantId} union
              select p.id, p.tenant_id, p.type, p.parent_id, p.code, p.name
              from ibo_organize p
                join org s on s.parent_id = p.id)
            select o.id, t.area_code as code, o.name
            from org o
              join ibt_tenant t on t.id = o.tenant_id
            where o.type = 0
            order by o.code desc limit 1
            """)
    BaseVo getLoginOrg(long userId, long tenantId);
}
