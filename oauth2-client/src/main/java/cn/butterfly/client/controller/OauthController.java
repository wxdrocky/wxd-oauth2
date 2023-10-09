package cn.butterfly.client.controller;

import cn.butterfly.client.api.GithubApiService;
import cn.butterfly.client.api.GithubAuthService;
import cn.butterfly.client.api.Oauth2ApiService;
import cn.butterfly.client.api.Oauth2AuthService;
import cn.butterfly.client.entity.*;
import cn.butterfly.client.feignClients.GithubApiFeignClient;
import cn.butterfly.client.feignClients.GithubAuthFeignClient;
import cn.butterfly.common.base.UserType;
import cn.butterfly.common.constant.BaseConstants;
import cn.butterfly.common.exception.ApiException;
import cn.butterfly.common.util.JwtUtils;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static cn.butterfly.common.constant.BaseConstants.REDIRECT_FAILED;

/**
 * oauth2 认证控制器
 *
 * @author zjw
 * @date 2021-10-23
 */
@RestController
@RequestMapping("/oauth")
@Slf4j
public class OauthController {

    @Resource
    private GithubAuth githubAuth;

    @Resource
    private Oauth2Auth oauth2Auth;

    @Resource
    private GithubApiService githubApiService;

    @Resource
    private GithubAuthService githubAuthService;

    @Resource
    private Oauth2AuthService oauth2AuthService;

    @Resource
    private Oauth2ApiService oauth2ApiService;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private GithubApiFeignClient githubApiFeignClient;

    @Autowired
    private GithubAuthFeignClient githubAuthFeignClient;

    /**
     * github 重定向地址
     *
     * @param code     临时授权码
     * @param response 响应
     */
    @GetMapping("/github/redirect")
    public void githubRedirect(String code, HttpServletResponse response) {
        log.info("进入github回调方法");
        String clientId = githubAuth.getClientId();
        String clientSecret = githubAuth.getClientSecret();
        Map<String, String> uriVariables = new HashMap<>(3);
        uriVariables.put("clientId", clientId);
        uriVariables.put("clientSecret", clientSecret);
        uriVariables.put("code", code);
        log.info("获取githubAccessToken请求参数:" + uriVariables);
        GithubUser githubUser = new GithubUser();
        try {
            GithubToken githubToken = githubAuthFeignClient.getToken(clientId, clientSecret, code);
            log.info("githubAccessToken:" + githubToken.getAccessToken() + ",token类型:" + githubToken.getTokenType());
            String authorization = String.join(StringUtils.SPACE, githubToken.getTokenType(), githubToken.getAccessToken());
            String finalAuthorization = "bearer " + authorization;
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Authorization", finalAuthorization);
            githubUser = githubApiFeignClient.getUserInfo(headers);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        // 获取 github 用户信息
        String token = JwtUtils.sign(githubUser.getUsername(), UserType.GITHUB.getType());
        try {
            response.sendRedirect(githubAuth.getFrontRedirectUrl() + "?token=" + token);
        } catch (IOException e) {
            throw new ApiException(REDIRECT_FAILED);
        }
    }

    /**
     * oauth2 重定向地址
     *
     * @param code     临时授权码
     * @param response 响应
     */
    @GetMapping("/oauth2/redirect")
    public void oauth2Redirect(String code, HttpServletResponse response) {
        // 获取 access_token
        String clientId = oauth2Auth.getClientId();
        String clientSecret = oauth2Auth.getClientSecret();
        String authorization = BaseConstants.BASIC_TYPE + Base64.getEncoder().encodeToString(
                (String.join(":", clientId, clientSecret)).getBytes()
        );
        // 获取 oauth2 用户名
        Oauth2Token oauth2Token = oauth2AuthService.getToken(authorization, code, "authorization_code");
        String username = oauth2ApiService.getUserInfo(
                String.join(StringUtils.SPACE, oauth2Token.getTokenType(), oauth2Token.getAccessToken()));
        // 生成本地访问 token
        String token = JwtUtils.sign(username, UserType.OAUTH2.getType());
        try {
            response.sendRedirect(oauth2Auth.getFrontRedirectUrl() + "?token=" + token);
        } catch (IOException e) {
            throw new ApiException(REDIRECT_FAILED);
        }
    }

}
