package sn.groupeisi.leaveworkflow.enums;

public enum StatutDg {
    EN_ATTENTE("En attente"),
    VALIDER("Validé"),
    REJETE("Rejeté"),
    PLUS_D_INFOS("Plus d'infos");

    private final String displayName;

    StatutDg(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

