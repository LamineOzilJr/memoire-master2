import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PublicHoliday {
  date: string;        // ISO date string
  localName: string;   // Localized name
  name: string;        // English name
  countryCode: string;
}

@Injectable({ providedIn: 'root' })
export class HolidaysService {
  constructor(private http: HttpClient) {}

  getSenegalHolidays(year: number): Observable<PublicHoliday[]> {
    // Nager.Date public holidays API
    return this.http.get<PublicHoliday[]>(`https://date.nager.at/api/v3/PublicHolidays/${year}/SN`);
  }
}
