package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.*;
import com.insight.utils.Json;
import com.insight.utils.Util;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.auth.TokenKey;
import com.insight.utils.pojo.base.BusinessException;
import com.insight.utils.pojo.base.DataBase;
import com.insight.utils.pojo.base.Reply;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 租户服务控制器
 */
@RestController
@RequestMapping("/base/auth")
public class AuthController {
    private final AuthService service;

    /**
     * 构造方法
     *
     * @param service 自动注入的AuthService
     */
    public AuthController(AuthService service) {
        this.service = service;
    }

    /**
     * 获取用户可选租户
     *
     * @param appId   应用ID
     * @param account 登录账号
     * @return Reply
     */
    @GetMapping("/v1.0/{appId}/tenants")
    public List<DataBase> getTenants(@PathVariable Long appId, @RequestParam String account) {
        return service.getTenants(appId, account);
    }

    /**
     * 生成加密Code或授权识别码
     *
     * @param dto CodeDTO
     * @return Reply
     */
    @PostMapping("/v1.0/codes")
    public Reply getCode(@Valid @RequestBody CodeDto dto) {
        switch (dto.getType()) {
            case 0, 1 -> {
                if (Util.isEmpty(dto.getAccount())) {
                    throw new BusinessException("登录账号不能为空");
                }
                return service.generateCode(dto);
            }
            case 2 -> {
                return service.getAuthCode();
            }
            case 3 -> {
                if (Util.isEmpty(dto.getHashKey())) {
                    throw new BusinessException("接口哈希值不能为空");
                }
                return service.getSubmitToken(dto.getHashKey());
            }
            default -> throw new BusinessException("错误的Code类型");
        }
    }

    /**
     * APP扫码授权
     *
     * @param loginInfo 用户信息
     * @param code      授权识别码
     */
    @PutMapping("/v1.0/codes/{code}")
    public void authWithCode(@RequestHeader("loginInfo") String loginInfo, @PathVariable String code) {
        var info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);
        service.authWithCode(info, code);
    }

    /**
     * 生成Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @PostMapping("/v1.0/tokens")
    public Reply generateToken(@RequestHeader("fingerprint") String fingerprint, @RequestHeader(value = "Authorization", required = false) String token, @Valid @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);

        // 账号密码或短信验证码登录
        if (Util.isNotEmpty(login.getAccount()) && Util.isNotEmpty(login.getSignature())) {
            return service.generateToken(login);
        }

        // 微信授权登录
        if (Util.isNotEmpty(login.getWeChatAppId())) {
            if (Util.isNotEmpty(login.getCode())) {
                return service.getTokenWithWeChat(login);
            }

            if (Util.isNotEmpty(login.getUnionId())) {
                if (Util.isNotEmpty(login.getAccount())) {
                    return service.getTokenWithUserInfo(login);
                }

                if (login.getTenantId() != null) {
                    return service.getTokenWithUnionId(login);
                }
            }
        }

        // APP扫码授权登录
        if (Util.isNotEmpty(login.getCode())) {
            return service.getTokenWithCode(login);
        }

        // 获取其他应用令牌
        if (Util.isNotEmpty(token)) {
            var accessToken = Json.toToken(token);
            if (accessToken == null) {
                throw new BusinessException(421, "无效凭证");
            }

            return service.getToken(login, accessToken);
        }

        throw new BusinessException("接口参数错误");
    }

    /**
     * 用户账号离线
     *
     * @param loginInfo 用户信息
     */
    @DeleteMapping("/v1.0/tokens")
    public void deleteToken(@RequestHeader("loginInfo") String loginInfo) {
        var info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);
        service.deleteToken(new TokenKey(info.getAppId(), info.getTenantId(), info.getId()));
    }

    /**
     * 获取用户授权码
     *
     * @param loginInfo 用户信息
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/permits")
    public List<String> getPermits(@RequestHeader("loginInfo") String loginInfo) {
        var info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);
        return service.getPermits(new TokenKey(info.getAppId(), info.getTenantId(), info.getId()));
    }

    /**
     * 获取用户导航栏
     *
     * @param loginInfo 用户信息
     * @return Reply
     */
    @GetMapping("/v1.0/navigators")
    public List<NavDto> getNavigators(@RequestHeader("loginInfo") String loginInfo) {
        var info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);
        return service.getNavigators(new TokenKey(info.getAppId(), info.getTenantId(), info.getId()));
    }

    /**
     * 获取业务模块的功能(及对用户的授权情况)
     *
     * @param loginInfo 用户信息
     * @param moduleId  功能模块ID
     * @return Reply
     */
    @GetMapping("/v1.0/navigators/{id}/functions")
    public List<FuncDto> getModuleFunctions(@RequestHeader("loginInfo") String loginInfo, @PathVariable("id") Long moduleId) {
        var info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);
        return service.getModuleFunctions(info.getTenantId(), info.getId(), moduleId);
    }

    /**
     * 获取学科网OpenID
     *
     * @param loginInfo 用户信息
     * @param dto       回调数据
     * @return 获取学科网OpenID
     */
    @GetMapping("/v1.0/xkw/openid")
    public String getXkwOpenId(@RequestHeader("loginInfo") String loginInfo, CallbackDto dto) {
        var info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);
        dto.setId(info.getId());
        dto.setAccount(info.getMobile());

        info.getOpenIds().stream().filter(i -> i.matches("xkw")).findFirst()
                .ifPresent(openId -> dto.setOpenId(openId.getOpenId()));
        return service.getXkwOpenId(dto);
    }
}
