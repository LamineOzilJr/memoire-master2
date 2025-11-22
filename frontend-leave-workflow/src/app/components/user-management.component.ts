import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { ErrorService } from '../services/error.service';
import { AuthService } from '../services/auth.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

interface User {
  id?: number;
  prenom: string;
  nom: string;
  email: string;
  password?: string;
  matricule: string;
  role: string;
  poste: string;
  telephone: string;
  adresse: string;
  departementId?: number;
  managerId?: number;
  active: boolean;
}

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  showForm = false;
  isEditing = false;
  searchTerm = '';
  loading = false;
  userForm: FormGroup;
  userPrenom: string | null = null;
  departements: any[] = [];
  managers: User[] = [];

  roles = [
    { value: 'ADMIN', label: 'Administrateur' },
    { value: 'MANAGER', label: 'Manager' },
    { value: 'SERVICE_RH', label: 'Service RH' },
    { value: 'SALARIE', label: 'Salarié' }
  ];

  private apiUrl = 'http://localhost:8080/api/users';
  private deptApiUrl = 'http://localhost:8080/api/departements';

  constructor(
    private http: HttpClient,
    private dialog: MatDialog,
    private errorService: ErrorService,
    private authService: AuthService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.userForm = this.createForm();
  }

  ngOnInit() {
    this.userPrenom = this.authService.getUserPrenom();
    this.loadUsers();
    this.loadDepartements();
  }

  createForm(): FormGroup {
    return this.fb.group({
      id: [null],
      prenom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      nom: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      password: [''],
      matricule: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{3,10}$/)]],
      role: ['SALARIE', Validators.required],
      poste: ['', Validators.required],
      telephone: ['', Validators.pattern(/^[+]?[0-9]{9,15}$/)],
      adresse: [''],
      departementId: [null],
      managerId: [null],
      active: [true]
    });
  }

  loadUsers() {
    this.loading = true;
    console.log('Loading users from:', this.apiUrl);
    console.log('Token in localStorage:', localStorage.getItem('token'));
    this.http.get<User[]>(this.apiUrl).subscribe({
      next: (data) => {
        console.log('Users loaded successfully:', data);
        this.users = data;
        this.filteredUsers = data;
        this.loading = false;
        this.loadManagers();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading users:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);
        this.errorService.handleError(error);
        this.loading = false;
      }
    });
  }

  searchUsers() {
    if (!this.searchTerm) {
      this.filteredUsers = this.users;
      return;
    }
    const term = this.searchTerm.toLowerCase();
    this.filteredUsers = this.users.filter(user =>
      user.prenom.toLowerCase().includes(term) ||
      user.nom.toLowerCase().includes(term) ||
      user.email.toLowerCase().includes(term) ||
      user.matricule.toLowerCase().includes(term)
    );
  }

  openForm() {
    this.showForm = true;
    this.isEditing = false;
    this.userForm.reset({ id: null, role: 'SALARIE', active: true });
    // Make password required for new users
    this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
    this.userForm.get('password')?.updateValueAndValidity();
  }

  editUser(user: User) {
    this.showForm = true;
    this.isEditing = true;
    this.userForm.patchValue(user);
    // Password is optional for updates
    this.userForm.get('password')?.clearValidators();
    this.userForm.get('password')?.updateValueAndValidity();
  }

  deleteUser(userId: number) {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) {
      return;
    }
    this.http.delete(`${this.apiUrl}/${userId}`).subscribe({
      next: () => {
        this.errorService.showSuccess('Utilisateur supprimé avec succès');
        this.loadUsers();
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
      }
    });
  }

  saveUser() {
    if (this.userForm.invalid) {
      this.errorService.showWarning('Veuillez remplir tous les champs obligatoires correctement');
      Object.keys(this.userForm.controls).forEach(key => {
        this.userForm.get(key)?.markAsTouched();
      });
      return;
    }
    const userData = this.userForm.value;
    if (this.isEditing) {
      const userId = this.userForm.value.id;
      this.http.put(`${this.apiUrl}/${userId}`, userData).subscribe({
        next: () => {
          this.errorService.showSuccess('Utilisateur modifié avec succès');
          this.loadUsers();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    } else {
      this.http.post(this.apiUrl, userData).subscribe({
        next: () => {
          this.errorService.showSuccess('Utilisateur créé avec succès');
          this.loadUsers();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    }
  }

  toggleUserStatus(user: User) {
    const endpoint = user.active ? 'deactivate' : 'activate';
    this.http.patch(`${this.apiUrl}/${user.id}/${endpoint}`, {}).subscribe({
      next: () => {
        const status = user.active ? 'désactivé' : 'activé';
        this.errorService.showSuccess(`Utilisateur ${status} avec succès`);
        this.loadUsers();
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
      }
    });
  }

  closeForm() {
    this.showForm = false;
    this.userForm.reset({ id: null, role: 'SALARIE', active: true });
  }

  getRoleLabel(role: string): string {
    const roleObj = this.roles.find(r => r.value === role);
    return roleObj ? roleObj.label : role;
  }

  getFormError(fieldName: string): string {
    const control = this.userForm.get(fieldName);
    if (control?.hasError('required')) {
      return 'Ce champ est requis';
    }
    if (control?.hasError('email')) {
      return 'Email invalide';
    }
    if (control?.hasError('minlength')) {
      const minLength = control.errors?.['minlength'].requiredLength;
      return `Minimum ${minLength} caractères requis`;
    }
    if (control?.hasError('maxlength')) {
      const maxLength = control.errors?.['maxlength'].requiredLength;
      return `Maximum ${maxLength} caractères autorisés`;
    }
    if (control?.hasError('pattern')) {
      if (fieldName === 'matricule') {
        return 'Format: 3-10 caractères alphanumériques majuscules';
      }
      if (fieldName === 'telephone') {
        return 'Numéro de téléphone invalide';
      }
    }
    return '';
  }

  loadDepartements() {
    this.http.get<any[]>(this.deptApiUrl).subscribe({
      next: (data) => {
        this.departements = data.filter(d => d.active);
        this.loadManagers();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading departments:', error);
      }
    });
  }

  loadManagers() {
    // Filter users with MANAGER role for manager selection
    this.managers = this.users.filter(u => u.role === 'MANAGER' || u.role === 'ADMIN');
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
