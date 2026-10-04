package com.acme.admin.repository;

import com.acme.admin.domain.AuditEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcAuditRepository implements AuditRepository {
    private final JdbcTemplate jdbc;
    public JdbcAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void append(String actor, String action, String target) {
        jdbc.update("INSERT INTO audit_events(actor, action, target) VALUES (?, ?, ?)", actor, action, target);
    }
    public List<AuditEvent> latest() {
        return jdbc.query("SELECT * FROM audit_events ORDER BY id DESC LIMIT 200", (rs, n) -> new AuditEvent(rs.getLong("id"), rs.getObject("occurred_at", OffsetDateTime.class), rs.getString("actor"), rs.getString("action"), rs.getString("target")));
    }
}
