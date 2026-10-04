package com.acme.admin.service;

import com.acme.admin.domain.Role;
import com.acme.admin.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Service
@Validated
public class RoleService {
    private final RoleRepository roles;
    private final UserRepository users;
    private final AuditService audit;
    public RoleService(RoleRepository roles, UserRepository users, AuditService audit) { this.roles=roles; this.users=users; this.audit=audit; }
    @PreAuthorize("hasAnyAuthority('ROLE_READ', 'USER_WRITE')")
    public List<Role> list() { return roles.findAll(); }
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public Role get(long id) { return roles.findById(id).orElseThrow(() -> new DomainException("Роль не найдена")); }
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public void create(@Valid Commands.SaveRole command) {
        users.lockSecurityChanges();
        roles.create(command.name(), command.permissions());
        audit.record("ROLE_CREATED", command.name());
    }
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public void update(long id, @Valid Commands.SaveRole command) {
        users.lockSecurityChanges();
        if (get(id).systemRole()) throw new DomainException("Системную роль ADMIN нельзя изменять");
        roles.update(id, command.name(), command.permissions());
        audit.record("ROLE_UPDATED", command.name());
    }
}
