import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ErrorService {
  constructor(private snackBar: MatSnackBar) {}

  handleError(error: HttpErrorResponse): void {
    let message = 'Une erreur s\'est produite';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      message = `Erreur: ${error.error.message}`;
    } else {
      // Server-side error
      if (error.error && error.error.message) {
        message = error.error.message;
      } else if (error.error && error.error.errors) {
        // Validation errors
        const validationErrors = Object.values(error.error.errors).join(', ');
        message = `Erreurs de validation: ${validationErrors}`;
      } else {
        message = `Erreur ${error.status}: ${error.message}`;
      }
    }

    this.showError(message);
  }

  showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['error-snackbar']
    });
  }

  showSuccess(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['success-snackbar']
    });
  }

  showWarning(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['warning-snackbar']
    });
  }
}
