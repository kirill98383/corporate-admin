package com.acme.admin.security;

import com.acme.admin.repository.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    private final RoleRepository roles;
    public DatabaseUserDetailsService(UserRepository users, RoleRepository roles) { this.users=users; this.roles=roles; }
    @Override public AccountPrincipal loadUserByUsername(String username) {
        var user=users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return new AccountPrincipal(user, roles.permissionsFor(username).stream().map(p -> new SimpleGrantedAuthority(p.name())).toList());
    }
}
