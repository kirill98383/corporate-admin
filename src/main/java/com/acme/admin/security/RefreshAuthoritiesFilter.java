package com.acme.admin.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reloads account state on each request: role revocations do not await a new login. */
public class RefreshAuthoritiesFilter extends OncePerRequestFilter {
    private final DatabaseUserDetailsService details;
    public RefreshAuthoritiesFilter(DatabaseUserDetailsService details) { this.details=details; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AccountPrincipal previous) {
            try {
                var current=details.loadUserByUsername(auth.getName());
                if (!current.isEnabled() || !current.credentialVersion().equals(previous.credentialVersion())) {
                    invalidate(request); response.sendRedirect(request.getContextPath()+"/login?expired"); return;
                }
                current.eraseCredentials();
                var refreshed=UsernamePasswordAuthenticationToken.authenticated(current, null, current.getAuthorities());
                refreshed.setDetails(auth.getDetails());
                var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(refreshed);
                SecurityContextHolder.setContext(context);
            } catch (UsernameNotFoundException ex) {
                invalidate(request); response.sendRedirect(request.getContextPath()+"/login?expired"); return;
            }
        }
        chain.doFilter(request, response);
    }
    private void invalidate(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session=request.getSession(false); if(session!=null) session.invalidate();
    }
}
