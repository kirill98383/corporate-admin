package com.acme.admin.domain;

public enum Permission {
    USER_READ("Просмотр пользователей"), USER_WRITE("Управление пользователями"),
    ROLE_READ("Просмотр ролей"), ROLE_WRITE("Управление ролями"), AUDIT_READ("Просмотр аудита");
    private final String label;
    Permission(String label) { this.label = label; }
    public String getLabel() { return label; }
}
