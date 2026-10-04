package com.acme.admin.config;

import com.acme.admin.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AuditRepository audit;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    public BootstrapAdmin(UserRepository users, RoleRepository roles, AuditRepository audit, PasswordEncoder encoder,
        @Value("${app.bootstrap.username}") String username, @Value("${app.bootstrap.password}") String password) {
        this.users=users; this.roles=roles; this.audit=audit; this.encoder=encoder; this.username=username; this.password=password;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        users.lockSecurityChanges();
        if (users.count()>0) return;
        if (!username.matches("[a-z0-9._-]{3,64}") || password.length()<12 || password.length()>64 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Set ADMIN_USERNAME (3-64 lowercase characters) and ADMIN_PASSWORD (12-64 characters, at most 72 UTF-8 bytes) before first startup");
        long roleId=roles.findAll().stream().filter(r -> r.name().equals("ADMIN")).findFirst().orElseThrow().id();
        users.create(username, "Администратор", encoder.encode(password), Set.of(roleId));
        audit.append("SYSTEM", "ADMIN_BOOTSTRAPPED", username);
    }
}
