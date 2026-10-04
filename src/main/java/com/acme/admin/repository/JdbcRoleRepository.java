package com.acme.admin.repository;

import com.acme.admin.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class JdbcRoleRepository implements RoleRepository {
    private final JdbcTemplate jdbc;
    public JdbcRoleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private Set<Permission> permissions(String sql, Object... args) {
        return jdbc.queryForList(sql, String.class, args).stream().map(Permission::valueOf).collect(Collectors.toSet());
    }
    public List<Role> findAll() {
        return jdbc.query("SELECT * FROM roles ORDER BY name", (rs, row) -> new Role(rs.getLong("id"), rs.getString("name"), rs.getBoolean("system_role"),
            permissions("SELECT permission FROM role_permissions WHERE role_id=?", rs.getLong("id"))));
    }
    public Optional<Role> findById(long id) { return findAll().stream().filter(r -> r.id()==id).findFirst(); }
    public void create(String name, Set<Permission> permissions) {
        jdbc.update("INSERT INTO roles(name, system_role) VALUES (?, FALSE)", name);
        assign(jdbc.queryForObject("SELECT id FROM roles WHERE name=?", Long.class, name), permissions);
    }
    public void update(long id, String name, Set<Permission> permissions) {
        jdbc.update("UPDATE roles SET name=? WHERE id=?", name, id);
        assign(id, permissions);
    }
    private void assign(long id, Set<Permission> permissions) {
        jdbc.update("DELETE FROM role_permissions WHERE role_id=?", id);
        permissions.forEach(p -> jdbc.update("INSERT INTO role_permissions(role_id, permission) VALUES (?, ?)", id, p.name()));
    }
    public Set<Permission> permissionsFor(String username) {
        return permissions("SELECT DISTINCT rp.permission FROM role_permissions rp JOIN user_roles ur ON ur.role_id=rp.role_id JOIN app_users u ON u.id=ur.user_id WHERE u.username=? AND u.enabled=TRUE", username);
    }
}
