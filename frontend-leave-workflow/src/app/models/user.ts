export interface User {
  id: number;
  prenom: string;
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  role: 'SALARIE' | 'MANAGER' | 'SERVICE_RH' | 'ADMIN';
  poste: string;
  matricule: string;
  departementId: number;
  departementLibelle?: string;
  managerId: number;
  managerFullName?: string;
  active?: boolean;
  createdAt?: string;
}
