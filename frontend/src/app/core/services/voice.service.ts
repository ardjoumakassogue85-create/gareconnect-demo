import { Injectable, NgZone, signal } from '@angular/core';

/**
 * Reconnaissance vocale (Web Speech API du navigateur).
 *
 * Purement cote client, aucun backend. Convertit la parole en texte, que l'on
 * injecte ensuite dans les assistants IA existants (recherche libre, anti-file).
 * Degradation gracieuse : `estDisponible()` permet de cacher le bouton micro si
 * le navigateur ne supporte pas la reconnaissance vocale.
 */
@Injectable({ providedIn: 'root' })
export class VoiceService {
  /** Vrai pendant l'ecoute (pour l'animation du bouton). */
  readonly ecouteEnCours = signal(false);

  private reconnaissance: any = null;

  constructor(private readonly zone: NgZone) {}

  estDisponible(): boolean {
    return (
      typeof window !== 'undefined' &&
      !!((window as any).SpeechRecognition || (window as any).webkitSpeechRecognition)
    );
  }

  /**
   * Lance une ecoute unique. Rappelle `onTexte` avec la transcription (fr-FR),
   * `onErreur` en cas de souci. Si une ecoute est deja en cours, elle est arretee.
   */
  ecouter(onTexte: (texte: string) => void, onErreur?: (code: string) => void): void {
    if (!this.estDisponible()) {
      onErreur?.('non-supporte');
      return;
    }
    if (this.ecouteEnCours()) {
      this.arreter();
      return;
    }

    const Ctor = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    const reco = new Ctor();
    reco.lang = 'fr-FR';
    reco.interimResults = false;
    reco.maxAlternatives = 1;
    reco.continuous = false;

    reco.onstart = () => this.zone.run(() => this.ecouteEnCours.set(true));
    reco.onend = () => this.zone.run(() => this.ecouteEnCours.set(false));
    reco.onerror = (evenement: any) =>
      this.zone.run(() => {
        this.ecouteEnCours.set(false);
        onErreur?.(evenement?.error ?? 'erreur');
      });
    reco.onresult = (evenement: any) => {
      const texte: string = evenement?.results?.[0]?.[0]?.transcript ?? '';
      this.zone.run(() => {
        this.ecouteEnCours.set(false);
        if (texte && texte.trim()) {
          onTexte(texte.trim());
        }
      });
    };

    this.reconnaissance = reco;
    try {
      reco.start();
    } catch {
      // start() peut lever si une ecoute est deja active : on ignore.
    }
  }

  arreter(): void {
    try {
      this.reconnaissance?.stop();
    } catch {
      // ignore
    }
    this.ecouteEnCours.set(false);
  }
}
