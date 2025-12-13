import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { DgActionDialogComponent } from './dg-action-dialog.component';
import { interval, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-dg-requests',
  templateUrl: './dg-requests.component.html',
  styleUrls: ['./dg-requests.component.scss']
})
export class DgRequestsComponent implements OnInit, OnDestroy {
  dataSource = new MatTableDataSource<LeaveRequestResponse>();
  displayedColumns = ['employee', 'manager', 'type', 'du', 'au', 'jours', 'motif', 'justificatif', 'commentaireChefService', 'actions'];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private refreshInterval = 30000; // 30 seconds

  // KPI stats
  totalPendingDg = 0;
  totalWithJustif = 0;
  totalRejectedDg = 0;
  totalApprovedDg = 0;

  constructor(
    private leaveRequestService: LeaveRequestService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadPendingRequests();
    interval(30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadPendingRequests());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadPendingRequests() {
    this.leaveRequestService.getDgRequests().subscribe(reqs => {
      this.dataSource.data = reqs;

      // Compute KPI stats
      this.totalPendingDg = reqs.filter(r => !r.statutDg || r.statutDg === 'EN_ATTENTE').length;
      this.totalWithJustif = reqs.filter(r => !!r.justificatif).length;
      this.totalRejectedDg = reqs.filter(r => r.statutDg === 'REJETE').length;
      this.totalApprovedDg = reqs.filter(r => r.statutDg === 'VALIDER').length;

      if (!this.dataSource.paginator) {
        this.dataSource.paginator = this.paginator;
      }
      if (!this.dataSource.sort) {
        this.dataSource.sort = this.sort;
      }
    });
  }

  applyFilter(event: Event) {
    const target = event.target as HTMLInputElement | null;
    const value = target ? target.value : '';
    this.dataSource.filter = value.trim().toLowerCase();
  }

  calculateJours(row: LeaveRequestResponse): number {
    return row.nombreJours || 0;
  }

  isHandledByDg(row: LeaveRequestResponse): boolean {
    const s = (row.statutDg || '').toUpperCase();
    return s === 'VALIDER' || s === 'APPROUVE' || s === 'REJETE';
  }

  openActionDialog(request: LeaveRequestResponse) {
    const dialogRef = this.dialog.open(DgActionDialogComponent, {
      width: '90%',
      maxWidth: '600px',
      maxHeight: '90vh',
      data: { request }
    });

    dialogRef.afterClosed().subscribe((result: LeaveRequestResponse | boolean) => {
      if (result) {
        if (typeof result !== 'boolean') {
          const updated = result as LeaveRequestResponse;
          const idx = this.dataSource.data.findIndex(d => d.id === updated.id);
          if (idx !== -1) {
            this.dataSource.data[idx] = updated;
            this.dataSource.data = [...this.dataSource.data];
          } else {
            this.loadPendingRequests();
          }
        } else {
          this.loadPendingRequests();
        }

        this.snackBar.open('Demande traitée avec succès', 'Fermer', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
      }
    });
  }

  openJustificatif(id: number) {
    this.leaveRequestService.downloadJustificatif(id).subscribe(
      blob => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => window.URL.revokeObjectURL(url), 10000);
      },
      error => this.snackBar.open('Erreur: ' + (error.error?.message || error.message), 'Fermer', {
        duration: 5000,
        panelClass: ['error-snackbar']
      })
    );
  }
}

