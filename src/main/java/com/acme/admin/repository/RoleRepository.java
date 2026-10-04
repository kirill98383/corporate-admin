package com.acme.admin.repository;

import com.acme.admin.domain.*;
import java.util.*;

public interface RoleRepository {
    List<Role> findAll();
    Optional<Role> findById(long id);
    void create(String name, Set<Permission> permissions);
    void update(long id, String name, Set<Permission> permissions);
    Set<Permission> permissionsFor(String username);
}
