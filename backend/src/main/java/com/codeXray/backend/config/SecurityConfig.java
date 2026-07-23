package com.codeXray.backend.config;

import com.codeXray.backend.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // 가입/로그인 등은 열어둠
                        .requestMatchers(HttpMethod.GET, "/api/problems/**").permitAll() // 문제 조회는 공개
                        // 커뮤니티 관리자 전용은 공개 GET 규칙보다 먼저 (순서 중요)
                        .requestMatchers("/api/community/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/community/posts/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/community/posts/*/admin-reply").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/community/**").permitAll() // 게시글 조회는 공개(선택적 인증)
                        .requestMatchers(HttpMethod.POST, "/api/ratings/recompute-all").hasRole("ADMIN") // 배치는 어드민만
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 어드민 배치 수동 트리거 등
                        .anyRequest().authenticated()                 // 그 외는 인증 필요
                )
                // 인증 안 됨 → 401, 인증됐지만 권한 부족 → 403 (둘 다 JSON)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                // 스프링 기본 인증 필터 앞에 우리 JWT 필터를 끼워, 요청을 먼저 인증 처리
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
