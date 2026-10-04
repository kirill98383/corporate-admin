package com.acme.admin.repository;

import com.acme.admin.domain.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbc;
    public JdbcUserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private List<UserAccount> query(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new UserAccount(rs.getLong("id"), rs.getString("username"),
            rs.getString("display_name"), rs.getString("password_hash"), rs.getBoolean("enabled"),
            new HashSet<>(jdbc.queryForList("SELECT role_id FROM user_roles WHERE user_id = ?", Long.class, rs.getLong("id")))), args);
    }
    public List<UserAccount> findAll() { return query("SELECT * FROM app_users ORDER BY username"); }
    public Optional<UserAccount> findByUsername(String username) { return query("SELECT * FROM app_users WHERE username = ?", username).stream().findFirst(); }
    public Optional<UserAccount> findById(long id) { return query("SELECT * FROM app_users WHERE id = ?", id).stream().findFirst(); }
    public void create(String username, String displayName, String passwordHash, Set<Long> roles) {
        jdbc.update("INSERT INTO app_users(username, display_name, password_hash) VALUES (?, ?, ?)", username, displayName, passwordHash);
        assign(findByUsername(username).orElseThrow().id(), roles);
    }
    private void assign(long id, Set<Long> roles) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", id);
        roles.forEach(role -> jdbc.update("INSERT INTO user_roles(user_id, role_id) VALUES (?, ?)", id, role));
    }
    public void update(long id, String displayName, boolean enabled, Set<Long> roles) {
        jdbc.update("UPDATE app_users SET display_name = ?, enabled = ? WHERE id = ?", displayName, enabled, id);
        assign(id, roles);
    }
    public void changePassword(long id, String hash) { jdbc.update("UPDATE app_users SET password_hash = ? WHERE id = ?", hash, id); }
    public long count() { return jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Long.class); }
    public long activeAdministrators() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_users u JOIN user_roles ur ON u.id=ur.user_id JOIN roles r ON r.id=ur.role_id WHERE u.enabled=TRUE AND r.name='ADMIN'", Long.class);
    }
    public void lockSecurityChanges() { jdbc.queryForObject("SELECT id FROM security_lock WHERE id=1 FOR UPDATE", Integer.class); }
}
