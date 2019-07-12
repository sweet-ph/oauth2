package com.example.demo.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

/**
 * Created by mike on 2019/7/9
 */
@Configuration
public class CustomConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Configuration
    @EnableAuthorizationServer
    static class CustomAuthorizationServer extends AuthorizationServerConfigurerAdapter {
        private final PasswordEncoder passwordEncoder;

        public CustomAuthorizationServer(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }

        @Override
        public void configure(AuthorizationServerSecurityConfigurer oauthServer) {
            oauthServer.tokenKeyAccess("permitAll()")
                    .checkTokenAccess("isAuthenticated()");
        }

        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.inMemory()
                    .withClient("custom_client_id")
                    .secret(passwordEncoder.encode("$2a$10$JknlOkbQANofGnc9BRkLv.Kuixt/pZleX2VC54udsy5Gqry7iSFzK"))
                    .authorizedGrantTypes("authorization_code")
                    .scopes("user_info")
                    .autoApprove(true)
                    .redirectUris("http://www.oauth.com/login/oauth2/code/custom")
                    .authorizedGrantTypes("authorization_code", "implicit", "refresh_token", "client_credentials");
        }
    }

    @Configuration
    @EnableResourceServer
    static class CustomResourceServer extends ResourceServerConfigurerAdapter {
        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            //此处是关键，默认stateless=true，只支持access_token形式，
            // OAuth2客户端连接需要使用session，所以需要设置成false以支持session授权
            resources.stateless(false);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .formLogin()
                    .and()
                    .httpBasic();

            //需要的时候创建session，支持从session中获取认证信息，ResourceServerConfiguration中
            //session创建策略是stateless不使用，这里其覆盖配置可创建session
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
        }
    }
}