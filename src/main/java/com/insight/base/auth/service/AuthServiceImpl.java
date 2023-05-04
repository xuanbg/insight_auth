package com.insight.base.auth.service;

import com.insight.base.auth.common.Core;
import com.insight.base.auth.common.Token;
import com.insight.base.auth.common.dto.*;
import com.insight.base.auth.common.mapper.AuthMapper;
import com.insight.utils.*;
import com.insight.utils.pojo.auth.AccessToken;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.base.BusinessException;
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
     * 获取用户可选租户
     *
     * @param appId   应用ID
     * @param account 登录账号
     * @return Reply
     */
    @Override
    public List<MemberDto> getTenants(Long appId, String account) {
        Long userId = core.getUserId(account);
        return mapper.getTenants(appId, userId);
    }

    /**
     * 生成加密Code
     *
     * @param dto CodeDTO
     * @return Reply
     */
    @Override
    public String generateCode(CodeDto dto) {
        var account = dto.getAccount();
        Long userId = core.getUserId(account);
        if (userId == null) {
            if (dto.getType() == 1 && autoReg) {
                User user = new User();
                user.setName(account);
                user.setAccount(account);
                user.setMobile(account);
                userId = core.addUser(user);
            } else {
                throw new BusinessException("账号或密码错误");
            }
        }

        String key = "User:" + userId;
        if (!Redis.hasKey(key)) {
            Redis.deleteKey("ID:" + account);
            throw new BusinessException("发生了一点小意外,请重新提交");
        }

        // 生成Code
        String code;
        if (dto.getType() == 0) {
            String password = Redis.get(key, "password");
            if (!Util.isNotEmpty(password)) {
                throw new BusinessException("账号或密码错误");
            }

            return core.getGeneralCode(userId, account, password);
        } else {
            if (!account.matches("^1[0-9]{10}")) {
                throw new BusinessException("请使用正确的手机号");
            }

            return core.getSmsCode(userId, account);
        }
    }

    /**
     * 获取授权码
     *
     * @return Reply
     */
    @Override
    public String getAuthCode() {
        String code = Util.uuid();
        Redis.set("Code:" + code, "", 300L);

        return authUrl + code;
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

        Redis.set("Code:" + code, info.getUserId().toString(), 30L);
    }

    /**
     * 获取提交数据用临时Token
     *
     * @param key 接口Hash: MD5(userId:method:url)
     * @return Reply
     */
    @Override
    public String getSubmitToken(String key) {
        String id = Redis.get("SubmitToken:" + key);
        if (!Util.isNotEmpty(id)) {
            id = Util.uuid();
            Redis.set("SubmitToken:" + key, id, 1L, TimeUnit.HOURS);
        }

        return id;
    }

    /**
     * 生成Token
     *
     * @param login 用户登录数据
     * @return Reply
     */
    @Override
    public Reply generateToken(LoginDto login) {
        String code = core.getCode(login.getSignature());
        if (code != null) {
            var token = core.getToken(code, login);
            return ReplyHelper.created(token);
        }

        // 处理错误
        String account = login.getAccount();
        logger.warn("账号[{}]正在尝试使用错误的签名请求令牌!", account);

        Long userId = core.getUserId(account);
        String key = "User:" + userId;
        if (!Redis.hasKey(key)) {
            Redis.deleteKey("ID:" + account);
            throw new BusinessException("发生了一点小意外,请重新提交");
        }

        int failureCount = core.getFailureCount(userId);
        if (failureCount > 5) {
            throw new BusinessException("错误次数过多,账号已被锁定!请于10分钟后再试");
        }

        Redis.setHash(key, "FailureCount", failureCount + 1);
        Redis.setHash(key, "LastFailureTime", DateTime.formatCurrentTime());

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
        String code = login.getCode();
        String weChatAppId = login.getWeChatAppId();

        WeChatUser weChatUser = core.getWeChatInfo(code, weChatAppId);
        if (weChatUser == null) {
            throw new BusinessException("微信授权失败");
        }

        String unionId = weChatUser.getUnionid();
        if (unionId == null || unionId.isEmpty()) {
            throw new BusinessException("未取得微信用户的UnionID");
        }

        // 使用微信UnionID读取缓存,如用户不存在,则缓存微信用户信息(30分钟)后返回微信用户信息
        Long userId = core.getUserId(unionId);
        if (userId == null) {
            logger.info("微信UnionId: {} 未绑定到用户", unionId);
            throw new BusinessException(425, "微信账号未绑定到用户");
        }

        User user = Json.clone(Redis.getEntity("User:" + userId), User.class);
        if (user.getInvalid()) {
            throw new BusinessException("用户被禁止使用");
        }

        core.checkExpired(login, userId);
        core.checkType(login.getAppId(), user.getType());
        core.bindOpenId(userId, weChatUser.getOpenid(), weChatAppId);

        var tenants = mapper.getTenantIds(userId);
        if (tenants.size() == 1) {
            login.setTenantId(tenants.get(0).getId());
        } else if (tenants.size() > 1) {
            String key = "Wechat:" + unionId;
            Redis.set(key, Json.toJson(weChatUser), 30L, TimeUnit.MINUTES);

            var dto = new TenantDto();
            dto.setUnionId(unionId);
            dto.setTenants(tenants);
            return ReplyHelper.isMultiTenant(dto);
        }

        var token = core.creatorToken(Util.uuid(), login, user);
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
        String unionId = login.getUnionId();
        String json = Redis.get("Wechat:" + unionId);
        WeChatUser weChatUser = Json.toBean(json, WeChatUser.class);
        if (weChatUser == null) {
            throw new BusinessException("微信授权已过期，请重新登录");
        }

        Long userId = core.getUserId(unionId);
        User user = Json.clone(Redis.getEntity("User:" + userId), User.class);

        var token = core.creatorToken(Util.uuid(), login, user);
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
        String mobile = login.getAccount();
        String verifyKey = Util.md5(0 + mobile + login.getCode());
        core.verifySmsCode(verifyKey);

        // 从缓存读取微信用户信息
        String unionId = login.getUnionId();
        String key = "Wechat:" + unionId;
        String json = Redis.get(key);
        WeChatUser weChatUser = Json.toBean(json, WeChatUser.class);
        if (weChatUser == null) {
            throw new BusinessException("微信授权已过期，请重新登录");
        }

        // 根据手机号获取用户
        Long userId = core.getUserId(mobile);
        User user = new User();
        if (userId != null) {
            key = "User:" + userId;
            boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
            if (isInvalid) {
                throw new BusinessException("用户被禁止使用");
            }

            user = Json.clone(Redis.getEntity(key), User.class);
            String uid = user.getUnionId();
            if (uid != null && !uid.isEmpty()) {
                if (login.getReplace()) {
                    Redis.deleteKey("ID:" + uid);
                } else {
                    throw new BusinessException("手机号 " + mobile + " 的用户已绑定其他微信号，请使用正确的微信号登录");
                }
            }

            // 更新用户微信UnionID
            core.updateUnionId(userId, unionId);
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

        core.checkExpired(login, userId);
        core.checkType(login.getAppId(), user.getType());
        core.bindOpenId(userId, weChatUser.getOpenid(), login.getWeChatAppId());

        var token = core.creatorToken(Util.uuid(), login, user);
        return ReplyHelper.created(token);
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

        if (Util.isEmpty(id)) {
            throw new BusinessException("Code已失效，请刷新");
        }

        var token = core.getToken(code, login);
        return ReplyHelper.created(token);
    }

    /**
     * 获取指定应用的Token
     *
     * @param fingerprint 用户特征串
     * @param accessToken 令牌
     * @param appId       应用ID
     * @return Reply
     */
    @Override
    public Reply getToken(String fingerprint, AccessToken accessToken, Long appId) {
        Token data = core.getToken(accessToken.getId());
        if (!data.verifySecretKey(accessToken.getSecret())) {
            throw new BusinessException(421, "非法的Token");
        }

        String key = "User:" + data.getUserId();
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
        if (isInvalid) {
            throw new BusinessException("用户被禁止使用");
        }

        data.setAppId(appId);
        var token = core.creatorToken(data, fingerprint);
        return ReplyHelper.created(token);
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
        Token data = core.getToken(tokenId);
        if (data.refreshKeyIsInvalid(accessToken)) {
            throw new BusinessException("未提供RefreshToken");
        }

        // 验证用户
        String key = "User:" + data.getUserId();
        boolean isInvalid = Boolean.parseBoolean(Redis.get(key, "invalid"));
        if (isInvalid) {
            throw new BusinessException("用户被禁止使用");
        }

        var token = core.refreshToken(tokenId, data, fingerprint);
        return ReplyHelper.created(token);
    }

    /**
     * 用户账号离线
     *
     * @param tokenId 令牌ID
     */
    @Override
    public void deleteToken(String tokenId) {
        core.deleteToken(tokenId);
    }

    /**
     * 获取用户授权码
     *
     * @param info 用户登录信息
     * @return Reply
     */
    @Override
    public List<String> getPermits(LoginInfo info) {
        return mapper.getAuthInfos(info.getAppId(), info.getTenantId(), info.getUserId());
    }

    /**
     * 获取用户导航栏
     *
     * @param info 用户登录信息
     * @return Reply
     */
    @Override
    public List<NavDto> getNavigators(LoginInfo info) {
        return mapper.getNavigators(info.getAppId(), info.getTenantId(), info.getUserId());
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param info     用户登录信息
     * @param moduleId 功能模块ID
     * @return Reply
     */
    @Override
    public List<FuncDto> getModuleFunctions(LoginInfo info, Long moduleId) {
        return mapper.getModuleFunctions(moduleId, info.getTenantId(), info.getUserId());
    }
}
