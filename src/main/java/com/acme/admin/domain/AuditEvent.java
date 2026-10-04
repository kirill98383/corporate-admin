package com.acme.admin.domain;

import java.time.OffsetDateTime;

public record AuditEvent(long id, OffsetDateTime occurredAt, String actor, String action, String target) {}
