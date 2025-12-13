import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ErrorService } from '../services/error.service';

interface TypeConge {
  id?: number;
  libelle: string;
  description?: string;
  maxJours?: number;
  requiresJustificatif: boolean;
  active: boolean;
}

@Component({
  selector: 'app-type-conge-management',
  templateUrl: './type-conge-management.component.html',
  styleUrls: ['./type-conge-management.component.scss']
})
export class TypeCongeManagementComponent implements OnInit {
  typeConges: TypeConge[] = [];
  filteredTypeConges: TypeConge[] = [];
  showForm = false;
  isEditing = false;
  searchTerm = '';
  loading = false;
  typeForm: FormGroup;
  userPrenom: string | null = null;

  private apiUrl = 'http://localhost:8080/api/type-conges';

  constructor(
    private http: HttpClient,
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private errorService: ErrorService
  ) {
    this.typeForm = this.createForm();
  }

  ngOnInit() {
    this.userPrenom = this.authService.getUserPrenom();
    this.loadTypeConges();
  }

  createForm(): FormGroup {
    return this.fb.group({
      id: [null],
      libelle: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      maxJours: [null, [Validators.min(1), Validators.max(365)]],
      requiresJustificatif: [false],
      active: [true]
    });
  }

  loadTypeConges() {
    this.loading = true;
    this.http.get<TypeConge[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.typeConges = data;
        this.filteredTypeConges = data;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
        this.loading = false;
      }
    });
  }

  searchTypeConges() {
    if (!this.searchTerm) {
      this.filteredTypeConges = this.typeConges;
      return;
    }
    const term = this.searchTerm.toLowerCase();
    this.filteredTypeConges = this.typeConges.filter(type =>
      type.libelle.toLowerCase().includes(term) ||
      (type.description && type.description.toLowerCase().includes(term))
    );
  }

  openForm() {
    this.showForm = true;
    this.isEditing = false;
    this.typeForm.reset({ id: null, requiresJustificatif: false, active: true });
  }

  editTypeConge(type: TypeConge) {
    this.showForm = true;
    this.isEditing = true;
    this.typeForm.patchValue(type);
  }

  saveTypeConge() {
    if (this.typeForm.invalid) {
      this.errorService.showWarning('Veuillez remplir tous les champs obligatoires');
      return;
    }

    const typeData = this.typeForm.value;
    if (this.isEditing) {
      const typeId = this.typeForm.value.id;
      this.http.put(`${this.apiUrl}/${typeId}`, typeData).subscribe({
        next: () => {
          this.errorService.showSuccess('Type de congé modifié avec succès');
          this.loadTypeConges();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    } else {
      this.http.post(this.apiUrl, typeData).subscribe({
        next: () => {
          this.errorService.showSuccess('Type de congé créé avec succès');
          this.loadTypeConges();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    }
  }

  deleteTypeConge(typeId: number) {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce type de congé ?')) {
      return;
    }
    this.http.delete(`${this.apiUrl}/${typeId}`).subscribe({
      next: () => {
        this.errorService.showSuccess('Type de congé supprimé avec succès');
        this.loadTypeConges();
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
      }
    });
  }

  closeForm() {
    this.showForm = false;
    this.typeForm.reset({ id: null, requiresJustificatif: false, active: true });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
