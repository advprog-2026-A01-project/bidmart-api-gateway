package id.ac.ui.cs.advprog.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private static final String AUTH_ROUTE_ID = "auth-service";
    private static final String CATALOG_ROUTE_ID = "catalog-service";
    private static final String AUCTION_WALLET_ROUTE_ID = "auction-wallet-service";

    @Bean
    public RouteLocator bidmartRoutes(
            final RouteLocatorBuilder builder,
            @Value("${services.auth.base-url}") final String authServiceBaseUrl,
            @Value("${services.catalog.base-url}") final String catalogServiceBaseUrl,
            @Value("${services.auction-wallet.base-url}") final String auctionWalletServiceBaseUrl
    ) {
        return builder.routes()
                .route(AUTH_ROUTE_ID, route -> route
                        .path(
                                "/api/auth/**",
                                "/api/admin/**",
                                "/api/rbac/**",
                                "/api/db/**",
                                "/api/users/**"
                        )
                        .uri(authServiceBaseUrl))
                .route(CATALOG_ROUTE_ID, route -> route
                        .path(
                                "/api/catalog/**",
                                "/api/categories/**",
                                "/api/listings/**"
                        )
                        .uri(catalogServiceBaseUrl))
                .route(AUCTION_WALLET_ROUTE_ID, route -> route
                        .path(
                                "/api/auctions/**",
                                "/api/bids/**",
                                "/api/wallet/**"
                        )
                        .uri(auctionWalletServiceBaseUrl))
                .build();
    }
}
