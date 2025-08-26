package com.insight.base.auth.common.xkw;

import com.insight.utils.EnvUtil;
import com.insight.utils.Json;
import com.insight.utils.Util;
import com.insight.utils.encrypt.AesEncryptor;
import com.insight.utils.http.HttpClient;
import com.insight.utils.pojo.base.BusinessException;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author 宣炳刚
 * @date 2025/8/23
 * @remark 学科网Auth模块通用工具类
 */
public class AuthUtil {
    private static final String appKey;
    private static final String appSecret;
    private static final String authUrl;
    private static final String service;
    private static final String redirectUri;

    static {
        appKey = EnvUtil.getValue("xkw.appKey");
        appSecret = EnvUtil.getValue("xkw.appSecret");
        authUrl = EnvUtil.getValue("xkw.authUrl");
        service = EnvUtil.getValue("xkw.service");
        redirectUri = EnvUtil.getValue("xkw.redirectUri");
    }

    public static String getAuthUrl(String account) {
        SortedMap<String, String> params = new TreeMap<>();
        params.put("client_id", appKey);
        params.put("service", service);
        params.put("redirect_uri", redirectUri);
        params.put("timespan", getTimespan());
        params.put("extra", account);

        var signature = generateSignature(params, appSecret);
        params.put("signature", signature);

        return HttpClient.buildUrl(authUrl + "/oauth2/authorize?", params);
    }

    /**
     * 获取学科网OpenId
     *
     * @param id   用户ID
     * @param code 授权码
     * @return OpenId
     */
    public static String getOpenId(Long id, String code) {
        var token = getAccessToken(id, code);
        return getProfile(token);
    }

    /**
     * 获取学科网AccessToken
     *
     * @param id   用户ID
     * @param code 授权码
     * @return AccessToken
     */
    private static String getAccessToken(Long id, String code) {
        SortedMap<String, String> params = new TreeMap<>();
        params.put("client_id", appKey);
        params.put("code", code);
        params.put("redirect_uri", redirectUri);

        var signature = generateSignature(params, appSecret);
        params.put("signature", signature);

        var url = HttpClient.buildUrl(authUrl + "/oauth2/accessToken", params);
        var data = HttpClient.get(url);

        var token = Json.toBean(data, AccessToken.class);
        if (token == null) {
            throw new BusinessException("获取AccessToken失败");
        } else if (Util.isEmpty(token.getToken())) {
            throw new BusinessException("获取AccessToken失败: " + token.getError());
        }

        return token.getToken();
    }

    /**
     * 获取授权用户信息
     *
     * @param token 授权令牌
     * @return OpenId
     */
    private static String getProfile(String token) {
        var url = authUrl + "oauth2/profile?access_token=" + token;
        var data = HttpClient.get(url);

        var profile = Json.toBean(data, Profile.class);
        if (profile == null) {
            throw new BusinessException("获取授权用户信息失败");
        } else if (Util.isEmpty(profile.getOpenId())) {
            throw new BusinessException("获取授权用户信息失败: " + profile.getError());
        }

        return profile.getOpenId();
    }

    /**
     * 根据SortMap类型参数和secret获取签名
     *
     * @param param  SortMap类型参数
     * @param secret 密钥
     * @return 签名
     */
    public static String generateSignature(SortedMap<String, String> param, String secret) {
        var sb = new StringBuilder();
        for (var key : param.keySet()) {
            sb.append(param.get(key));
        }

        sb.append(secret);
        return Util.md5(sb.toString());
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    private static String getTimespan() {
        var encryptor = new AesEncryptor(appSecret);
        var timespan = String.valueOf(System.currentTimeMillis());
        return encryptor.encrypt(timespan);
    }
}
