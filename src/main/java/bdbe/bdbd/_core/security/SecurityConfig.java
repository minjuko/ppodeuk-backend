package bdbe.bdbd._core.security;


import bdbe.bdbd._core.exception.ForbiddenError;
import bdbe.bdbd._core.exception.UnAuthorizedError;
import bdbe.bdbd._core.utils.FilterResponseUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.Arrays;


@Configuration
public class SecurityConfig {

    @Value("${frontend.localurl}")
    private String frontlocalurl;

    @Value("${frontend.localurls:${frontend.localurl}}")
    private String frontlocalurls;

    @Value("${frontend.prod.userurl}")
    private String prodfrontuserurl;

    @Value("${frontend.prod.ownerurl}")
    private String prodfrontownerurl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    public class CustomSecurityFilterManager extends AbstractHttpConfigurer<CustomSecurityFilterManager, HttpSecurity> {
        @Override
        public void configure(HttpSecurity builder) throws Exception {
            AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);
            builder.addFilter(new JwtAuthenticationFilter(authenticationManager));

            super.configure(builder);
        }
    }


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        // CSRF 해제
        http.csrf().disable();

        // iframe 거부
        http.headers().frameOptions().sameOrigin();

        // cors 재설정
        http.cors().configurationSource(corsConfigurationSource);

        // jSessionId 사용 거부
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // form 로긴 해제 (UsernamePasswordAuthenticationFilter 비활성화)
        http.formLogin().disable();

        // 로그인 인증창이 뜨지 않게 비활성화
        http.httpBasic().disable();

        // 커스텀 필터 적용 (시큐리티 필터 교환)
        http.apply(new CustomSecurityFilterManager());

        // 인증 실패 처리
        http.exceptionHandling().authenticationEntryPoint((request, response, authException) -> {
            FilterResponseUtils.unAuthorized(response, new UnAuthorizedError(
                    UnAuthorizedError.ErrorCode.AUTHENTICATION_FAILED,
                    Collections.singletonMap("auth", "Not authenticated")
            ));
        });

        // 권한 실패 처리
        http.exceptionHandling().accessDeniedHandler((request, response, accessDeniedException) -> {
            FilterResponseUtils.forbidden(response, new ForbiddenError(
                    ForbiddenError.ErrorCode.ROLE_BASED_ACCESS_ERROR,
                    Collections.singletonMap("access", "Access is denied")
            ));
        });

        // 인증, 권한 필터 설정
        http.authorizeRequests(authorize -> authorize
                .antMatchers("/api/open/**").permitAll()
                .antMatchers("/demo-images/**").permitAll()
                .antMatchers("/uploads/**").permitAll()
                .antMatchers("/api/user/**").access("hasAnyRole('USER', 'OWNER')")
                .antMatchers("/api/owner/**").access("hasRole('OWNER')")
                .anyRequest().authenticated()); // 모든 다른 요청은 인증 필요

        return http.build();
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedDoubleSlash(true);
        return firewall;
    }

    // WebSecurityCustomizer를 통해 WebSecurity를 커스터마이징
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
    }

    @Bean
    @Profile("!prod & !demo")
    @Primary
    public CorsConfigurationSource devCorsConfigurationSource() {
        // 개발 환경용 CORS 설정
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        Arrays.stream(frontlocalurls.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .forEach(configuration::addAllowedOrigin);
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    @Profile("prod | demo")
    @Primary
    public CorsConfigurationSource prodCorsConfigurationSource() {
        // 운영 환경용 CORS 설정
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        // USER, OWNER 배포 주소 (React)
        configuration.addAllowedOriginPattern(prodfrontuserurl);
        configuration.addAllowedOriginPattern(prodfrontownerurl);
        configuration.setAllowCredentials(true);
        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }


    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowCredentials(true); // 클라이언트에서 쿠키 요청 허용
        configuration.addExposedHeader("Authorization"); // 권고사항

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
