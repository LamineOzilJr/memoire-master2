import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ErrorService } from '../services/error.service';

interface Departement {
  id?: number;
  libelle: string;
  description?: string;
  active: boolean;
  userCount?: number;
}

@Component({
  selector: 'app-departement-management',
  templateUrl: './departement-management.component.html',
  styleUrls: ['./departement-management.component.scss']
})
export class DepartementManagementComponent implements OnInit {
  departements: Departement[] = [];
  filteredDepartements: Departement[] = [];
  showForm = false;
  isEditing = false;
  searchTerm = '';
  loading = false;
  deptForm: FormGroup;
  userPrenom: string | null = null;

  private apiUrl = 'http://localhost:8080/api/departements';

  constructor(
    private http: HttpClient,
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private errorService: ErrorService
  ) {
    this.deptForm = this.createForm();
  }

  ngOnInit() {
    this.userPrenom = this.authService.getUserPrenom();
    this.loadDepartements();
  }

  createForm(): FormGroup {
    return this.fb.group({
      id: [null],
      libelle: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      active: [true]
    });
  }

  loadDepartements() {
    this.loading = true;
    this.http.get<Departement[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.departements = data;
        this.filteredDepartements = data;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
        this.loading = false;
      }
    });
  }

  searchDepartements() {
    if (!this.searchTerm) {
      this.filteredDepartements = this.departements;
      return;
    }
    const term = this.searchTerm.toLowerCase();
    this.filteredDepartements = this.departements.filter(dept =>
      dept.libelle.toLowerCase().includes(term) ||
      (dept.description && dept.description.toLowerCase().includes(term))
    );
  }

  openForm() {
    this.showForm = true;
    this.isEditing = false;
    this.deptForm.reset({ id: null, active: true });
  }

  editDepartement(dept: Departement) {
    this.showForm = true;
    this.isEditing = true;
    this.deptForm.patchValue(dept);
  }

  saveDepartement() {
    if (this.deptForm.invalid) {
      this.errorService.showWarning('Veuillez remplir tous les champs obligatoires');
      return;
    }

    const deptData = this.deptForm.value;
    if (this.isEditing) {
      const deptId = this.deptForm.value.id;
      this.http.put(`${this.apiUrl}/${deptId}`, deptData).subscribe({
        next: () => {
          this.errorService.showSuccess('Département modifié avec succès');
          this.loadDepartements();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    } else {
      this.http.post(this.apiUrl, deptData).subscribe({
        next: () => {
          this.errorService.showSuccess('Département créé avec succès');
          this.loadDepartements();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    }
  }

  deleteDepartement(deptId: number) {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce département ?')) {
      return;
    }
    this.http.delete(`${this.apiUrl}/${deptId}`).subscribe({
      next: () => {
        this.errorService.showSuccess('Département supprimé avec succès');
        this.loadDepartements();
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
      }
    });
  }

  closeForm() {
    this.showForm = false;
    this.deptForm.reset({ id: null, active: true });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
