export interface LeaveRequest {
  id: number;
  dateSoumission: string;
  dateDebut: string;
  dateFin: string;
  nombreJours?: number;
  motif: string;
  justificatif: string;
  statutManager: 'EN_ATTENTE' | 'APPROUVE' | 'REJETE' | 'PLUS_D_INFOS';
  statutRh: 'EN_ATTENTE' | 'VALIDER' | 'APPROUVE' | 'REJETE' | 'PLUS_D_INFOS';
  userId: number;
  userName?: string;
  typeCongeId: number;
  typeCongeLibelle?: string;
  commentaireManager?: string;
  commentaireRh?: string;
  dateCreation?: string;
  dateTraitementManager?: string;
  dateTraitementRh?: string;
}
