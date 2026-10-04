package com.acme.admin.security;

import com.acme.admin.domain.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public class AccountPrincipal extends User {
    private final String credentialVersion;
    public AccountPrincipal(UserAccount account, Collection<? extends GrantedAuthority> authorities) {
        super(account.username(), account.passwordHash(), account.enabled(), true, true, true, authorities);
        credentialVersion = fingerprint(account.passwordHash());
    }
    public String credentialVersion() { return credentialVersion; }
    private static String fingerprint(String value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
