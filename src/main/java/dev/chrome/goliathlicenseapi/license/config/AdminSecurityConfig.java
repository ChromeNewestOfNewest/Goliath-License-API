package dev.chrome.goliathlicenseapi.license.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import dev.chrome.goliathlicenseapi.license.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.session.SessionManagementFilter;

@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AdminUserRepository adminUserRepository) throws Exception {
        AdminSessionValidationFilter sessionValidationFilter = new AdminSessionValidationFilter(adminUserRepository);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(org.springframework.security.config.Customizer.withDefaults()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/auth/login", "/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .addFilterBefore(sessionValidationFilter, SessionManagementFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/admin/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Admin access required.\"}");
                        }));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String adminOrigin = System.getenv().getOrDefault("APP_ADMIN_ORIGIN", "");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration adminConfig = new CorsConfiguration();
        if (adminOrigin != null && !adminOrigin.isBlank()) {
            adminConfig.addAllowedOrigin(adminOrigin);
        }
        adminConfig.addAllowedHeader("*");
        adminConfig.addAllowedMethod("GET");
        adminConfig.addAllowedMethod("POST");
        adminConfig.addAllowedMethod("PATCH");
        adminConfig.setAllowCredentials(true);
        source.registerCorsConfiguration("/api/v1/admin/**", adminConfig);

        CorsConfiguration defaultConfig = new CorsConfiguration();
        defaultConfig.addAllowedOriginPattern("*");
        defaultConfig.addAllowedHeader("*");
        defaultConfig.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", defaultConfig);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
