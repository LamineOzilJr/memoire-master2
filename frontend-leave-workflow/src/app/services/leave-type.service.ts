import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LeaveType } from '../models/leave-type';

@Injectable({
  providedIn: 'root'
})
export class LeaveTypeService {
  private apiUrl = 'http://localhost:8080/api/type-conges';

  constructor(private http: HttpClient) {}

  getAllLeaveTypes(): Observable<LeaveType[]> {
    return this.http.get<LeaveType[]>(this.apiUrl);
  }

  createLeaveType(type: LeaveType): Observable<LeaveType> {
    return this.http.post<LeaveType>(this.apiUrl, type);
  }
}
