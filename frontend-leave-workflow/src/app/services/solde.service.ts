import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SoldeResponse } from '../models/solde-response';

@Injectable({
  providedIn: 'root'
})
export class SoldeService {
  private apiUrl = 'http://localhost:8080/api/soldes';

  constructor(private http: HttpClient) {}

  getSoldes(): Observable<SoldeResponse[]> {
    return this.http.get<SoldeResponse[]>(`${this.apiUrl}/my`);
  }

  getMyTotal(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/my/total`);
  }
}
