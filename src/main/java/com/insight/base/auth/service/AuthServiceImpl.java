package com.insight.base.auth.service;

import com.insight.base.auth.common.Core;
import com.insight.base.auth.common.dto.*;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.utils.DateTime;
import com.insight.utils.Json;
import com.insight.utils.ReplyHelper;
import com.insight.utils.Util;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.auth.TokenKey;
import com.insight.utils.pojo.base.BusinessException;
import com.insight.utils.pojo.base.Reply;
import com.insight.utils.pojo.user.MemberDto;
import com.insight.utils.pojo.user.User;
import com.insight.utils.pojo.wechat.WechatUser;
import com.insight.utils.redis.HashOps;
import com.insight.utils.redis.KeyOps;
import com.insight.utils.redis.StringOps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 身份验证服务接口实现类
 */
@Service
public class AuthServiceImpl implements AuthService {
    private final AuthMapper mapper;
    private final Core core;

    /**
     * 是否允许自动注册
     */
    @Value("${insight.auth.autoReg}")
    private Boolean autoReg;

    /**
     * 构造函数
     *
     * @param mapper AuthMapper
     * @param core   Core
     */
    public AuthServiceImpl(AuthMapper mapper, Core core) {
        this.mapper = mapper;
        this.core = core;
    }

    /**
     * 获取用户可选租户
     *
     * @param appId   应用ID
     * @param account 登录账号
     * @return Reply
     */
    @Override
    public List<MemberDto> getTenants(Long appId, String account) {
        var userId = core.getUserId(account);
        return mapper.getTenants(appId, userId);
    }

    /**
     * 生成加密Code
     *
     * @param dto CodeDTO
     * @return Reply
     */
    @Override
    public Reply generateCode(CodeDto dto) {
        var account = dto.getAccount();
        var userId = core.getUserId(account);
        if (userId == null) {
            if (dto.getType() == 1 && autoReg) {
                var user = new User();
                user.setName(account);
                user.setAccount(account);
                user.setMobile(account);
                userId = core.addUser(user);
            } else {
                throw new BusinessException("账号或密码错误");
            }
        }

        var key = "User:" + userId;
        if (!KeyOps.hasKey(key)) {
            KeyOps.delete("ID:" + account);
            throw new BusinessException("发生了一点小意外,请重新提交");
        }

        // 生成Code
        if (dto.getType() == 0) {
            var password = HashOps.get(key, "password");
            if (!Util.isNotEmpty(password)) {
                throw new BusinessException("账号或密码错误");
            }

            var code = core.getGeneralCode(userId, account, password);
            return ReplyHelper.created(code);
        } else {
            if (!account.matches("^1[0-9]{10}")) {
                throw new BusinessException("请使用正确的手机号");
            }

            var code = core.getSmsCode(userId, account);
            return ReplyHelper.created(code);
        }
    }

    /**
     * 获取授权码
     *
     * @return Reply
     */
    @Override
    public Reply getAuthCode() {
        var code = Util.uuid();
        StringOps.set("Code:" + code, "", 300L);

        return ReplyHelper.created(code);
    }

    /**
     * 扫码授权
     *
     * @param info 用户登录信息
     * @param code 授权识别码
     */
    @Override
    public void authWithCode(LoginInfo info, String code) {
        if (Util.isEmpty(code)) {
            throw new BusinessException("无效参数code: " + code);
        }

        var key = "Code:" + code;
        if (!KeyOps.hasKey(key)) {
            throw new BusinessException("Code已失效，请重新获取Code");
        }

        var tokenKey = new TokenKey(info.getAppId(), info.getTenantId(), info.getId());
        StringOps.set(key, tokenKey, 30L);
    }

    /**
     * 获取提交数据用临时Token
     *
     * @param key 接口Hash: MD5(userId:method:url)
     * @return Reply
     */
    @Override
    public Reply getSubmitToken(String key) {
        var id = StringOps.get("SubmitToken:" + key);
        if (!Util.isNotEmpty(id)) {
            id = Util.uuid();
            StringOps.set("SubmitToken:" + key, id, 1L, TimeUnit.HOURS);
        }

        return ReplyHelper.created(id);
    }

    /**
     * 生成Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply generateToken(LoginDto login) {
        var code = core.getCode(login.getSignature());
        if (code != null) {
            var token = core.getToken(code, login);
            return ReplyHelper.created(token);
        }

        // 处理错误
        var account = login.getAccount();
        var userId = core.getUserId(account);
        var key = "User:" + userId;
        if (!KeyOps.hasKey(key)) {
            KeyOps.delete("ID:" + account);
            throw new BusinessException("发生了一点小意外,请重新提交");
        }

        var failureCount = core.getFailureCount(userId);
        if (failureCount > 5) {
            throw new BusinessException("错误次数过多,账号已被锁定!请于10分钟后再试");
        }

        HashOps.put(key, "FailureCount", failureCount + 1);
        HashOps.put(key, "LastFailureTime", DateTime.formatCurrentTime());

        throw new BusinessException("账号或密码错误");
    }

    /**
     * 通过微信授权码获取Token,如用户不存在,缓存微信用户信息
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithWeChat(LoginDto login) {
        var code = login.getCode();
        var weChatAppId = login.getWeChatAppId();

        var weChatUser = core.getWeChatInfo(code, weChatAppId);
        if (weChatUser == null) {
            throw new BusinessException("微信授权失败");
        }

        var unionId = weChatUser.getUnionid();
        if (unionId == null || unionId.isEmpty()) {
            throw new BusinessException("未取得微信用户的UnionID");
        }

        // 使用微信UnionID读取缓存,如用户不存在,则缓存微信用户信息(30分钟)后返回微信用户信息
        var userId = core.getUserId(unionId);
        if (userId == null) {
            throw new BusinessException(425, "微信账号未绑定到用户");
        }

        var user = core.getUser(userId);
        core.bindOpenId(userId, weChatUser.getOpenid(), weChatAppId);

        var key = "User:" + userId;
        HashOps.put(key, "nickname", weChatUser.getNickname());
        if (Util.isEmpty(user.getHeadImg())) {
            HashOps.put(key, "headImg", weChatUser.getHeadimgurl());
        }

        if (login.getTenantId() == null) {
            var tenants = mapper.getTenantIds(userId);
            if (tenants.size() == 1) {
                login.setTenantId(tenants.get(0).getId());
            } else if (tenants.size() > 1) {
                key = "Wechat:" + unionId;
                StringOps.set(key, Json.toJson(weChatUser), 30L, TimeUnit.MINUTES);

                var dto = new TenantDto();
                dto.setUnionId(unionId);
                dto.setTenants(tenants);
                return ReplyHelper.isMultiTenant(dto);
            }
        }

        var token = core.creatorToken(login, userId);
        return ReplyHelper.created(token);
    }

    /**
     * 通过微信UnionId获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithUnionId(LoginDto login) {
        var unionId = login.getUnionId();
        var json = StringOps.get("Wechat:" + unionId);
        var weChatUser = Json.toBean(json, WechatUser.class);
        if (weChatUser == null) {
            throw new BusinessException("微信授权已过期，请重新登录");
        }

        var userId = core.getUserId(unionId);
        var user = core.getUser(userId);
        var key = "User:" + userId;
        HashOps.put(key, "nickname", weChatUser.getNickname());
        if (Util.isEmpty(user.getHeadImg())) {
            HashOps.put(key, "headImg", weChatUser.getHeadimgurl());
        }

        var token = core.creatorToken(login, userId);
        return ReplyHelper.created(token);
    }

    /**
     * 通过微信UnionId获取Token,如用户不存在,则自动创建用户
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithUserInfo(LoginDto login) {
        // 验证账号绑定的手机号
        var mobile = login.getAccount();
        var verifyKey = Util.md5(0 + mobile + login.getCode());
        core.verifySmsCode(verifyKey);

        // 从缓存读取微信用户信息
        var unionId = login.getUnionId();
        var key = "Wechat:" + unionId;
        var json = StringOps.get(key);
        var weChatUser = Json.toBean(json, WechatUser.class);
        if (weChatUser == null) {
            throw new BusinessException("微信授权已过期，请重新登录");
        }

        // 根据手机号获取用户
        var userId = core.getUserId(mobile);
        var user = new User();
        if (userId != null) {
            user = core.getUser(userId);
            var uid = user.getUnionId();
            if (Util.isNotEmpty(uid)) {
                if (login.getReplace()) {
                    KeyOps.delete("ID:" + uid);
                } else {
                    throw new BusinessException("手机号 " + mobile + " 的用户已绑定其他微信号，请使用正确的微信号登录");
                }
            }

            // 更新用户微信UnionID
            core.updateUnionId(userId, unionId, weChatUser.getNickname());
        } else if (autoReg) {
            // 用户不存在,自动创建用户
            user.setName(weChatUser.getNickname());
            user.setAccount(mobile);
            user.setMobile(mobile);
            user.setUnionId(unionId);
            user.setHeadImg(weChatUser.getHeadimgurl());
            core.addUser(user);
        } else {
            throw new BusinessException("用户不存在,请联系管理员创建用户");
        }

        core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());

        key = "User:" + userId;
        HashOps.put(key, "nickname", weChatUser.getNickname());
        if (Util.isEmpty(user.getHeadImg())) {
            HashOps.put(key, "headImg", weChatUser.getHeadimgurl());
        }

        var token = core.creatorToken(login, userId);
        return ReplyHelper.created(token);
    }

    /**
     * APP扫码授权获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithCode(LoginDto login) {
        var key = "Code:" + login.getCode();
        var tokenKey = StringOps.get(key, TokenKey.class);
        if (tokenKey == null) {
            throw new BusinessException(427, "请等待APP扫码");
        }

        var data = core.getToken(tokenKey);
        login.setTenantId(data.getTenantId());

        var token = core.creatorToken(login, data.getUserId());
        return ReplyHelper.created(token);
    }

    /**
     * 获取指定应用的Token
     *
     * @param appId       应用ID
     * @param fingerprint 用户特征串
     * @param deviceId    设备ID
     * @param key         令牌
     * @return Reply
     */
    @Override
    public Reply getToken(Long appId, String fingerprint, String deviceId, TokenKey key) {
        if (KeyOps.hasKey(key.getKey())) {
            var token = core.getToken(key);
            if (!token.verify(key.getSecret())) {
                throw new BusinessException(421, "非法的Token");
            }

            key.setAppId(appId);
            var dto = core.creatorToken(key, fingerprint, deviceId);
            return ReplyHelper.created(dto);
        } else {
            throw new BusinessException(421, "非法的Token");
        }
    }

    /**
     * 用户账号离线
     *
     * @param key 用户关键信息
     */
    @Override
    public void deleteToken(TokenKey key) {
        core.deleteToken(key);
    }

    /**
     * 获取用户授权码
     *
     * @param key 用户关键信息
     * @return 授权码集合
     */
    @Override
    public List<String> getPermits(TokenKey key) {
        return mapper.getAuthInfos(key);
    }

    /**
     * 获取用户导航栏
     *
     * @param key 用户关键信息
     * @return 导航数据集合
     */
    @Override
    public List<NavDto> getNavigators(TokenKey key) {
        var list = mapper.getNavigators(key);
        for (var nav : list) {
            if (nav.getType() > 1) {
                var funs = mapper.getModuleFunctions(nav.getId(), key.getTenantId(), key.getUserId());
                nav.setFunctions(funs);
            }
        }

        return list;
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param moduleId 功能模块ID
     * @return 功能及权限数据集合
     */
    @Override
    public List<FuncDto> getModuleFunctions(Long tenantId, Long userId, Long moduleId) {
        return mapper.getModuleFunctions(moduleId, tenantId, userId);
    }
}
