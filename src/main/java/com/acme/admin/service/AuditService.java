package com.acme.admin.service;

import com.acme.admin.domain.AuditEvent;
import com.acme.admin.repository.AuditRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditService {
    private final AuditRepository repository;
    public AuditService(AuditRepository repository) { this.repository = repository; }
    public void record(String action, String target) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        repository.append(auth == null ? "SYSTEM" : auth.getName(), action, target);
    }
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public List<AuditEvent> latest() { return repository.latest(); }
}
