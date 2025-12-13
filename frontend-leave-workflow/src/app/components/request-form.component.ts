import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { LeaveTypeService } from '../services/leave-type.service';
import { LeaveRequestService, LeaveRequestResponse } from '../services/leave-request.service';
import { UserService } from '../services/user.service';
import { LeaveType } from '../models/leave-type';
import { User } from '../models/user';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-request-form',
  templateUrl: './request-form.component.html',
  styleUrls: ['./request-form.component.scss']
})
export class RequestFormComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  leaveTypes: LeaveType[] = [];
  managers: User[] = [];
  file: File | null = null;
  selectedFileName: string = '';
  numberOfDays: number = 0;
  selectedLeaveTypeId: number | null = null;

  // Edit Mode
  isEditMode = false;
  currentRequestId: number | null = null;
  currentRequest: LeaveRequestResponse | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private leaveTypeService: LeaveTypeService,
    private userService: UserService,
    private leaveRequestService: LeaveRequestService,
    public router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      typeCongeId: ['', Validators.required],
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required],
      motif: [''],
      managerId: ['', Validators.required]
    });

    // Load leave types
    this.leaveTypeService.getAllLeaveTypes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (types) => {
          console.log('Loaded leave types:', types);
          this.leaveTypes = types;
        },
        error: (error) => {
          console.error('Error loading leave types:', error);
        }
      });

    // Load managers
    this.userService.getAllUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (users) => {
          console.log('Loaded users:', users);
          this.managers = users.filter(u => u.role === 'MANAGER' || u.role === 'ADMIN');
        },
        error: (error) => {
          console.error('Error loading users:', error);
        }
      });

    // Check if we're in edit mode
    this.route.queryParams
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        if (params['id']) {
          this.loadRequestForEdit(Number(params['id']));
        }
      });
  }

  loadRequestForEdit(requestId: number) {
    this.leaveRequestService.getRequestById(requestId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (request) => {
          this.isEditMode = true;
          this.currentRequestId = requestId;
          this.currentRequest = request;

          // Check if request can be edited
          if (request.statutManager !== 'EN_ATTENTE') {
            alert('Seules les demandes en attente peuvent être modifiées');
            this.router.navigate(['/my-requests']);
            return;
          }

          // Populate form
          this.form.patchValue({
            typeCongeId: request.typeCongeId,
            dateDebut: new Date(request.dateDebut).toISOString().split('T')[0],
            dateFin: new Date(request.dateFin).toISOString().split('T')[0],
            motif: request.motif
          });

          this.selectedLeaveTypeId = request.typeCongeId;
          this.calculateDays();
        },
        error: (error) => {
          console.error('Error loading request:', error);
          alert('Erreur lors du chargement de la demande');
          this.router.navigate(['/my-requests']);
        }
      });
  }

  onFileChange(event: any) {
    this.file = event.target.files[0];
    this.selectedFileName = this.file ? this.file.name : '';
  }

  onLeaveTypeChange(typeId: number, event: any) {
    if (event.checked) {
      this.selectedLeaveTypeId = typeId;
    } else {
      this.selectedLeaveTypeId = null;
    }
    this.form.patchValue({ typeCongeId: this.selectedLeaveTypeId });
  }

  calculateDays() {
    const startDate = this.form.get('dateDebut')?.value;
    const endDate = this.form.get('dateFin')?.value;

    if (startDate && endDate) {
      const start = new Date(startDate);
      const end = new Date(endDate);
      const timeDiff = end.getTime() - start.getTime();
      this.numberOfDays = Math.ceil(timeDiff / (1000 * 3600 * 24)) + 1;
    } else {
      this.numberOfDays = 0;
    }
  }

  submit() {
    if (this.form.invalid) {
      alert('Veuillez remplir tous les champs obligatoires');
      return;
    }

    const { typeCongeId, dateDebut, dateFin, motif } = this.form.value;
    if (!typeCongeId) {
      alert('Veuillez sélectionner un type de congé');
      return;
    }
    const payload = {
      typeCongeId: Number(typeCongeId),
      dateDebut: (new Date(dateDebut)).toISOString().split('T')[0],
      dateFin: (new Date(dateFin)).toISOString().split('T')[0],
      motif
    };

    if (this.isEditMode && this.currentRequestId) {
      // Update existing request
      this.leaveRequestService.updateRequest(this.currentRequestId, payload, this.file || undefined)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            alert('Demande mise à jour avec succès');
            this.router.navigate(['/my-requests']);
          },
          error: (error) => {
            console.error('Error updating request:', error);
            const msg = error?.error?.message || error?.message || 'Erreur lors de la mise à jour de la demande';
            alert(msg);
          }
        });
    } else {
      // Create new request
      this.leaveRequestService.submitRequest(payload, this.file || undefined)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (resp) => {
            alert('Demande soumise avec succès');
            if (resp?.emailStatus) {
              if (resp.emailStatus.includes('✅')) {
                alert('Notification envoyée au manager par e-mail.');
              } else {
                const err = resp.emailError || 'Erreur inconnue lors de l\'envoi de l\'email.';
                alert('Notification créée mais e-mail non envoyé: ' + err);
              }
            }
            this.router.navigate(['/my-requests']);
          },
          error: (error) => {
            console.error('Error submitting request:', error);
            const msg = error?.error?.message || error?.message || 'Erreur lors de la soumission de la demande';
            alert(msg);
          }
        });
    }
  }

  cancel() {
    this.router.navigate(['/my-requests']);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
