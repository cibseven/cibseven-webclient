package org.cibseven.webapp.rest.model;

import java.util.Objects;

public class KeyTenant {
    private String key;
    private String tenantId;

    public KeyTenant(String key, String tenantId) {
        this.key = key;
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyTenant that = (KeyTenant) o;
        return Objects.equals(key, that.key) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, tenantId);
    }
}
