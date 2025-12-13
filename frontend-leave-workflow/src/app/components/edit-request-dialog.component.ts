import { Component, Inject, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { LeaveTypeService } from '../services/leave-type.service';
import { LeaveType } from '../models/leave-type';

@Component({
  selector: 'app-edit-request-dialog',
  templateUrl: './edit-request-dialog.component.html',
  styleUrls: ['./edit-request-dialog.component.scss']
})
export class EditRequestDialogComponent implements OnInit, AfterViewInit {
  @ViewChild('dateDebutRef') dateDebutRef!: ElementRef<HTMLInputElement>;

  form: FormGroup;
  leaveTypes: LeaveType[] = [];
  isLoading = false;
  selectedFile: File | null = null;

  constructor(
    private fb: FormBuilder,
    private leaveRequestService: LeaveRequestService,
    private leaveTypeService: LeaveTypeService,
    public dialogRef: MatDialogRef<EditRequestDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { request: LeaveRequestResponse }
  ) {
    this.form = this.fb.group({
      typeCongeId: ['', Validators.required],
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required],
      motif: ['']
    });
  }

  ngOnInit() {
    this.loadLeaveTypes();
    this.populateForm();
  }

  ngAfterViewInit() {
    // focus the first input inside the dialog to ensure assistive tech focus is correct
    try {
      setTimeout(() => {
        if (this.dateDebutRef && this.dateDebutRef.nativeElement) {
          this.dateDebutRef.nativeElement.focus();
        }
      }, 50);
    } catch (e) { /* ignore */ }
  }

  loadLeaveTypes() {
    this.leaveTypeService.getAllLeaveTypes().subscribe((types: LeaveType[]) => {
      this.leaveTypes = types;
    });
  }

  populateForm() {
    if (this.data.request) {
      this.form.patchValue({
        typeCongeId: this.data.request.typeCongeId,
        dateDebut: this.data.request.dateDebut,
        dateFin: this.data.request.dateFin,
        motif: this.data.request.motif
      });
    }
  }

  onFileSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    const files = target.files;
    if (files && files.length > 0) {
      this.selectedFile = files[0];
    }
  }

  submit(): void {
    if (this.form.invalid) {
      alert('Veuillez remplir tous les champs obligatoires');
      return;
    }

    this.isLoading = true;
    const raw = this.form.value;
    const formValue = {
      typeCongeId: Number(raw.typeCongeId),
      dateDebut: (new Date(raw.dateDebut)).toISOString().split('T')[0],
      dateFin: (new Date(raw.dateFin)).toISOString().split('T')[0],
      motif: raw.motif
    };

    this.leaveRequestService.updateRequest(
      this.data.request.id,
      formValue,
      this.selectedFile || undefined
    ).subscribe(
      () => {
        this.isLoading = false;
        this.dialogRef.close(true);
      },
      (error: any) => {
        this.isLoading = false;
        alert('Erreur: ' + (error.error?.message || error.message));
      }
    );
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
