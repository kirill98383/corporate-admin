package com.acme.admin.service;

import com.acme.admin.domain.*;
import com.acme.admin.repository.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@Service
@Validated
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final AuditService audit;
    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder passwords, AuditService audit) {
        this.users=users; this.roles=roles; this.passwords=passwords; this.audit=audit;
    }
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<UserAccount> list() { return users.findAll(); }
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public UserAccount get(long id) { return users.findById(id).orElseThrow(() -> new DomainException("Пользователь не найден")); }
    private void checkRoles(Set<Long> ids) {
        if (ids.stream().anyMatch(id -> roles.findById(id).isEmpty())) throw new DomainException("Неизвестная роль");
    }
    private String encode(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new DomainException("Пароль не должен превышать 72 байта UTF-8");
        return passwords.encode(password);
    }
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public void create(@Valid Commands.CreateUser command) {
        users.lockSecurityChanges(); checkRoles(command.roleIds());
        users.create(command.username(), command.displayName(), encode(command.password()), command.roleIds());
        audit.record("USER_CREATED", command.username());
    }
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public void update(long id, @Valid Commands.UpdateUser command) {
        users.lockSecurityChanges(); checkRoles(command.roleIds());
        var user = get(id);
        users.update(id, command.displayName(), command.enabled(), command.roleIds());
        if (users.activeAdministrators()==0) throw new DomainException("Нельзя отключить или лишить роли последнего администратора");
        audit.record("USER_UPDATED", user.username());
    }
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public void resetPassword(long id, @Valid Commands.ResetPassword command) {
        users.lockSecurityChanges();
        var user = get(id);
        users.changePassword(id, encode(command.password()));
        audit.record("PASSWORD_RESET", user.username());
    }
}
