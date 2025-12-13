import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationService } from '../services/notification.service';

@Component({
  selector: 'app-dg-action-dialog',
  templateUrl: './dg-action-dialog.component.html',
  styleUrls: ['./dg-action-dialog.component.scss']
})
export class DgActionDialogComponent {
  selectedAction: 'validate' | 'info' | 'reject' | null = null;
  comment = '';
  isLoading = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { request: LeaveRequestResponse },
    private dialogRef: MatDialogRef<DgActionDialogComponent>,
    private leaveRequestService: LeaveRequestService,
    private snackBar: MatSnackBar,
    private notificationService: NotificationService
  ) {}

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
    let statutDg: string;

    switch (this.selectedAction) {
      case 'validate':
        statutDg = 'VALIDER';
        request$ = this.leaveRequestService.updateDgDecision(this.data.request.id!, {
          statutDg,
          commentaire: this.comment || ''
        });
        break;
      case 'info':
        if (!this.comment || this.comment.trim() === '') {
          this.snackBar.open('Veuillez entrer les informations requises', 'Fermer', {
            duration: 4000,
            panelClass: ['error-snackbar']
          });
          return;
        }
        statutDg = 'PLUS_D_INFOS';
        request$ = this.leaveRequestService.updateDgDecision(this.data.request.id!, {
          statutDg,
          commentaire: this.comment
        });
        break;
      case 'reject':
        if (!this.comment || this.comment.trim() === '') {
          this.snackBar.open('Veuillez entrer un motif de refus', 'Fermer', {
            duration: 4000,
            panelClass: ['error-snackbar']
          });
          return;
        }
        statutDg = 'REJETE';
        request$ = this.leaveRequestService.updateDgDecision(this.data.request.id!, {
          statutDg,
          commentaire: this.comment
        });
        break;
      default:
        request$ = null;
    }

    if (!request$) {
      return;
    }

    this.isLoading = true;
    request$.subscribe(
      (updated: LeaveRequestResponse) => {
        this.isLoading = false;
        this.snackBar.open('Action effectuée avec succès', 'Fermer', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.notificationService.notifyRefresh();
        this.dialogRef.close(updated);
      },
      (error: any) => {
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
      case 'validate': return 'Valider';
      case 'info': return 'Demander plus d\'informations';
      case 'reject': return 'Refuser';
      default: return '';
    }
  }
}

