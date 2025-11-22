export interface AbsenceResponse {
  id: number;
  userId: number;
  userName: string;
  dateDebut: string;
  dateFin: string;
  nombreJours: number;
  motif?: string;
  createdAt: string;
}

