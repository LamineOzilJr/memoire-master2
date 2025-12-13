package sn.groupeisi.leaveworkflow.enums;

public enum Role {
    SALARIE("Salarié"),
    MANAGER("Manager"),
    SERVICE_RH("Service RH"),
    CHEF_SERVICE("Chef de Service"),
    DG("Directeur Général"),
    ADMIN("Administrateur");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}