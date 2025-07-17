package io.github.anonventions.applydcu.api;

/**
 * Application status enumeration
 */
public enum ApplicationStatus {
    PENDING("in progress"),
    ACCEPTED("accepted"),
    DENIED("denied"),
    EXPIRED("expired"),
    INACTIVE("inactive");
    
    private final String legacyValue;
    
    ApplicationStatus(String legacyValue) {
        this.legacyValue = legacyValue;
    }
    
    public String getLegacyValue() {
        return legacyValue;
    }
    
    public static ApplicationStatus fromLegacyValue(String value) {
        for (ApplicationStatus status : values()) {
            if (status.legacyValue.equals(value)) {
                return status;
            }
        }
        return PENDING;
    }
}