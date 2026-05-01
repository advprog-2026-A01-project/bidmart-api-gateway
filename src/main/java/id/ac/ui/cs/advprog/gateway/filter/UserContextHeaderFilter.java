package id.ac.ui.cs.advprog.gateway.filter;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserContextHeaderFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_PERMISSIONS = "X-User-Permissions";
    private static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_PERMISSIONS = "permissions";

    private static final List<String> USER_CONTEXT_HEADERS = List.of(
            HEADER_USER_ID,
            HEADER_USERNAME,
            HEADER_USER_ROLE,
            HEADER_USER_PERMISSIONS,
            HEADER_GATEWAY_SECRET
    );

    private final String gatewaySecret;

    public UserContextHeaderFilter(@Value("${app.gateway-secret}") final String gatewaySecret) {
        this.gatewaySecret = gatewaySecret;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
        final ServerWebExchange sanitizedExchange = withGatewaySecretOnly(exchange);

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> chain.filter(withJwtContext(exchange, auth.getToken())))
                .switchIfEmpty(chain.filter(sanitizedExchange));
    }

    private ServerWebExchange withGatewaySecretOnly(final ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    clearGatewayHeaders(headers);
                    headers.set(HEADER_GATEWAY_SECRET, gatewaySecret);
                }))
                .build();
    }

    private ServerWebExchange withJwtContext(final ServerWebExchange exchange, final Jwt jwt) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    clearGatewayHeaders(headers);
                    headers.set(HEADER_GATEWAY_SECRET, gatewaySecret);
                    setIfPresent(headers, HEADER_USER_ID, jwt.getSubject());
                    setIfPresent(headers, HEADER_USERNAME, jwt.getClaimAsString(CLAIM_USERNAME));
                    setIfPresent(headers, HEADER_USER_ROLE, jwt.getClaimAsString(CLAIM_ROLE));
                    headers.set(HEADER_USER_PERMISSIONS, permissionsAsHeaderValue(jwt));
                }))
                .build();
    }

    private static void clearGatewayHeaders(final MultiValueMap<String, String> headers) {
        USER_CONTEXT_HEADERS.forEach(headers::remove);
    }

    private static void setIfPresent(
            final MultiValueMap<String, String> headers,
            final String name,
            final String value
    ) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private static String permissionsAsHeaderValue(final Jwt jwt) {
        final Object rawPermissions = jwt.getClaim(CLAIM_PERMISSIONS);
        if (rawPermissions instanceof Collection<?> permissions) {
            final StringJoiner joiner = new StringJoiner(",");
            permissions.stream()
                    .map(String::valueOf)
                    .filter(permission -> !permission.isBlank())
                    .forEach(joiner::add);
            return joiner.toString();
        }
        return rawPermissions == null ? "" : String.valueOf(rawPermissions);
    }
}
