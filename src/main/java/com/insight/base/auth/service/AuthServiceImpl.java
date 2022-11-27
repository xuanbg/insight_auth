package com.insight.base.auth.service;

import com.insight.base.auth.common.Core;
import com.insight.base.auth.common.Token;
import com.insight.base.auth.common.dto.FuncDto;
import com.insight.base.auth.common.dto.LoginDto;
import com.insight.base.auth.common.dto.NavDto;
import com.insight.base.auth.common.dto.TokenDto;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.utils.*;
import com.insight.utils.pojo.auth.AccessToken;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.base.Reply;
import com.insight.utils.pojo.user.MemberDto;
import com.insight.utils.pojo.user.User;
import com.insight.utils.wechat.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final AuthMapper mapper;
    private final Core core;

    /**
     * 是否允许自动注册
     */
    @Value("${insight.auth.autoReg}")
    private Boolean autoReg;

    /**
     * 扫码授权URL
     */
    @Value("${insight.auth.url}")
    private String authUrl;

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
     * 获取提交数据用临时Token
     *
     * @param key 接口Hash: MD5(userId:method:url)
     * @return Reply
     */
    @Override
    public Reply getSubmitToken(String key) {
        String id = Redis.get("SubmitToken:" + key);
        if (!Util.isNotEmpty(id)) {
            id = Util.uuid();
            Redis.set("SubmitToken:" + key, id, 1L, TimeUnit.HOURS);
        }

        return ReplyHelper.success(id);
    }

    /**
     * 获取Code
     *
     * @param account 用户登录账号
     * @param type    登录类型(0:密码登录、1:验证码登录,如用户不存在,则自动创建用户)
     * @return Reply
     */
    @Override
    public Reply getCode(String account, int type) {
        Long userId = core.getUserId(account);
        if (userId == null) {
            if (type == 1 && autoReg) {
                User user = new User();
                user.setName(account);
                user.setAccount(account);
                user.setMobile(account);
                userId = core.addUser(user);
            } else {
                return ReplyHelper.notExist("账号或密码错误");
            }
        }

        String key = "User:" + userId;
        if (!Redis.hasKey(key)) {
            Redis.deleteKey("ID:" + account);
            return ReplyHelper.fail("发生了一点小意外,请重新提交");
        }

        // 生成Code
        String code;
        if (type == 0) {
            String password = Redis.get(key, "password");
            if (!Util.isNotEmpty(password)) {
                return ReplyHelper.notExist("账号或密码错误");
            }

            code = core.getGeneralCode(userId, account, password);
        } else {
            code = core.getSmsCode(userId, account);
            if (!code.matches("[0-9a-f]{32}")) {
                return ReplyHelper.fail(code);
            }
        }

        return ReplyHelper.success(code);
    }

    /**
     * 获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getToken(LoginDto login) {
        String code = core.getCode(login.getSignature());
        if (code != null) {
            return core.getToken(code, login);
        }

        // 处理错误
        String account = login.getAccount();
        logger.warn("账号[{}]正在尝试使用错误的签名请求令牌!", account);

        Long userId = core.getUserId(account);
        String key = "User:" + userId;
        if (!Redis.hasKey(key)) {
            Redis.deleteKey("ID:" + account);
            return ReplyHelper.fail("发生了一点小意外,请重新提交");
        }

        int failureCount = core.getFailureCount(userId);
        if (failureCount > 5) {
            return ReplyHelper.fail("错误次数过多,账号已被锁定!请于10分钟后再试");
        }

        Redis.setHash(key, "FailureCount", failureCount + 1);
        Redis.setHash(key, "LastFailureTime", DateTime.formatCurrentTime());

        return ReplyHelper.invalidParam("账号或密码错误");
    }

    /**
     * 获取授权码
     *
     * @return Reply
     */
    @Override
    public Reply getAuthCode() {
        String code = Util.uuid();
        Redis.set("Code:" + code, "", 300L);

        return ReplyHelper.success(authUrl + code);
    }

    /**
     * 扫码授权
     *
     * @param info 用户登录信息
     * @param code 二维码
     * @return Reply
     */
    @Override
    public Reply authWithCode(LoginInfo info, String code) {
        if (Util.isEmpty(code)) {
            return ReplyHelper.invalidParam();
        }

        Redis.set("Code:" + code, info.getUserId().toString(), 30L);
        return ReplyHelper.success();
    }

    /**
     * 扫码授权获取Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithCode(LoginDto login) {
        String code = login.getCode();
        String id = Redis.get("Code:" + code);

        return Util.isEmpty(id) ? ReplyHelper.invalidCode() : core.getToken(code, login);
    }

    /**
     * 通过微信授权码获取Token,如用户不存在,则返回用户不存在的错误,同时返回微信用户信息
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply getTokenWithWeChat(LoginDto login) {
        String code = login.getCode();
        String weChatAppId = login.getWeChatAppId();

        WeChatUser weChatUser = core.getWeChatInfo(code, weChatAppId);
        if (weChatUser == null) {
            return ReplyHelper.invalidParam("微信授权失败");
        }

        String unionId = weChatUser.getUnionid();
        if (unionId == null || unionId.isEmpty()) {
            return ReplyHelper.fail("未取得微信用户的UnionID");
        }

        // 使用微信UnionID读取缓存,如用户不存在,则缓存微信用户信息(30分钟)后返回微信用户信息
        Long userId = core.getUserId(unionId);
        if (userId == null) {
            String key = "Wechat:" + Util.md5(unionId + weChatAppId);
            Redis.set(key, Json.toJson(weChatUser), 30L, TimeUnit.MINUTES);

            return ReplyHelper.notExist(weChatUser);
        }

        String key = "User:" + userId;
        User user = Json.clone(Redis.getEntity(key), User.class);
        if (user.getInvalid()) {
            return ReplyHelper.fail("用户被禁止使用");
        }

        // 验证应用是否过期
        if (core.appIsExpired(login, userId)) {
            return ReplyHelper.fail("应用已过期,请续租");
        }

        core.bindOpenId(userId, weChatUser.getOpenid(), weChatAppId);
        TokenDto tokens = core.creatorToken(Util.uuid(), login, user);

        return ReplyHelper.success(tokens);
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
        String mobile = login.getAccount();
        String verifyKey = Util.md5(0 + mobile + login.getCode());
        Reply reply = core.verifySmsCode(verifyKey);
        if (!reply.getSuccess()) {
            return ReplyHelper.invalidParam("短信验证码错误");
        }

        // 从缓存读取微信用户信息
        String unionId = login.getUnionId();
        String key = "Wechat:" + unionId;
        String json = Redis.get(key);
        WeChatUser weChatUser = Json.toBean(json, WeChatUser.class);
        if (weChatUser == null) {
            return ReplyHelper.invalidParam("微信授权已过期，请重新登录");
        }

        // 根据手机号获取用户
        Long userId = core.getUserId(mobile);
        User user = new User();
        if (userId != null) {
            key = "User:" + userId;
            boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
            if (isInvalid) {
                return ReplyHelper.fail("用户被禁止使用");
            }

            user = Json.clone(Redis.getEntity(key), User.class);
            String uid = user.getUnionId();
            if (uid != null && !uid.isEmpty()) {
                if (login.getReplace()) {
                    Redis.deleteKey("ID:" + uid);
                } else {
                    return ReplyHelper.invalidParam("手机号 " + mobile + " 的用户已绑定其他微信号，请使用正确的微信号登录");
                }
            }

            // 更新用户微信UnionID
            core.updateUnionId(userId, unionId);
        } else {
            // 用户不存在,自动创建用户
            user.setName(weChatUser.getNickname());
            user.setAccount(mobile);
            user.setMobile(mobile);
            user.setUnionId(unionId);
            user.setHeadImg(weChatUser.getHeadimgurl());
            core.addUser(user);
        }

        // 验证应用是否过期
        if (core.appIsExpired(login, userId)) {
            return ReplyHelper.fail("应用已过期,请续租");
        }

        // 绑定用户微信OpenID,创建令牌
        core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());
        TokenDto tokens = core.creatorToken(Util.uuid(), login, user);

        return ReplyHelper.success(tokens);
    }

    /**
     * 获取用户授权码
     *
     * @param info 用户登录信息
     * @return Reply
     */
    @Override
    public Reply getPermits(LoginInfo info) {
        List<String> list = mapper.getAuthInfos(info.getAppId(), info.getTenantId(), info.getUserId());

        return ReplyHelper.success(list);
    }

    /**
     * 刷新访问令牌过期时间
     *
     * @param fingerprint 用户特征串
     * @param accessToken 刷新令牌
     * @return Reply
     */
    @Override
    public Reply refreshToken(String fingerprint, AccessToken accessToken) {
        // 验证令牌
        String tokenId = accessToken.getId();
        Token token = core.getToken(tokenId);
        if (token == null || !token.verifyRefreshKey(accessToken)) {
            return ReplyHelper.fail("未提供RefreshToken");
        }

        // 验证用户
        String key = "User:" + token.getUserId();
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
        if (isInvalid) {
            return ReplyHelper.fail("用户被禁止使用");
        }

        TokenDto tokens = core.refreshToken(token, tokenId, fingerprint);
        return ReplyHelper.success(tokens);
    }

    /**
     * 用户账号离线
     *
     * @param tokenId 令牌ID
     * @return Reply
     */
    @Override
    public Reply deleteToken(String tokenId) {
        core.deleteToken(tokenId);

        return ReplyHelper.success();
    }

    /**
     * 获取用户可选租户
     *
     * @param appId   应用ID
     * @param account 登录账号
     * @return Reply
     */
    @Override
    public Reply getTenants(Long appId, String account) {
        Long userId = core.getUserId(account);
        List<MemberDto> list = mapper.getTenants(appId, userId);

        return ReplyHelper.success(list);
    }

    /**
     * 获取用户导航栏
     *
     * @param info 用户登录信息
     * @return Reply
     */
    @Override
    public Reply getNavigators(LoginInfo info) {
        List<NavDto> list = mapper.getNavigators(info.getAppId(), info.getTenantId(), info.getUserId());

        return ReplyHelper.success(list);
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param info     用户登录信息
     * @param moduleId 功能模块ID
     * @return Reply
     */
    @Override
    public Reply getModuleFunctions(LoginInfo info, Long moduleId) {
        List<FuncDto> list = mapper.getModuleFunctions(moduleId, info.getTenantId(), info.getUserId());

        return ReplyHelper.success(list);
    }
}
