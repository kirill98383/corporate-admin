package com.acme.admin.repository;

import com.acme.admin.domain.AuditEvent;
import java.util.List;

public interface AuditRepository {
    void append(String actor, String action, String target);
    List<AuditEvent> latest();
}
