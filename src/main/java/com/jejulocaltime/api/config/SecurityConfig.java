package com.jejulocaltime.api.config;

import com.jejulocaltime.api.auth.JwtAuthenticationFilter;
import com.jejulocaltime.api.auth.JwtTokenProvider;
import com.jejulocaltime.api.common.exception.ErrorCode;
import com.jejulocaltime.api.common.exception.ErrorResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import com.jejulocaltime.api.repository.UserRepository;

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        
                        // 입점 신청 도메인: 로그인한 유저 누구나 접근 가능 (USER 권한)
                        .requestMatchers("/api/seller/application/**").authenticated()
                        
                        // 판매자 프로필 도메인: 승인된 판매자만 접근 가능 (SELLER 권한)
                        .requestMatchers("/api/seller/profile/**").hasRole("SELLER")

                        // 판매자 상품/자원 관리 도메인 (SEL-01~04): 승인된 판매자만 접근 가능 (SELLER 권한)
                        .requestMatchers("/api/seller/products/**").hasRole("SELLER")
                        .requestMatchers("/api/seller/**").hasRole("SELLER")

                        // 관리자(ADMIN) 전용 API: ADMIN 권한만 접근 가능
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception.accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // role이 요구 조건(hasRole 등)을 만족하지 못해 거부될 때, 다른 도메인 예외와 동일한
    // ErrorResponse(ErrorCode.ACCESS_DENIED) 포맷으로 응답을 내려주기 위한 핸들러
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper objectMapper = new ObjectMapper();
        return (request, response, ex) -> {
            response.setStatus(ErrorCode.ACCESS_DENIED.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            String body = objectMapper.writeValueAsString(
                    ErrorResponse.of(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage()));
            response.getWriter().write(body);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
