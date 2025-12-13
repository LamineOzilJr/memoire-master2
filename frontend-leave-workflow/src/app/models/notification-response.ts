export interface NotificationResponse {
  id: number;
  titre: string;
  message: string;
  lu: boolean;
  createdAt: string;
  targetId?: number;
}
