package sn.groupeisi.leaveworkflow.enums;

public enum StatutManager {
    EN_ATTENTE("En attente"),
    APPROUVE("Approuvé"),
    REJETE("Rejeté"),
    PLUS_D_INFOS("Plus d'infos");

    private final String displayName;

    StatutManager(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}