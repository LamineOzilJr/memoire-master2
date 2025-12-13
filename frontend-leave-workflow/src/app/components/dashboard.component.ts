import { Component, OnInit, Inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { SoldeModalComponent } from '../solde-modal/solde-modal.component';
import { UserService } from '../services/user.service';
import { SoldeService } from '../services/solde.service';
import { User } from '../models/user';
import { HolidaysService, PublicHoliday } from '../services/holidays.service';
import { SenegalHolidaysService, SenegalHoliday } from '../services/senegal-holidays.service';
import { NotificationService } from '../services/notification.service';
import { NotificationResponse } from '../models/notification-response';
import { Subject, takeUntil, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { LeaveRequestService } from '../services/leave-request.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  userRole: string | null = null;
  userPrenom: string | null = null;
  currentUser?: User;
  yearsOfService = 0;
  yearsOfServiceText = '';
  soldeTotal = 0;

  // notifications
  unreadCount = 0;
  notifications: NotificationResponse[] = [];
  showNotificationsDropdown = false;

  // Manager KPIs
  managerTotalEmployees = 0;
  managerActiveEmployees = 0;
  managerPendingDemandes = 0;
  managerTotalAbsents = 0;

  months = ['Janvier','Février','Mars','Avril','Mai','Juin','Juillet','Août','Septembre','Octobre','Novembre','Décembre'];
  monthIndex = new Date().getMonth();
  holidaysByMonth = new Map<number, PublicHoliday[]>();
  senegalHolidaysByMonth = new Map<number, SenegalHoliday[]>();

  private destroy$ = new Subject<void>();
  private pollingSub: any;

  constructor(
    private authService: AuthService,
    private dialog: MatDialog,
    private router: Router,
    private userService: UserService,
    private soldeService: SoldeService,
    @Inject(HolidaysService) private holidaysService: HolidaysService,
    private senegalHolidaysService: SenegalHolidaysService,
    private notificationService: NotificationService,
    private leaveRequestService: LeaveRequestService
  ) {}

  ngOnInit() {
    this.userRole = this.authService.getUserRole();
    this.userPrenom = this.authService.getUserPrenom();

    // Load notifications
    this.loadNotifications();

    // Poll notifications every 15 seconds
    this.pollingSub = interval(15000).pipe(
      switchMap(() => this.notificationService.getUnreadCount())
    ).subscribe({ next: c => this.unreadCount = c || 0, error: () => {} });

    // Load current user for dynamic info panel
    this.userService.getCurrentUser().subscribe(
      user => {
        if (user) {
          this.currentUser = user;
          if (user?.createdAt) {
            const created = new Date(user.createdAt);
            const now = new Date();
            let years = now.getFullYear() - created.getFullYear();
            const m = now.getMonth() - created.getMonth();
            if (m < 0 || (m === 0 && now.getDate() < created.getDate())) {
              years--;
            }
            this.yearsOfService = Math.max(0, years);

            // Set years of service text
            if (this.yearsOfService === 0) {
              this.yearsOfServiceText = 'Moins d\'un an d\'expérience';
            } else {
              this.yearsOfServiceText = `${this.yearsOfService} an${this.yearsOfService > 1 ? 's' : ''} de service`;
            }
          }
        }
      },
      error => {
        console.error('Error loading current user:', error);
        this.yearsOfServiceText = '—';
      }
    );

    // Load solde total to control add request availability
    this.soldeService.getMyTotal().subscribe(
      total => {
        this.soldeTotal = total || 0;
      },
      error => {
        console.error('Error loading solde:', error);
        this.soldeTotal = 0;
      }
    );

    // Load Senegal holidays and group by month
    this.loadSenegalHolidays();

    // Load public holidays for Senegal and group by month
    const year = new Date().getFullYear();
    this.holidaysService.getSenegalHolidays(year).subscribe(
      (list: PublicHoliday[]) => {
        if (list && list.length > 0) {
          const grouped = new Map<number, PublicHoliday[]>();
          list.forEach((h: PublicHoliday) => {
            const d = new Date(h.date);
            const month = d.getMonth();
            const arr = grouped.get(month) || [];
            arr.push(h);
            grouped.set(month, arr);
          });
          this.holidaysByMonth = grouped;
        }
      },
      error => {
        console.error('Error loading holidays:', error);
      }
    );

    // after setting userRole and userPrenom, fetch manager stats if applicable
    if (this.userRole === 'MANAGER') {
      this.userService.getManagerStats().subscribe(
        stats => {
          this.managerTotalEmployees = stats?.totalEmployees || 0;
          this.managerActiveEmployees = stats?.activeEmployees || 0;
          this.managerPendingDemandes = stats?.totalDemandesEnAttente || 0;
          this.managerTotalAbsents = stats?.totalAbsents || 0;
        },
        err => {
          console.error('Error loading manager stats:', err);
        }
      );
    }

    // subscribe to external refresh requests
    this.notificationService.refresh$.pipe(takeUntil(this.destroy$)).subscribe(() => this.loadNotifications());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.pollingSub) {
      this.pollingSub.unsubscribe();
    }
  }

  loadSenegalHolidays() {
    try {
      const allHolidays = this.senegalHolidaysService.getAllHolidays();
      if (allHolidays && allHolidays.length > 0) {
        const grouped = new Map<number, any[]>();

        allHolidays.forEach(holiday => {
          const month = holiday.month - 1; // Convert to 0-based month
          const arr = grouped.get(month) || [];
          arr.push(holiday);
          grouped.set(month, arr);
        });

        this.senegalHolidaysByMonth = grouped;
      }
    } catch (error) {
      console.error('Error loading Senegal holidays:', error);
    }
  }

  loadNotifications() {
    // fetch unread count and last notifications
    this.notificationService.getUnreadCount().subscribe(
      count => this.unreadCount = count || 0,
      err => console.error('Error loading unread count', err)
    );

    this.notificationService.getNotifications().subscribe(
      list => this.notifications = list || [],
      err => console.error('Error loading notifications', err)
    );
  }

  toggleNotifications() {
    this.showNotificationsDropdown = !this.showNotificationsDropdown;
    if (this.showNotificationsDropdown) {
      // Optionally mark all as read when opening dropdown; or user can click each item to mark
      // For now we'll just refresh list
      this.loadNotifications();
    }
  }

  markNotificationRead(n: NotificationResponse) {
    // If notification has a linked demande, load it first
    const handleAfterFetch = () => {
      this.notificationService.markAsRead(n.id).subscribe(
        () => {
          // remove from list and update count
          this.notifications = this.notifications.filter(notif => notif.id !== n.id);
          this.unreadCount = Math.max(0, this.unreadCount - 1);
          this.showNotificationsDropdown = false;
          console.log('✅ Notification marked as read and removed from list');
        },
        err => console.error('Error marking notification as read', err)
      );
    };

    if (n?.targetId) {
      // fetch demande details and show quick info before marking as read
      this.leaveRequestService.getRequestById(n.targetId).subscribe(
        req => {
          // Display minimal info - you can replace this with a dialog component
          alert(`Demande #${req.id}\nEmployé: ${req.userName}\nType: ${req.typeCongeLibelle}\nPériode: ${req.dateDebut} → ${req.dateFin}\nStatut Manager: ${req.statutManager}`);
          handleAfterFetch();
        },
        err => {
          console.error('Error loading related demande', err);
          // still mark as read even if fetching failed
          handleAfterFetch();
        }
      );
    } else {
      // no related target - simply mark as read
      handleAfterFetch();
    }
  }

  openSoldeModal() {
    this.dialog.open(SoldeModalComponent, { data: { total: this.soldeTotal } });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  navigateTo(page: string) {
    if (page === 'rh-requests') {
      this.router.navigate(['/rh-requests']);
      return;
    }
    if (page === 'chef-service-requests') {
      this.router.navigate(['/chef-service-requests']);
      return;
    }
    if (page === 'dg-requests') {
      this.router.navigate(['/dg-requests']);
      return;
    }
    if (page === 'absences') {
      this.router.navigate(['/absences']);
      return;
    }
    if (page === 'requests') {
      this.router.navigate(['/requests']);
      return;
    }
    // keep previous behavior for pages not implemented yet
    alert(`La page "${page}" sera disponible prochainement.`);
    console.log('Navigation vers:', page);
  }

  prevMonth() {
    this.monthIndex = (this.monthIndex + 11) % 12;
  }

  nextMonth() {
    this.monthIndex = (this.monthIndex + 1) % 12;
  }

  get holidaysForCurrentMonth(): PublicHoliday[] {
    return this.holidaysByMonth.get(this.monthIndex) || [];
  }

  getSenegalHolidaysForCurrentMonth(): any[] {
    if (!this.senegalHolidaysByMonth || this.senegalHolidaysByMonth.size === 0) {
      return [];
    }
    return this.senegalHolidaysByMonth.get(this.monthIndex) || [];
  }
}
