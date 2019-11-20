package com.insight.base.auth.service;

import com.insight.base.auth.common.Core;
import com.insight.base.auth.common.Token;
import com.insight.base.auth.common.dto.LoginDto;
import com.insight.base.auth.common.dto.TokenDto;
import com.insight.base.auth.common.enums.TokenType;
import com.insight.util.*;
import com.insight.util.pojo.AccessToken;
import com.insight.util.pojo.LoginInfo;
import com.insight.util.pojo.Reply;
import com.insight.utils.wechat.WeChatUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Core core;

    /**
     * 构造函数
     *
     * @param core Core
     */
    public AuthServiceImpl(Core core) {
        this.core = core;
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
        String userId = core.getUserId(account);
        if (userId == null) {
            if (type == 0) {
                return ReplyHelper.notExist("账号或密码错误");
            } else {
                userId = core.addUser(account, account, null, null);
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
            if (password == null || password.isEmpty()) {
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
        // 验证签名
        String code = core.getCode(login.getSignature());
        if (code == null) {
            String account = login.getAccount();
            logger.warn("账号[{}]正在尝试使用错误的签名请求令牌!", account);

            String userId = core.getUserId(account);
            String key = "User:" + userId;
            if (!Redis.hasKey(key)) {
                Redis.deleteKey("ID:" + account);
                return ReplyHelper.fail("发生了一点小意外,请重新提交");
            }

            int failureCount = core.getFailureCount(userId);
            if (failureCount > 5) {
                return ReplyHelper.fail("错误次数过多,账号已被锁定!请于10分钟后再试");
            }

            Redis.set(key, "FailureCount", failureCount + 1);
            Redis.set(key, "LastFailureTime", DateHelper.getDateTime());

            return ReplyHelper.invalidParam("账号或密码错误");
        }

        // 验证用户
        String userId = core.getId(code);
        if (userId == null || userId.isEmpty()) {
            return ReplyHelper.fail("发生了一点小意外,请重新提交");
        }

        String key = "User:" + userId;
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
        if (isInvalid) {
            return ReplyHelper.forbid();
        }

        return ReplyHelper.success(core.creatorToken(code, login, userId));
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
        String userId = core.getUserId(unionId);
        if (userId == null || userId.isEmpty()) {
            String key = "Wechat:" + Util.md5(unionId + weChatAppId);
            Redis.set(key, Json.toJson(weChatUser), 30, TimeUnit.MINUTES);

            return ReplyHelper.notExist(weChatUser);
        }

        String key = "User:" + userId;
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
        if (isInvalid) {
            return ReplyHelper.forbid();
        }

        core.bindOpenId(userId, weChatUser.getOpenid(), weChatAppId);
        TokenDto tokens = core.creatorToken(Generator.uuid(), login, userId);

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

        // 根据手机号获取用户,如用户不存在,则则自动创建用户
        String userId = core.getUserId(mobile);
        if (userId == null || userId.isEmpty()) {
            core.addUser(weChatUser.getNickname(), mobile, unionId, weChatUser.getHeadimgurl());
            core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());
            TokenDto tokens = core.creatorToken(Generator.uuid(), login, userId);

            return ReplyHelper.success(tokens);
        }

        key = "User:" + userId;
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
        if (isInvalid) {
            return ReplyHelper.forbid();
        }

        String uid = Redis.get(key, "unionId");
        if (uid != null && !uid.isEmpty()) {
            // 已绑定微信UnionID
            if (login.getReplace()) {
                Redis.deleteKey("ID:" + uid);
            } else {
                return ReplyHelper.invalidParam("手机号 " + mobile + " 的用户已绑定其他微信号，请使用正确的微信号登录");
            }
        }

        // 绑定用户微信UnionID和OpenID
        core.updateUnionId(userId, unionId);
        core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());
        TokenDto tokens = core.creatorToken(Generator.uuid(), login, userId);

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
        List<String> list = core.getPermits(info.getAppId(), info.getUserId(), info.getTenantId(), info.getDeptId());

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
        String tokenId = accessToken.getId();
        String secret = accessToken.getSecret();

        // 验证令牌
        Token token = core.getToken(tokenId);
        if (token == null || !token.verify(secret, TokenType.RefreshToken, tokenId, token.getUserId())) {
            return ReplyHelper.invalidToken();
        }

        // 验证用户
        String key = "User:" + token.getUserId();
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
        if (isInvalid) {
            return ReplyHelper.forbid();
        }

        return ReplyHelper.success(core.refreshToken(token, tokenId, fingerprint));
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
     * 获取用户导航栏
     *
     * @param info 用户登录信息
     * @return Reply
     */
    @Override
    public Reply getNavigators(LoginInfo info) {
        return ReplyHelper.success(core.getNavigators(info));
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param info     用户登录信息
     * @param moduleId 功能模块ID
     * @return Reply
     */
    @Override
    public Reply getModuleFunctions(LoginInfo info, String moduleId) {
        return ReplyHelper.success(core.getModuleFunctions(info, moduleId));
    }
}
