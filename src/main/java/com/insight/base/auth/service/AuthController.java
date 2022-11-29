package com.insight.base.auth.service;

import com.insight.base.auth.common.dto.FuncDto;
import com.insight.base.auth.common.dto.LoginDto;
import com.insight.base.auth.common.dto.NavDto;
import com.insight.base.auth.common.dto.TokenDto;
import com.insight.utils.Json;
import com.insight.utils.ReplyHelper;
import com.insight.utils.pojo.auth.AccessToken;
import com.insight.utils.pojo.auth.LoginInfo;
import com.insight.utils.pojo.base.BusinessException;
import com.insight.utils.pojo.base.Reply;
import com.insight.utils.pojo.user.MemberDto;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author 宣炳刚
 * @date 2017/12/18
 * @remark 租户服务控制器
 */
@CrossOrigin
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
     * 获取提交数据用临时Token
     *
     * @param key 接口Hash
     * @return Reply
     */
    @GetMapping("/v1.0/tokens")
    public String getSubmitToken(@RequestParam String key) {
        return service.getSubmitToken(key);
    }

    /**
     * 获取Code
     *
     * @param account 用户登录账号
     * @param type    登录类型(0:密码登录、1:验证码登录)
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/codes")
    public String getCode(@RequestParam String account, @RequestParam(defaultValue = "0") int type) {
        return service.getCode(account, type);
    }

    /**
     * 获取Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @PostMapping("/v1.0/tokens")
    public TokenDto getToken(@RequestHeader("fingerprint") String fingerprint, @Valid @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);
        String account = login.getAccount();
        if (account == null || account.isEmpty()) {
            throw new BusinessException("登录账号不能为空");
        }

        return service.getToken(login);
    }

    /**
     * 获取授权码
     *
     * @return Reply
     */
    @PostMapping("/v1.0/tokens/codes")
    public String getAuthCode() {
        return service.getAuthCode();
    }

    /**
     * 扫码授权
     *
     * @param loginInfo 用户信息
     * @param code      用户登录数据
     */
    @PutMapping("/v1.0/tokens/{code}")
    public void authWithCode(@RequestHeader("loginInfo") String loginInfo, @PathVariable String code) {
        LoginInfo info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);
        service.authWithCode(info, code);
    }

    /**
     * 扫码授权获取Token
     *
     * @param fingerprint 用户特征串
     * @param code        授权码
     * @param login       用户登录数据
     * @return Reply
     */
    @PostMapping("/v1.0/tokens/{code}")
    public TokenDto getTokenWithCode(@RequestHeader("fingerprint") String fingerprint, @PathVariable String code, @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);
        login.setCode(code);

        return service.getTokenWithCode(login);
    }

    /**
     * 通过微信授权码获取Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @PostMapping("/v1.0/tokens/wechat/code")
    public TokenDto getTokenWithWeChat(@RequestHeader("fingerprint") String fingerprint, @Valid @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);
        String appId = login.getWeChatAppId();
        if (appId == null || appId.isEmpty()) {
            throw new BusinessException("weChatAppId不能为空");
        }

        return service.getTokenWithWeChat(login);
    }

    /**
     * 通过微信UnionId获取Token
     *
     * @param fingerprint 用户特征串
     * @param login       用户登录数据
     * @return Reply
     */
    @PostMapping("/v1.0/tokens/wechat/unionid")
    public TokenDto getTokenWithUserInfo(@RequestHeader("fingerprint") String fingerprint, @Valid @RequestBody LoginDto login) {
        login.setFingerprint(fingerprint);
        String appId = login.getWeChatAppId();
        if (appId == null || appId.isEmpty()) {
            throw new BusinessException("weChatAppId不能为空");
        }

        return service.getTokenWithUserInfo(login);
    }

    /**
     * 验证Token
     *
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/status")
    public Reply verifyToken() {
        return ReplyHelper.success();
    }

    /**
     * 获取用户授权码
     *
     * @param loginInfo 用户信息
     * @return Reply
     */
    @GetMapping("/v1.0/tokens/permits")
    public List<String> getPermits(@RequestHeader("loginInfo") String loginInfo) {
        LoginInfo info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);

        return service.getPermits(info);
    }

    /**
     * 刷新Token，延长过期时间至2小时后
     *
     * @param fingerprint 用户特征串
     * @param token       刷新令牌字符串
     * @return Reply
     */
    @PutMapping("/v1.0/tokens")
    public TokenDto refreshToken(@RequestHeader("fingerprint") String fingerprint, @RequestHeader("Authorization") String token) {
        AccessToken refreshToken = Json.toAccessToken(token);
        if (refreshToken == null) {
            throw new BusinessException("未提供RefreshToken");
        }

        return service.refreshToken(fingerprint, refreshToken);
    }

    /**
     * 用户账号离线
     *
     * @param token 访问令牌字符串
     */
    @DeleteMapping("/v1.0/tokens")
    public void deleteToken(@RequestHeader(value = "Authorization") String token) {
        AccessToken accessToken = Json.toAccessToken(token);

        service.deleteToken(accessToken.getId());
    }

    /**
     * 获取用户可选租户
     *
     * @param appId   应用ID
     * @param account 登录账号
     * @return Reply
     */
    @GetMapping("/v1.0/{appId}/tenants")
    public List<MemberDto> getTenants(@PathVariable Long appId, @RequestParam String account) {

        return service.getTenants(appId, account);
    }

    /**
     * 获取用户导航栏
     *
     * @param loginInfo 用户信息
     * @return Reply
     */
    @GetMapping("/v1.0/navigators")
    public List<NavDto> getNavigators(@RequestHeader("loginInfo") String loginInfo) {
        LoginInfo info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);

        return service.getNavigators(info);
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
        LoginInfo info = Json.toBeanFromBase64(loginInfo, LoginInfo.class);

        return service.getModuleFunctions(info, moduleId);
    }
}
