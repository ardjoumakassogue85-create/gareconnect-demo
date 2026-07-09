import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notification } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private readonly http: HttpClient) {}

  lister(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${environment.apiUrl}/notifications`).pipe(timeout(8000));
  }

  compteur(): Observable<{ nonLues: number }> {
    return this.http
      .get<{ nonLues: number }>(`${environment.apiUrl}/notifications/compteur`)
      .pipe(timeout(8000));
  }

  marquerLu(id: string): Observable<void> {
    return this.http
      .patch<void>(`${environment.apiUrl}/notifications/${id}/lu`, {})
      .pipe(timeout(8000));
  }
}
