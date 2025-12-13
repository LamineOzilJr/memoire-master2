import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';

@Component({
  selector: 'app-solde-modal',
  templateUrl: './solde-modal.component.html',
  styleUrls: ['./solde-modal.component.scss']
})
export class SoldeModalComponent {
  constructor(
    public dialogRef: MatDialogRef<SoldeModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private router: Router
  ) {}

  close() {
    this.dialogRef.close();
  }

  get total(): number { return this.data?.total ?? 0; }
  get canAdd(): boolean { return this.total > 0; }

  goToAdd() {
    if (!this.canAdd) return;
    this.dialogRef.close();
    this.router.navigate(['/request-form']);
  }
}

