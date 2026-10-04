package com.acme.admin.config;

import com.acme.admin.security.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http, DatabaseUserDetailsService details) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/error").permitAll()
                .requestMatchers("/users/new", "/users/*/edit", "/users/*/password").hasAuthority("USER_WRITE")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/**").hasAuthority("USER_WRITE")
                .requestMatchers("/users", "/users/**").hasAuthority("USER_READ")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/roles/**").hasAuthority("ROLE_WRITE")
                .requestMatchers("/roles/new", "/roles/*/edit").hasAuthority("ROLE_WRITE")
                .requestMatchers("/roles", "/roles/**").hasAuthority("ROLE_READ")
                .requestMatchers("/audit").hasAuthority("AUDIT_READ").anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; style-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'")))
            .addFilterBefore(new RefreshAuthoritiesFilter(details), AuthorizationFilter.class)
            .build();
    }
}
