package id.ac.ui.cs.advprog.gateway.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String API_AUTH_CAPTCHA = "/api/auth/captcha";
    private static final String API_DB_PING = "/api/db/ping";
    private static final String API_AUTH_REGISTER = "/api/auth/register";
    private static final String API_AUTH_LOGIN = "/api/auth/login";
    private static final String API_AUTH_REFRESH = "/api/auth/refresh";
    private static final String API_AUTH_VERIFY_EMAIL = "/api/auth/verify-email";
    private static final String API_AUTH_VERIFY_MFA = "/api/auth/2fa/verify";
    private static final String API_PUBLIC_PROFILE = "/api/users/*/public-profile";
    private static final String API_GATEWAY_AUTH_GRPC_STATUS = "/api/gateway/internal/auth-grpc/status";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(final ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                API_AUTH_CAPTCHA,
                                API_DB_PING,
                                API_GATEWAY_AUTH_GRPC_STATUS
                        ).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                HttpMethod.POST,
                                API_AUTH_REGISTER,
                                API_AUTH_LOGIN,
                                API_AUTH_REFRESH,
                                API_AUTH_VERIFY_EMAIL,
                                API_AUTH_VERIFY_MFA
                        ).permitAll()
                        .pathMatchers(HttpMethod.GET, API_PUBLIC_PROFILE).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-origin}") final String frontendOrigin
    ) {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
