import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatRadioModule } from '@angular/material/radio';
import { MatCardModule } from '@angular/material/card';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AppComponent } from './app.component';
import { LoginComponent } from './components/login.component';
import { DashboardComponent } from './components/dashboard.component';
import { RequestFormComponent } from './components/request-form.component';
import { RequestListComponent } from './components/request-list.component';
import { EditRequestDialogComponent } from './components/edit-request-dialog.component';
import { ManagerRequestsComponent } from './components/manager-requests.component';
import { ManagerActionDialogComponent } from './components/manager-action-dialog.component';
import { RhRequestsComponent } from './components/rh-requests.component';
import { RhActionDialogComponent } from './components/rh-action-dialog.component';
import { ChefServiceRequestsComponent } from './components/chef-service-requests.component';
import { ChefServiceActionDialogComponent } from './components/chef-service-action-dialog.component';
import { DgRequestsComponent } from './components/dg-requests.component';
import { DgActionDialogComponent } from './components/dg-action-dialog.component';
import { EntrepriseManagementComponent } from './components/entreprise-management.component';
import { RulesPageComponent } from './components/rules-page.component';
import { SoldeModalComponent } from './solde-modal/solde-modal.component';
import { UserManagementComponent } from './components/user-management.component';
import { DepartementManagementComponent } from './components/departement-management.component';
import { TypeCongeManagementComponent } from './components/type-conge-management.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { JwtInterceptor } from './interceptors/jwt.interceptor';
import { FilterPipe } from './pipes/filter.pipe';
import { HolidaysService } from './services/holidays.service';
import { AbsencesComponent } from './components/absences.component';
import { AbsenceService } from './services/absence.service';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'rules', component: RulesPageComponent, canActivate: [AuthGuard] },
  { path: 'users', component: UserManagementComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN'] } },
  { path: 'departements', component: DepartementManagementComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'SERVICE_RH'] } },
  { path: 'entreprises', component: EntrepriseManagementComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN'] } },
  { path: 'type-conges', component: TypeCongeManagementComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'SERVICE_RH'] } },
  { path: 'rh-requests', component: RhRequestsComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['SERVICE_RH'] } },
  { path: 'chef-service-requests', component: ChefServiceRequestsComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['CHEF_SERVICE'] } },
  { path: 'dg-requests', component: DgRequestsComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['DG'] } },
  { path: 'request-form', component: RequestFormComponent, canActivate: [AuthGuard, RoleGuard], data: { roles: ['SALARIE'] } },
  { path: 'my-requests', component: RequestListComponent, canActivate: [AuthGuard] },
  { path: 'absences', component: AbsencesComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: '/login', pathMatch: 'full' }
];

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    DashboardComponent,
    RequestFormComponent,
    RequestListComponent,
    EditRequestDialogComponent,
    ManagerRequestsComponent,
    ManagerActionDialogComponent,
    RhRequestsComponent,
    RhActionDialogComponent,
    ChefServiceRequestsComponent,
    ChefServiceActionDialogComponent,
    DgRequestsComponent,
    DgActionDialogComponent,
    RulesPageComponent,
    SoldeModalComponent,
    UserManagementComponent,
    DepartementManagementComponent,
    EntrepriseManagementComponent,
    TypeCongeManagementComponent,
    AbsencesComponent
  ],
  imports: [
    BrowserModule,
    CommonModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forRoot(routes),
    BrowserAnimationsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatRadioModule,
    MatMenuModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatCheckboxModule,
    FilterPipe
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    AbsenceService,
    HolidaysService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
