import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Notification } from '../../../core/models/notification.model';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent implements OnInit {
  notifications = signal<Notification[]>([]);
  nonLues = signal(0);
  panneauOuvert = signal(false);

  constructor(
    public readonly authService: AuthService,
    private readonly notificationService: NotificationService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.rafraichirCompteur();
  }

  estClient(): boolean {
    return this.authService.isAuthenticated() && this.authService.role() === 'CLIENT';
  }

  basculerPanneau(): void {
    const ouvert = !this.panneauOuvert();
    this.panneauOuvert.set(ouvert);
    if (ouvert) {
      this.notificationService.lister().subscribe({
        next: (liste) => this.notifications.set(liste),
        error: () => this.notifications.set([]),
      });
    }
  }

  ouvrirNotification(notification: Notification): void {
    if (!notification.lu) {
      this.notificationService.marquerLu(notification.id).subscribe({ next: () => {}, error: () => {} });
      this.notifications.update((liste) =>
        liste.map((n) => (n.id === notification.id ? { ...n, lu: true } : n)),
      );
      this.nonLues.update((n) => Math.max(0, n - 1));
    }
    this.panneauOuvert.set(false);
    if (notification.type === 'NOTER_VOYAGE' && notification.reservationId) {
      this.router.navigate(['/espace-client'], { queryParams: { noter: notification.reservationId } });
    }
  }

  private rafraichirCompteur(): void {
    if (!this.estClient()) {
      return;
    }
    this.notificationService.compteur().subscribe({
      next: (reponse) => this.nonLues.set(reponse.nonLues),
      error: () => {},
    });
  }
}
