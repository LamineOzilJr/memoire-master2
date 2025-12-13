package sn.groupeisi.leaveworkflow.util;

import sn.groupeisi.leaveworkflow.enums.Role;

/**
 * Utility class for generating matricules automatically based on role.
 *
 * Format: [ROLE_PREFIX][SEQUENCE_NUMBER]
 * Examples:
 * - SALARIE: SAL001, SAL002, SAL003, ...
 * - MANAGER: MAN001, MAN002, ...
 * - CHEF_SERVICE: CS001, CS002, ...
 * - DG: DG001, DG002, ...
 * - SERVICE_RH: RH001, RH002, ...
 * - ADMIN: ADM001, ADM002, ...
 */
public class MatriculeGenerator {

    /**
     * Get the prefix for a given role
     */
    public static String getPrefixForRole(Role role) {
        if (role == null) {
            return "SAL"; // Default to SALARIE
        }

        return switch (role) {
            case SALARIE -> "SAL";
            case MANAGER -> "MAN";
            case CHEF_SERVICE -> "CS";
            case DG -> "DG";
            case SERVICE_RH -> "RH";
            case ADMIN -> "ADM";
        };
    }

    /**
     * Generate matricule based on role and sequence number
     *
     * @param role The role of the user
     * @param sequenceNumber The next sequence number (e.g., 1, 2, 3...)
     * @return The generated matricule (e.g., SAL001, MAN002)
     */
    public static String generateMatricule(Role role, long sequenceNumber) {
        String prefix = getPrefixForRole(role);
        return String.format("%s%03d", prefix, sequenceNumber);
    }

    /**
     * Extract sequence number from matricule
     *
     * @param matricule The matricule string
     * @return The sequence number or 0 if invalid
     */
    public static long extractSequenceFromMatricule(String matricule) {
        if (matricule == null || matricule.isEmpty()) {
            return 0;
        }

        try {
            // Remove the role prefix (first 2-3 characters depending on role)
            String numericPart;

            if (matricule.startsWith("SAL")) {
                numericPart = matricule.substring(3);
            } else if (matricule.startsWith("MAN")) {
                numericPart = matricule.substring(3);
            } else if (matricule.startsWith("CS")) {
                numericPart = matricule.substring(2);
            } else if (matricule.startsWith("DG")) {
                numericPart = matricule.substring(2);
            } else if (matricule.startsWith("RH")) {
                numericPart = matricule.substring(2);
            } else if (matricule.startsWith("ADM")) {
                numericPart = matricule.substring(3);
            } else {
                return 0;
            }

            return Long.parseLong(numericPart);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}

