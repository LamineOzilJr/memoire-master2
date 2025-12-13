import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { RhActionDialogComponent } from './rh-action-dialog.component';
import { interval, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-rh-requests',
  templateUrl: './rh-requests.component.html',
  styleUrls: ['./rh-requests.component.scss']
})
export class RhRequestsComponent implements OnInit, OnDestroy {
  dataSource = new MatTableDataSource<LeaveRequestResponse>();
  displayedColumns = ['employee', 'manager', 'type', 'du', 'au', 'jours', 'motif', 'justificatif', 'commentaireManager', 'actions'];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private refreshInterval = 30000; // 30 seconds

  // KPI stats
  totalApprovedRequests = 0;
  totalPendingRh = 0;
  totalWithJustif = 0;
  totalRejectedRh = 0;

  constructor(
    private leaveRequestService: LeaveRequestService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadRhRequests();
    // Auto-refresh every 30 seconds
    interval(this.refreshInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadRhRequests());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadRhRequests() {
    this.leaveRequestService.getRhRequests().subscribe(reqs => {
      // Only keep requests that were approved by manager
      const approved = reqs.filter(r => r.statutManager === 'APPROUVE');
      this.dataSource.data = approved;

      // compute KPI stats
      this.totalApprovedRequests = approved.length;
      this.totalPendingRh = approved.filter(r => !r.statutRh || r.statutRh === 'EN_ATTENTE').length;
      this.totalWithJustif = approved.filter(r => !!r.justificatif).length;
      // Count requests explicitly rejected by RH
      this.totalRejectedRh = approved.filter(r => r.statutRh === 'REJETE').length;

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

  isApproved(row: LeaveRequestResponse): boolean {
    return row.statutManager === 'APPROUVE';
  }

  // Consider RH-handled statuses: when status is VALIDER, APPROUVE or REJETE, the request is finalized for RH
  isHandledByRh(row: LeaveRequestResponse): boolean {
    const s = (row.statutRh || '').toUpperCase();
    return s === 'VALIDER' || s === 'APPROUVE' || s === 'REJETE';
  }

  openActionDialog(request: LeaveRequestResponse) {
    const dialogRef = this.dialog.open(RhActionDialogComponent, {
      width: '90%',
      maxWidth: '600px',
      maxHeight: '90vh',
      data: { request }
    });

    dialogRef.afterClosed().subscribe((result: LeaveRequestResponse | boolean) => {
      if (result) {
        // If the dialog returned the updated request, update the row immediately
        if (typeof result !== 'boolean') {
          const updated = result as LeaveRequestResponse;
          const idx = this.dataSource.data.findIndex(d => d.id === updated.id);
          if (idx !== -1) {
            // Replace the item in place so table updates
            this.dataSource.data[idx] = updated;
            // Trigger data change detection by creating a new array reference
            this.dataSource.data = [...this.dataSource.data];
          } else {
            // fallback: reload everything
            this.loadRhRequests();
          }
        } else {
          this.loadRhRequests();
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
