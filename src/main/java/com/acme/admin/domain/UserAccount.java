package com.acme.admin.domain;

import java.util.Set;

public record UserAccount(long id, String username, String displayName, String passwordHash,
                          boolean enabled, Set<Long> roleIds) {}
