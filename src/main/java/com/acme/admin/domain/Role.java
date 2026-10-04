package com.acme.admin.domain;

import java.util.Set;

public record Role(long id, String name, boolean systemRole, Set<Permission> permissions) {}
