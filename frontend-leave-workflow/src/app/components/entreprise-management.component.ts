import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ErrorService } from '../services/error.service';
import { EntrepriseService, Entreprise } from '../services/entreprise.service';

@Component({
  selector: 'app-entreprise-management',
  templateUrl: './entreprise-management.component.html',
  styleUrls: ['./entreprise-management.component.scss']
})
export class EntrepriseManagementComponent implements OnInit {
  entreprises: Entreprise[] = [];
  filteredEntreprises: Entreprise[] = [];
  showForm = false;
  isEditing = false;
  searchTerm = '';
  loading = false;
  entForm: FormGroup;
  userPrenom: string | null = null;

  constructor(
    private entrepriseService: EntrepriseService,
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private errorService: ErrorService
  ) {
    this.entForm = this.createForm();
  }

  ngOnInit() {
    this.userPrenom = this.authService.getUserPrenom();
    this.loadEntreprises();
  }

  createForm(): FormGroup {
    return this.fb.group({
      id: [null],
      libelle: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      active: [true]
    });
  }

  loadEntreprises() {
    this.loading = true;
    this.entrepriseService.getAllEntreprises().subscribe({
      next: (data) => {
        this.entreprises = data;
        this.filteredEntreprises = data;
        this.loading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
        this.loading = false;
      }
    });
  }

  searchEntreprises() {
    if (!this.searchTerm) {
      this.filteredEntreprises = this.entreprises;
      return;
    }
    const term = this.searchTerm.toLowerCase();
    this.filteredEntreprises = this.entreprises.filter(ent =>
      ent.libelle.toLowerCase().includes(term) ||
      (ent.description && ent.description.toLowerCase().includes(term))
    );
  }

  openForm() {
    this.showForm = true;
    this.isEditing = false;
    this.entForm.reset({ id: null, active: true });
  }

  editEntreprise(ent: Entreprise) {
    this.showForm = true;
    this.isEditing = true;
    this.entForm.patchValue(ent);
  }

  saveEntreprise() {
    if (this.entForm.invalid) {
      this.errorService.showWarning('Veuillez remplir tous les champs obligatoires');
      return;
    }

    const entData = this.entForm.value;
    if (this.isEditing) {
      const entId = this.entForm.value.id;
      this.entrepriseService.updateEntreprise(entId, entData).subscribe({
        next: () => {
          this.errorService.showSuccess('Entreprise modifiée avec succès');
          this.loadEntreprises();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    } else {
      this.entrepriseService.createEntreprise(entData).subscribe({
        next: () => {
          this.errorService.showSuccess('Entreprise créée avec succès');
          this.loadEntreprises();
          this.closeForm();
        },
        error: (error: HttpErrorResponse) => {
          this.errorService.handleError(error);
        }
      });
    }
  }

  deleteEntreprise(entId: number) {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette entreprise ?')) {
      return;
    }
    this.entrepriseService.deleteEntreprise(entId).subscribe({
      next: () => {
        this.errorService.showSuccess('Entreprise supprimée avec succès');
        this.loadEntreprises();
      },
      error: (error: HttpErrorResponse) => {
        this.errorService.handleError(error);
      }
    });
  }

  closeForm() {
    this.showForm = false;
    this.entForm.reset({ id: null, active: true });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
