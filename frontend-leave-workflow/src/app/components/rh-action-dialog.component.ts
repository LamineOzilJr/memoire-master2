import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { NotificationService } from '../services/notification.service';
import { SoldeService } from '../services/solde.service';

@Component({
  selector: 'app-rh-action-dialog',
  templateUrl: './rh-action-dialog.component.html',
  styleUrls: ['./rh-action-dialog.component.scss']
})
export class RhActionDialogComponent {
  request: LeaveRequestResponse;
  selectedAction: 'validate' | 'info' | 'reject' | null = null;
  comment = '';
  isLoading = false;

  constructor(
    public dialogRef: MatDialogRef<RhActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { request: LeaveRequestResponse },
    private leaveRequestService: LeaveRequestService,
    private snackBar: MatSnackBar,
    private notificationService: NotificationService,
    private soldeService: SoldeService
  ) {
    this.request = data.request;
  }

  selectAction(action: 'validate' | 'info' | 'reject') {
    this.selectedAction = action;
  }

  submit() {
    if (!this.selectedAction) {
      this.snackBar.open('Veuillez sélectionner une action', 'Fermer', {
        duration: 4000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    let request$;

    switch (this.selectedAction) {
      case 'validate':
        request$ = this.leaveRequestService.approveByRh(this.request.id, this.comment);
        break;
      case 'info':
        if (!this.comment || this.comment.trim() === '') {
          this.snackBar.open('Veuillez entrer les informations requises', 'Fermer', {
            duration: 4000,
            panelClass: ['error-snackbar']
          });
          return;
        }
        request$ = this.leaveRequestService.requestMoreInfoByRh(this.request.id, this.comment);
        break;
      case 'reject':
        if (!this.comment || this.comment.trim() === '') {
          this.snackBar.open('Veuillez entrer un motif de refus', 'Fermer', {
            duration: 4000,
            panelClass: ['error-snackbar']
          });
          return;
        }
        request$ = this.leaveRequestService.rejectByRh(this.request.id, this.comment);
        break;
    }

    this.isLoading = true;
    request$.subscribe(
      (updated: LeaveRequestResponse) => {
        this.isLoading = false;
        this.snackBar.open('Action effectuée avec succès', 'Fermer', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        // notify other parts of the app to refresh notifications and solde
        this.notificationService.notifyRefresh();
        // if it's a validated request and it's 3 days or more, refresh solde
        if (this.selectedAction === 'validate') {
          const jours = this.request.nombreJours || 0;
          // refresh solde in any case; backend should update solde/absences
          this.soldeService.getMyTotal().subscribe({
            next: () => {
              // trigger dashboard to refresh via notifications as well
              this.notificationService.notifyRefresh();
            },
            error: () => {
              // ignore solde errors here
            }
          });
        }
        // Close dialog returning the updated request so the table can update immediately
        this.dialogRef.close(updated);
      },
      error => {
        this.isLoading = false;
        const errorMsg = error.error?.message || error.message || 'Une erreur est survenue';
        this.snackBar.open('Erreur: ' + errorMsg, 'Fermer', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    );
  }

  cancel() {
    this.dialogRef.close(false);
  }

  getActionLabel(): string {
    switch (this.selectedAction) {
      case 'validate':
        return 'Valider';
      case 'info':
        return 'Demander plus d\'informations';
      case 'reject':
        return 'Refuser';
      default:
        return '';
    }
  }
}
