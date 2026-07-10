export type TypeNotification = 'NOTER_VOYAGE';

export interface Notification {
  id: string;
  type: TypeNotification;
  titre: string;
  message: string;
  reservationId: string | null;
  compagnie: string | null;
  lu: boolean;
  creeLe: string | null;
}
