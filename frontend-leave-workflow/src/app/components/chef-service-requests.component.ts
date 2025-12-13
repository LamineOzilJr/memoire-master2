import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { ChefServiceActionDialogComponent } from './chef-service-action-dialog.component';
import { interval, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-chef-service-requests',
  templateUrl: './chef-service-requests.component.html',
  styleUrls: ['./chef-service-requests.component.scss']
})
export class ChefServiceRequestsComponent implements OnInit, OnDestroy {
  dataSource = new MatTableDataSource<LeaveRequestResponse>();
  displayedColumns = ['employee', 'manager', 'type', 'du', 'au', 'jours', 'motif', 'justificatif', 'commentaireRh', 'actions'];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private refreshInterval = 30000; // 30 seconds

  // KPI stats
  totalPendingChefService = 0;
  totalWithJustif = 0;
  totalRejectedChefService = 0;
  totalApprovedChefService = 0;

  constructor(
    private leaveRequestService: LeaveRequestService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadChefServiceRequests();
    interval(this.refreshInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadChefServiceRequests());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadChefServiceRequests() {
    this.leaveRequestService.getChefServiceRequests().subscribe(
      (reqs: any) => {
        console.log('Chef Service Requests loaded:', reqs);
        // requests validated by RH and pending chef service
        this.dataSource.data = reqs;

        // Compute KPI stats
        this.totalPendingChefService = reqs.filter((r: any) => !r.statutChefService || r.statutChefService === 'EN_ATTENTE').length;
        this.totalWithJustif = reqs.filter((r: any) => !!r.justificatif).length;
        this.totalRejectedChefService = reqs.filter((r: any) => r.statutChefService === 'REJETE').length;
        this.totalApprovedChefService = reqs.filter((r: any) => r.statutChefService === 'VALIDER').length;

        if (!this.dataSource.paginator) {
          this.dataSource.paginator = this.paginator;
        }
        if (!this.dataSource.sort) {
          this.dataSource.sort = this.sort;
        }
      },
      (error: any) => {
        console.error('Error loading chef service requests:', error);
      }
    );
  }

  calculateJours(row: LeaveRequestResponse): number {
    return row.nombreJours || 0;
  }


  isHandledByChefService(row: LeaveRequestResponse): boolean {
    const s = (row.statutChefService || '').toUpperCase();
    return s === 'VALIDER' || s === 'APPROUVE' || s === 'REJETE';
  }

  applyFilter(event: Event) {
    const target = event.target as HTMLInputElement | null;
    const value = target ? target.value : '';
    this.dataSource.filter = value.trim().toLowerCase();
  }

  openActionDialog(request: LeaveRequestResponse) {
    const dialogRef = this.dialog.open(ChefServiceActionDialogComponent, {
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
            this.loadChefServiceRequests();
          }
        } else {
          this.loadChefServiceRequests();
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
