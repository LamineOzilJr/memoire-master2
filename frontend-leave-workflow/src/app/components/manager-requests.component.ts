import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { ManagerActionDialogComponent } from './manager-action-dialog.component';
import { interval, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-manager-requests',
  templateUrl: './manager-requests.component.html',
  styleUrls: ['./manager-requests.component.scss']
})
export class ManagerRequestsComponent implements OnInit, OnDestroy {
  dataSource = new MatTableDataSource<LeaveRequestResponse>();
  displayedColumns = ['employee', 'type', 'du', 'au', 'jours', 'chevauchement', 'motif', 'justificatif', 'commentaireManager', 'actions'];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private refreshInterval = 30000; // 30 seconds

  constructor(
    private leaveRequestService: LeaveRequestService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadTeamRequests();
    // Auto-refresh every 30 seconds
    interval(this.refreshInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadTeamRequests());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTeamRequests() {
    this.leaveRequestService.getManagerTeamRequests().subscribe(reqs => {
      this.dataSource.data = reqs;
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

  isPending(row: LeaveRequestResponse): boolean {
    return row.statutManager === 'EN_ATTENTE';
  }

  // Consider manager-handled statuses: when status is APPROUVE, VALIDER or REJETE, the request is finalized for manager
  isHandledByManager(row: LeaveRequestResponse): boolean {
    const s = (row.statutManager || '').toUpperCase();
    return s === 'APPROUVE' || s === 'VALIDER' || s === 'REJETE';
  }

  approveRequest(id: number) {
    const request = this.dataSource.data.find(r => r.id === id);
    if (request) {
      this.openActionDialog(request);
    }
  }

  hasOverlap(row: LeaveRequestResponse): boolean {
    return row.hasOverlap || false;
  }

  getRowClass(row: LeaveRequestResponse): string {
    return this.hasOverlap(row) ? 'overlap-row' : '';
  }


  private openActionDialog(request: LeaveRequestResponse) {
    const dialogRef = this.dialog.open(ManagerActionDialogComponent, {
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
            this.loadTeamRequests();
          }
        } else {
          this.loadTeamRequests();
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
      error => alert('Erreur lors du téléchargement du justificatif: ' + (error.error?.message || error.message))
    );
  }
}
