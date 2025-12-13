package sn.groupeisi.leaveworkflow.enums;

public enum StatutChefService {
    EN_ATTENTE("En attente"),
    VALIDER("Validé"),
    REJETE("Rejeté"),
    PLUS_D_INFOS("Plus d'infos");

    private final String displayName;

    StatutChefService(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

