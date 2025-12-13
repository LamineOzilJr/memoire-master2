import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Entreprise {
  id?: number;
  libelle: string;
  description?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
  userCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class EntrepriseService {

  // Point frontend requests to backend API server
  private apiUrl = 'http://localhost:8080/api/entreprises';

  constructor(private http: HttpClient) { }

  getAllEntreprises(): Observable<Entreprise[]> {
    return this.http.get<Entreprise[]>(this.apiUrl);
  }

  getEntrepriseById(id: number): Observable<Entreprise> {
    return this.http.get<Entreprise>(`${this.apiUrl}/${id}`);
  }

  createEntreprise(entreprise: Entreprise): Observable<Entreprise> {
    return this.http.post<Entreprise>(this.apiUrl, entreprise);
  }

  updateEntreprise(id: number, entreprise: Entreprise): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}`, entreprise);
  }

  deleteEntreprise(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  toggleActive(id: number): Observable<Entreprise> {
    return this.http.put<Entreprise>(`${this.apiUrl}/${id}/toggle-active`, {});
  }
}
