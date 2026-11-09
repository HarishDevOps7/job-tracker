
package com.harish.job_tracker.config;

import com.harish.job_tracker.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.harish.job_tracker.service.GoogleOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.function.Supplier;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserAuthProvider;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.repository.UserAuthProviderRepository;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // ==========================================
    // PASSWORD ENCODER
    // ==========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==========================================
    // AUTHENTICATION PROVIDER
    // ==========================================

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    // ==========================================
    // AUTHENTICATION MANAGER
    // ==========================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }


@Bean
public GrantedAuthoritiesMapper oauth2AuthoritiesMapper(
        UserAuthProviderRepository providerRepository,
        UserRepository userRepository) {

    return authorities -> {

        Set<GrantedAuthority> mappedAuthorities =
                new HashSet<>(authorities);

        for (GrantedAuthority authority : authorities) {

            if (authority instanceof OidcUserAuthority oidcAuthority) {

                String googleSubject =
                        oidcAuthority.getIdToken().getSubject();

                UserAuthProvider provider =
                        providerRepository
                                .findByProviderAndProviderUserId(
                                        "GOOGLE",
                                        googleSubject
                                )
                                .orElseThrow(() ->
                                        new OAuth2AuthenticationException(
                                                new OAuth2Error("invalid_user"),
                                                "Google account is not linked to an application user"
                                        )
                                );

                Long userId = provider.getUser().getId();

                User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                                new OAuth2AuthenticationException(
                                        new OAuth2Error("user_not_found"),
                                        "Application user not found"
                                )
                        );

                mappedAuthorities.add(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                );
            }
        }

        return mappedAuthorities;
    };
}




    // ==========================================
    // SECURITY FILTER CHAIN
    // ==========================================

   @Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        DaoAuthenticationProvider authenticationProvider,
        GoogleOAuth2UserService googleOAuth2UserService,
        GrantedAuthoritiesMapper oauth2AuthoritiesMapper)
        throws Exception {

        http

            .authenticationProvider(authenticationProvider)

            // CSRF protection
            .csrf(csrf -> csrf
                    .csrfTokenRepository(
                            CookieCsrfTokenRepository.withHttpOnlyFalse()
                    )
                    .csrfTokenRequestHandler(
                            new SpaCsrfTokenRequestHandler()
                    )
            )

            // CORS
            .cors(Customizer.withDefaults())

            // URL authorization
            .authorizeHttpRequests(auth -> auth

                    // Public endpoints
                    .requestMatchers(
                            "/",
                            "/login",
                            "/register",
                            "/error",
                            "/api/auth/**",
                            "/oauth2/**",
                            "/login/oauth2/**",
                            "/css/**",
                            "/js/**",
                            "/images/**"
                    ).permitAll()

                    // Admin-only endpoints
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/admin/**").hasRole("ADMIN")

                    // Other API endpoints require authentication
                    .requestMatchers("/api/**").authenticated()

                    // Remaining endpoints
                    .anyRequest().authenticated()
            )

            // ======================================
            // FORM LOGIN
            // ======================================

            .formLogin(form -> form

                    .loginPage("/login")
                    .loginProcessingUrl("/login")

                    .successHandler((request, response, authentication) -> {

                        boolean isAdmin = authentication.getAuthorities()
                                .stream()
                                .anyMatch(authority ->
                                        authority.getAuthority()
                                                .equals("ROLE_ADMIN")
                                );

                        if (isAdmin) {
                            response.sendRedirect("/admin/dashboard");
                        } else {
                            response.sendRedirect("/dashboard");
                        }
                    })

                    .failureUrl("/login?error=true")
                    .permitAll()
            )

            // ======================================
            // GOOGLE OAUTH2 LOGIN
            // ======================================

            // ======================================
// GOOGLE OAUTH2 LOGIN
// ======================================

.oauth2Login(oauth2 -> oauth2

        .userInfoEndpoint(userInfo -> userInfo

                .oidcUserService(googleOAuth2UserService)

                .userAuthoritiesMapper(oauth2AuthoritiesMapper)
        )

        .defaultSuccessUrl(
                "http://localhost:5173/dashboard",
                true
        )

        .failureUrl(
                "http://localhost:5173/login?oauth2Error=true"
        )
)
            // HTTP Basic for API/Postman testing
            .httpBasic(Customizer.withDefaults())

            // API authentication errors
            .exceptionHandling(exception -> exception

                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(
                                    HttpStatus.UNAUTHORIZED
                            ),

                            PathPatternRequestMatcher.withDefaults()
                                    .matcher("/api/**")
                    )
            )

            // Logout
            .logout(logout -> logout

                    .logoutSuccessUrl("/login?logout=true")
                    .permitAll()
            );

        return http.build();
    }

    // ==========================================
    // CORS CONFIGURATION
    // ==========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "X-XSRF-TOKEN",
                "Authorization"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // ==========================================
    // SPA CSRF HANDLER
    // ==========================================

    private static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestAttributeHandler plain =
                new CsrfTokenRequestAttributeHandler();

        private final XorCsrfTokenRequestAttributeHandler xor =
                new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {

            xor.handle(request, response, csrfToken);

            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                CsrfToken csrfToken) {

            String headerValue = request.getHeader(
                    csrfToken.getHeaderName()
            );

            return (StringUtils.hasText(headerValue) ? plain : xor)
                    .resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
