import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<any> {
    const payload = { email, password };
    console.log('Sending login request to:', `${this.apiUrl}/login`);
    console.log('Payload:', { email, password: '***' });

    return this.http.post<any>(`${this.apiUrl}/login`, payload).pipe(
      tap(res => {
        console.log('Login response received:', res);
        localStorage.setItem('token', res.token);
        localStorage.setItem('userRole', res.role);
        localStorage.setItem('userPrenom', res.prenom);
        localStorage.setItem('userId', res.id);
        localStorage.setItem('userEmail', res.email);
      })
    );
  }

  register(registerData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, registerData).pipe(
      tap(res => localStorage.setItem('token', res.token))
    );
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userRole');
    localStorage.removeItem('userPrenom');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUserRole(): string | null {
    return localStorage.getItem('userRole');
  }

  getUserPrenom(): string | null {
    return localStorage.getItem('userPrenom');
  }

  getUserId(): string | null {
    return localStorage.getItem('userId');
  }

  getUserEmail(): string | null {
    return localStorage.getItem('userEmail');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    const userRole = this.getUserRole();
    return userRole === role;
  }
}
