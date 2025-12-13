package sn.groupeisi.leaveworkflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.groupeisi.leaveworkflow.enums.StatutDg;
import sn.groupeisi.leaveworkflow.model.DemandeConge;
import sn.groupeisi.leaveworkflow.model.User;
import sn.groupeisi.leaveworkflow.repository.DemandeCongeRepository;
import sn.groupeisi.leaveworkflow.repository.UserRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scheduled service to automatically reactivate users when their leave period ends.
 * When a leave request is validated by RH, the user is marked as inactive (active = false).
 * This service runs daily to check if any inactive users have completed their leave period
 * and should be reactivated (active = true).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveSchedulerService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final UserRepository userRepository;

    /**
     * Scheduled task that runs every day at midnight (00:00) to deactivate users whose leave starts today.
     * Fetches all validated leave requests (StatutDg = VALIDER) with dateDebut = today
     * and deactivates users if they're not already inactive and have no other ongoing leaves.
     */
    @Scheduled(cron = "0 0 0 * * *")  // Runs daily at midnight
    @Transactional
    public void deactivateUsersForLeavesStartingToday() {
        log.info("🔄 Starting scheduled task: Deactivating users for leaves starting today...");

        LocalDate today = LocalDate.now();

        // Fetch all validated leave requests with dateDebut = today
        List<DemandeConge> leavesStartingToday = demandeCongeRepository.findByStatutDgAndDateDebutEquals(
                StatutDg.VALIDER,
                today
        );

        if (leavesStartingToday.isEmpty()) {
            log.info("✅ No validated leaves starting today found.");
            return;
        }

        log.info("📋 Found {} validated leaves starting today", leavesStartingToday.size());

        // Collect unique users from leaves starting today
        Set<User> usersToDeactivate = new HashSet<>();
        for (DemandeConge leave : leavesStartingToday) {
            User user = leave.getUser();
            if (user != null && Boolean.TRUE.equals(user.getActive())) {
                usersToDeactivate.add(user);
            }
        }

        log.info("👥 Identified {} users to deactivate", usersToDeactivate.size());

        // Deactivate users
        for (User user : usersToDeactivate) {
            user.setActive(false);
            userRepository.save(user);
            log.info("✅ Deactivated user: {} (ID: {}) for leave starting today", user.getFullName(), user.getId());
        }

        log.info("🎉 Daily deactivation task completed successfully!");
    }

    /**
     * Scheduled task that runs every day at 1:00 AM to reactivate users whose leave period has ended.
     * - Fetches all validated leave requests (StatutDg = VALIDER) with dateFin in the past
     * - Identifies unique users from these requests
     * - Verifies they have no other active validated leaves
     * - Reactivates them by setting active = true
     */
    @Scheduled(cron = "0 0 1 * * *")  // Runs daily at 1:00 AM (seconds, minutes, hours, day, month, dayOfWeek)
    @Transactional
    public void reactivateUsersAfterLeaveExpiry() {
        log.info("🔄 Starting scheduled task: Reactivating users after leave period expiry...");

        LocalDate today = LocalDate.now();

        // Fetch all validated leave requests with end date in the past
        List<DemandeConge> expiredValidatedLeaves = demandeCongeRepository.findByStatutDgAndDateFinBefore(
                StatutDg.VALIDER,
                today
        );

        if (expiredValidatedLeaves.isEmpty()) {
            log.info("✅ No expired validated leaves found. All active users have ongoing leaves or no validated leaves.");
            return;
        }

        log.info("📋 Found {} expired validated leave requests", expiredValidatedLeaves.size());

        // Collect unique users from expired leaves
        Set<User> usersToReactivate = new HashSet<>();
        for (DemandeConge leave : expiredValidatedLeaves) {
            User user = leave.getUser();
            if (user != null && !Boolean.TRUE.equals(user.getActive())) {
                usersToReactivate.add(user);
            }
        }

        log.info("👥 Identified {} users potentially needing reactivation", usersToReactivate.size());

        // Verify and reactivate users
        for (User user : usersToReactivate) {
            // Check if user has any other validated leave that's still ongoing
            List<DemandeConge> ongoingLeaves = demandeCongeRepository.findByUserAndStatutDgAndDateFinAfter(
                    user,
                    StatutDg.VALIDER,
                    today
            );

            if (ongoingLeaves.isEmpty()) {
                // No ongoing leaves - safe to reactivate
                user.setActive(true);
                userRepository.save(user);
                log.info("✅ Reactivated user: {} (ID: {})", user.getFullName(), user.getId());
            } else {
                log.info("ℹ️  User {} still has {} ongoing validated leave(s). Keeping inactive.",
                        user.getFullName(), ongoingLeaves.size());
            }
        }

        log.info("🎉 Scheduled reactivation task completed successfully!");
    }
}

