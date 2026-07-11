package com.jejulocaltime.api.config;

import com.jejulocaltime.api.auth.JwtAuthenticationFilter;
import com.jejulocaltime.api.auth.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/",
            "/error",
            "/actuator/health",
            "/api/auth/kakao",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        
                        // 입점 신청 도메인: 로그인한 유저 누구나 접근 가능 (USER 권한)
                        .requestMatchers("/api/seller/application/**").authenticated()
                        
                        // 판매자 프로필 도메인: 승인된 판매자만 접근 가능 (SELLER 권한)
                        .requestMatchers(HttpMethod.GET, "/api/seller/profile").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/seller/profile").hasRole("SELLER")

                        // 관리자(ADMIN) 전용 API: ADMIN 권한만 접근 가능
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
