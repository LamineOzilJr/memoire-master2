package sn.groupeisi.leaveworkflow.enums;

public enum StatutRh {
    EN_ATTENTE("En attente"),
    VALIDER("Validé"),
    REJETE("Rejeté"),
    PLUS_D_INFOS("Plus d'infos");

    private final String displayName;

    StatutRh(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}