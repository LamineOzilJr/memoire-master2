import { Component, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { EditRequestDialogComponent } from './edit-request-dialog.component';
import { interval, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-request-list',
  templateUrl: './request-list.component.html',
  styleUrls: ['./request-list.component.scss']
})
export class RequestListComponent implements OnInit, OnDestroy {
  dataSource = new MatTableDataSource<LeaveRequestResponse>();
  displayedColumns = ['type', 'du', 'au', 'jours', 'statutManager', 'statutRh', 'statutChefService', 'statutDg', 'motif', 'justificatif', 'commentaireManager', 'actions'];

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();
  private refreshInterval = 30000; // 30 seconds

  constructor(
    private leaveRequestService: LeaveRequestService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.loadRequests();
    // Auto-refresh every 30 seconds
    interval(this.refreshInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadRequests());
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadRequests() {
    this.leaveRequestService.getMyRequests().subscribe(reqs => {
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

  isEditable(row: LeaveRequestResponse): boolean {
    return row.statutManager === 'EN_ATTENTE' || row.statutManager === 'PLUS_D_INFOS';
  }

  isDeletable(row: LeaveRequestResponse): boolean {
    return row.statutManager === 'EN_ATTENTE' && row.statutRh === 'EN_ATTENTE';
  }

  modify(id: number) {
    // Find the request to edit
    const request = this.dataSource.data.find(r => r.id === id);
    if (!request) {
      alert('Demande non trouvée');
      return;
    }

    // If an element is focused, blur it to avoid aria-hidden accessibility warning
    try {
      const active = document.activeElement as HTMLElement | null;
      if (active && typeof active.blur === 'function') {
        active.blur();
      }
    } catch (e) {
      // ignore
    }

    // Open edit dialog; prevent automatic focus restoration to avoid hiding focus
    const dialogRef = this.dialog.open(EditRequestDialogComponent, {
      width: '90%',
      maxWidth: '600px',
      maxHeight: '90vh',
      data: { request },
      restoreFocus: false,
      autoFocus: false
    });

    // Reload requests when dialog closes with result
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadRequests();
      }

      // restore focus to the table or some safe element
      try {
        const tableEl = document.querySelector('.requests-table') as HTMLElement | null;
        if (tableEl) {
          tableEl.focus();
        }
      } catch (e) { /* ignore */ }
    });
  }

  delete(id: number) {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette demande ? Cette action est irréversible.')) {
      this.leaveRequestService.deleteRequest(id).subscribe(
        () => {
          alert('Demande supprimée avec succès');
          this.loadRequests();
        },
        error => {
          alert('Erreur lors de la suppression: ' + (error.error?.message || error.message));
        }
      );
    }
  }

  openJustificatif(id: number, filename?: string) {
    this.leaveRequestService.downloadJustificatif(id).subscribe(
      blob => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        // revoke after a delay
        setTimeout(() => window.URL.revokeObjectURL(url), 10000);
      },
      error => alert('Erreur lors du téléchargement du justificatif: ' + (error.error?.message || error.message))
    );
  }
}
