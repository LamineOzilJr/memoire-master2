import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LeaveRequest {
  id: number;
  dateSoumission: string;
  dateDebut: string;
  dateFin: string;
  motif: string;
  justificatif: string;
  statutManager: 'EN_ATTENTE' | 'APPROUVE' | 'REJETE' | 'PLUS_D_INFOS';
  statutRh: 'EN_ATTENTE' | 'VALIDER' | 'APPROUVE' | 'REJETE' | 'PLUS_D_INFOS';
  userId: number;
  typeCongeId: number;
}

export interface LeaveRequestResponse extends LeaveRequest {
  typeCongeLibelle: string;
  userName: string;
  nombreJours: number;
  dateCreation: string;
  dateTraitementManager?: string;
  commentaireManager?: string;
  dateTraitementRh?: string;
  commentaireRh?: string;
  dateTraitementChefService?: string;
  commentaireChefService?: string;
  dateTraitementDg?: string;
  commentaireDg?: string;
  statutChefService?: 'EN_ATTENTE' | 'VALIDER' | 'REJETE' | 'PLUS_D_INFOS';
  statutDg?: 'EN_ATTENTE' | 'VALIDER' | 'REJETE' | 'PLUS_D_INFOS';
  emailStatus?: string;
  emailError?: string;
  hasOverlap?: boolean;  // New field for overlap detection
}

@Injectable({
  providedIn: 'root'
})
export class LeaveRequestService {
  private apiUrl = 'http://localhost:8080/api/demandes';

  constructor(private http: HttpClient) {}

  submitRequest(request: any, file?: File): Observable<LeaveRequestResponse> {
    if (file) {
      const formData = new FormData();
      // Send request as JSON string so backend can parse reliably
      formData.append('request', JSON.stringify(request));
      formData.append('justificatif', file);
      return this.http.post<LeaveRequestResponse>(this.apiUrl, formData);
    } else {
      // No file -> send JSON
      return this.http.post<LeaveRequestResponse>(this.apiUrl, request);
    }
  }

  getMyRequests(): Observable<LeaveRequestResponse[]> {
    return this.http.get<LeaveRequestResponse[]>(`${this.apiUrl}/my`);
  }

  getManagerTeamRequests(): Observable<LeaveRequestResponse[]> {
    return this.http.get<LeaveRequestResponse[]>(`${this.apiUrl}/manager/team`);
  }

  getRhRequests(): Observable<LeaveRequestResponse[]> {
    return this.http.get<LeaveRequestResponse[]>(`${this.apiUrl}/rh/pending`);
  }

  getChefServiceRequests(): Observable<LeaveRequestResponse[]> {
    return this.http.get<LeaveRequestResponse[]>(`${this.apiUrl}/chef-service/pending`);
  }

  getDgRequests(): Observable<LeaveRequestResponse[]> {
    return this.http.get<LeaveRequestResponse[]>(`${this.apiUrl}/dg/pending`);
  }

  getRequestById(id: number): Observable<LeaveRequestResponse> {
    return this.http.get<LeaveRequestResponse>(`${this.apiUrl}/${id}`);
  }

  updateRequest(id: number, request: any, file?: File): Observable<LeaveRequestResponse> {
    if (file) {
      const formData = new FormData();
      // Send request as JSON string
      formData.append('request', JSON.stringify(request));
      formData.append('justificatif', file);
      // Use POST /{id}/modify to avoid multipart PUT issues on some servers
      return this.http.post<LeaveRequestResponse>(`${this.apiUrl}/${id}/modify`, formData);
    } else {
      // No file -> send plain JSON PUT
      return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}`, request);
    }
  }

  deleteRequest(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  approveByManager(id: number, commentaire?: string): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/manager`, {
      statutManager: 'APPROUVE',
      commentaire: commentaire || ''
    });
  }

  requestMoreInfoByManager(id: number, commentaire: string): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/manager`, {
      statutManager: 'PLUS_D_INFOS',
      commentaire
    });
  }

  rejectByManager(id: number, commentaire: string): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/manager`, {
      statutManager: 'REJETE',
      commentaire
    });
  }

  approveByRh(id: number, commentaire?: string): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/rh`, {
      statutRh: 'VALIDER',
      commentaire: commentaire || ''
    });
  }

  requestMoreInfoByRh(id: number, commentaire: string): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/rh`, {
      statutRh: 'PLUS_D_INFOS',
      commentaire
    });
  }

  rejectByRh(id: number, commentaire: string): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/rh`, {
      statutRh: 'REJETE',
      commentaire
    });
  }

  updateChefServiceDecision(id: number, decision: any): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/chef-service`, decision);
  }

  updateDgDecision(id: number, decision: any): Observable<LeaveRequestResponse> {
    return this.http.put<LeaveRequestResponse>(`${this.apiUrl}/${id}/dg`, decision);
  }

  downloadJustificatif(id: number) {
    // returns an Observable<Blob>
    const url = `${this.apiUrl}/${id}/justificatif`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
