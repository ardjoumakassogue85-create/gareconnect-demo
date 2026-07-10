import { Injectable, signal } from '@angular/core';

interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

@Injectable({ providedIn: 'root' })
export class PwaInstallService {
  peutInstaller = signal(false);
  private evenementInstallation: BeforeInstallPromptEvent | null = null;

  constructor() {
    if (typeof window === 'undefined') return;

    window.addEventListener('beforeinstallprompt', (event) => {
      event.preventDefault();
      this.evenementInstallation = event as BeforeInstallPromptEvent;
      this.peutInstaller.set(true);
    });

    window.addEventListener('appinstalled', () => {
      this.evenementInstallation = null;
      this.peutInstaller.set(false);
    });
  }

  async installer(): Promise<void> {
    if (!this.evenementInstallation) return;

    const evenement = this.evenementInstallation;
    this.evenementInstallation = null;
    this.peutInstaller.set(false);

    await evenement.prompt();
    await evenement.userChoice;
  }
}
