package com.insight.base.auth.common.filter;

import com.insight.util.Generator;
import com.insight.util.Json;
import com.insight.util.pojo.BodyReaderRequestWrapper;
import com.insight.util.pojo.Log;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author 宣炳刚
 * @date 2017/10/06
 * @remark 调试信息过滤器
 */
@Component
@WebFilter(urlPatterns = {"/*"})
public class LogFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private FilterConfig filterConfig;

    /**
     * 初始化方法,传入过滤器配置
     *
     * @param filterConfig FilterConfig
     */
    @Override
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (this.filterConfig == null) {
            logger.debug("FilterConfig is NULL!");
        }
    }

    /**
     * 拦截请求，通过DEBUG级别日志输出请求内容
     *
     * @param servletRequest  ServletRequest
     * @param servletResponse ServletResponse
     * @param filterChain     FilterChain
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        MDC.put("requestId", Generator.uuid());
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Log log = new Log();
        log.setTime(new Date());
        log.setLevel("DEBUG");

        // 请求方法和调用的接口URL
        String method = request.getMethod();
        String path = request.getRequestURI();
        log.setMethod(method);
        log.setUrl(path);

        // 读取请求头
        Map<String, String> headers = new HashMap<>(16);
        Enumeration<String> headerList = request.getHeaderNames();
        while (headerList.hasMoreElements()) {
            String headerName = headerList.nextElement();
            String header = request.getHeader(headerName);
            if ("requestid".equalsIgnoreCase(headerName)) {
                ThreadContext.put("requestid", header);
            }

            headers.put(headerName, header);
        }
        log.setHeaders(headers);

        // 读取请求参数
        Map<String, String> params = new HashMap<>(16);
        Map<String, String[]> map = request.getParameterMap();
        map.forEach((k, v) -> params.put(k, v[0]));
        log.setParams(params.isEmpty() ? null : params);

        // 如请求方法为GET,则打印日志后结束
        if ("GET".equals(method)) {
            logger.info("请求参数：{}", Json.toJson(log));
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 非GET请求,读取BODY,打印日志
        BodyReaderRequestWrapper requestWrapper = new BodyReaderRequestWrapper(request);
        BufferedReader reader = requestWrapper.getReader();

        String inputLine;
        StringBuilder body = new StringBuilder();
        while ((inputLine = reader.readLine()) != null) {
            body.append(inputLine);
        }
        reader.close();

        boolean isMatch = Pattern.matches("^[{|\\[].*[}|\\]]$", body);
        if (isMatch) {
            Map bodyMap = Json.toMap(body.toString());
            log.setBody(bodyMap == null ? body.toString() : bodyMap);
        } else {
            log.setBody(body.toString());
        }

        logger.info("请求参数：{}", Json.toJson(log));
        filterChain.doFilter(requestWrapper, servletResponse);
    }

    /**
     * 释放过滤器
     */
    @Override
    public void destroy() {
        this.filterConfig = null;
    }
}
