package com.codeXray.backend.config;

import com.codeXray.backend.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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

    // CORS: 허용 오리진을 env(APP_CORS_ORIGINS, 콤마 구분)로 주입.
    // 비어 있으면 교차 오리진 없음(= 프론트/백엔드 same-origin 배포 전제). 자격증명(쿠키) 허용.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        if (!allowedOrigins.isBlank()) {
            config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // corsConfigurationSource 빈 사용
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // 가입/로그인 등은 열어둠
                        .requestMatchers(HttpMethod.GET, "/api/problems/**").permitAll() // 문제 조회는 공개
                        .requestMatchers(HttpMethod.GET, "/api/tags").permitAll() // 태그 목록 조회는 공개
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
