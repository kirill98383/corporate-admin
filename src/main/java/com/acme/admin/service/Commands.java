package com.acme.admin.service;

import com.acme.admin.domain.Permission;
import jakarta.validation.constraints.*;
import java.util.Set;

public final class Commands {
    private Commands() {}
    public record CreateUser(@NotBlank @Pattern(regexp="[a-z0-9._-]{3,64}") String username,
        @NotBlank @Size(max=100) String displayName, @NotBlank @Size(min=12,max=64) String password,
        @NotEmpty Set<@NotNull Long> roleIds) {}
    public record UpdateUser(@NotBlank @Size(max=100) String displayName, boolean enabled,
        @NotEmpty Set<@NotNull Long> roleIds) {}
    public record SaveRole(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{2,63}") String name,
        @NotEmpty Set<@NotNull Permission> permissions) {}
    public record ResetPassword(@NotBlank @Size(min=12,max=64) String password) {}
}
