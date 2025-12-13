import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { NotificationService } from '../services/notification.service';

@Component({
  selector: 'app-manager-action-dialog',
  templateUrl: './manager-action-dialog.component.html',
  styleUrls: ['./manager-action-dialog.component.scss']
})
export class ManagerActionDialogComponent {
  request: LeaveRequestResponse;
  selectedAction: 'approve' | 'info' | 'reject' | null = null;
  comment = '';
  isLoading = false;

  constructor(
    public dialogRef: MatDialogRef<ManagerActionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { request: LeaveRequestResponse },
    private leaveRequestService: LeaveRequestService,
    private snackBar: MatSnackBar,
    private notificationService: NotificationService
  ) {
    this.request = data.request;
  }

  selectAction(action: 'approve' | 'info' | 'reject') {
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
      case 'approve':
        request$ = this.leaveRequestService.approveByManager(this.request.id, this.comment);
        break;
      case 'info':
        if (!this.comment || this.comment.trim() === '') {
          this.snackBar.open('Veuillez entrer les informations requises', 'Fermer', {
            duration: 4000,
            panelClass: ['error-snackbar']
          });
          return;
        }
        request$ = this.leaveRequestService.requestMoreInfoByManager(this.request.id, this.comment);
        break;
      case 'reject':
        if (!this.comment || this.comment.trim() === '') {
          this.snackBar.open('Veuillez entrer un motif de refus', 'Fermer', {
            duration: 4000,
            panelClass: ['error-snackbar']
          });
          return;
        }
        request$ = this.leaveRequestService.rejectByManager(this.request.id, this.comment);
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
        // notify notifications to refresh (manager action creates notifications for RH)
        this.notificationService.notifyRefresh();
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
      case 'approve':
        return 'Approuver';
      case 'info':
        return 'Demander plus d\'informations';
      case 'reject':
        return 'Refuser';
      default:
        return '';
    }
  }
}
