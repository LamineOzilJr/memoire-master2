import { Injectable } from '@angular/core';

export interface SenegalHoliday {
  date: string;
  day: string;
  name: string;
  month: number;
}

@Injectable({
  providedIn: 'root'
})
export class SenegalHolidaysService {

  private holidays: SenegalHoliday[] = [
    { date: '1er janvier', day: 'Mercredi', name: 'Jour de l\'An', month: 1 },
    { date: '4 avril', day: 'Vendredi', name: 'Fête de l\'Indépendance', month: 4 },
    { date: '21 avril', day: 'Lundi', name: 'Lundi de Pâques', month: 4 },
    { date: '1er mai', day: 'Jeudi', name: 'Fête du Travail', month: 5 },
    { date: '29 mai', day: 'Jeudi', name: 'Ascension', month: 5 },
    { date: '9 juin', day: 'Lundi', name: 'Lundi de Pentecôte', month: 6 },
    { date: '7 juillet', day: 'Lundi', name: 'Tamkharit (Ashura)', month: 7 },
    { date: '13 août', day: 'Mercredi', name: 'Magal de Touba', month: 8 },
    { date: '15 août', day: 'Vendredi', name: 'Assomption', month: 8 },
    { date: '5 septembre', day: 'Vendredi', name: 'Maouloud', month: 9 }
  ];

  constructor() { }

  getHolidaysByMonth(month: number): SenegalHoliday[] {
    return this.holidays.filter(h => h.month === month);
  }

  getAllHolidays(): SenegalHoliday[] {
    return this.holidays;
  }

  getHolidaysByYear(year: number): SenegalHoliday[] {
    return this.holidays;
  }
}

