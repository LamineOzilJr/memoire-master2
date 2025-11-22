import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AbsenceResponse } from '../models/absence-response';

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private apiUrl = 'http://localhost:8080/api/absences';
  private fallbackUrl = 'http://localhost:8080/absences';
  constructor(private http: HttpClient) {}

  getMyAbsences(): Observable<AbsenceResponse[]> {
    return this.http.get<AbsenceResponse[]>(`${this.apiUrl}/my`).pipe(
      catchError(err => {
        // If server returns 404 for the /api prefixed path, retry without the /api prefix.
        if (err && err.status === 404) {
          return this.http.get<AbsenceResponse[]>(`${this.fallbackUrl}/my`);
        }
        return throwError(() => err);
      })
    );
  }
}
