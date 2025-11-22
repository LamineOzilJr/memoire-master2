import { Component, OnInit } from '@angular/core';
import { AbsenceService } from '../services/absence.service';
import { AbsenceResponse } from '../models/absence-response';

@Component({
  selector: 'app-absences',
  templateUrl: './absences.component.html',
  styleUrls: ['./absences.component.scss']
})
export class AbsencesComponent implements OnInit {
  absences: AbsenceResponse[] = [];
  isLoading = true;

  constructor(private absenceService: AbsenceService) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.isLoading = true;
    this.absenceService.getMyAbsences().subscribe({
      next: list => { this.absences = list || []; this.isLoading = false; },
      error: () => { this.isLoading = false; }
    });
  }
}

