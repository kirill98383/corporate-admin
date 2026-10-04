package com.acme.admin.repository;

import com.acme.admin.domain.UserAccount;
import java.util.*;

public interface UserRepository {
    List<UserAccount> findAll();
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findById(long id);
    void create(String username, String displayName, String passwordHash, Set<Long> roles);
    void update(long id, String displayName, boolean enabled, Set<Long> roles);
    void changePassword(long id, String passwordHash);
    long count();
    long activeAdministrators();
    void lockSecurityChanges();
}
