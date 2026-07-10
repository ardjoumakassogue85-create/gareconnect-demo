import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import jsQR from 'jsqr';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { BilletService } from '../../core/services/billet.service';
import { ValidationBillet, VerificationHorsLigne } from '../../core/models/billet.model';

@Component({
  selector: 'app-controle',
  standalone: true,
  imports: [CommonModule, FormsModule, LayoutComponent],
  templateUrl: './controle.component.html',
  styleUrl: './controle.component.scss',
})
export class ControleComponent implements OnInit, OnDestroy {
  @ViewChild('camera') cameraRef?: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas') canvasRef?: ElementRef<HTMLCanvasElement>;

  token = '';
  resultatEnLigne = signal<ValidationBillet | null>(null);
  resultatHorsLigne = signal<VerificationHorsLigne | null>(null);
  chargement = signal(false);
  erreur = signal<string | null>(null);
  scanEnCours = signal(false);
  horsLignePret = signal(false);

  private flux?: MediaStream;
  private animation = 0;

  constructor(private readonly billetService: BilletService) {}

  ngOnInit(): void {
    // Met la cle publique en cache pour permettre la verification hors-ligne.
    this.billetService.clePublique().subscribe({
      next: (cle) => {
        this.billetService.mettreEnCacheClePublique(cle.cle);
        this.horsLignePret.set(true);
      },
      error: () => this.horsLignePret.set(!!this.billetService.clePubliqueEnCache()),
    });
  }

  ngOnDestroy(): void {
    this.arreterScan();
  }

  scanSupporte(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia;
  }

  reinitialiser(): void {
    this.resultatEnLigne.set(null);
    this.resultatHorsLigne.set(null);
    this.erreur.set(null);
  }

  validerEnLigne(): void {
    const token = this.token.trim();
    if (!token) {
      this.erreur.set('Aucun billet à valider.');
      return;
    }
    this.reinitialiser();
    this.chargement.set(true);
    this.billetService.valider(token).subscribe({
      next: (resultat) => {
        this.resultatEnLigne.set(resultat);
        this.chargement.set(false);
      },
      error: () => {
        this.chargement.set(false);
        this.erreur.set('Validation en ligne impossible. Essaie la vérification hors-ligne.');
      },
    });
  }

  async verifierHorsLigne(): Promise<void> {
    const token = this.token.trim();
    if (!token) {
      this.erreur.set('Aucun billet à vérifier.');
      return;
    }
    this.reinitialiser();
    const resultat = await this.billetService.verifierHorsLigne(token);
    this.resultatHorsLigne.set(resultat);
  }

  async demarrerScan(): Promise<void> {
    if (!this.scanSupporte()) {
      this.erreur.set('Caméra non disponible sur cet appareil — colle le contenu du QR.');
      return;
    }
    this.reinitialiser();
    try {
      this.flux = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      const video = this.cameraRef?.nativeElement;
      if (!video) {
        return;
      }
      video.srcObject = this.flux;
      video.setAttribute('playsinline', 'true');
      await video.play();
      this.scanEnCours.set(true);
      this.animation = requestAnimationFrame(() => this.boucleScan());
    } catch {
      this.scanEnCours.set(false);
      this.erreur.set(
        'Caméra indisponible ou permission refusée. (La caméra exige HTTPS ou localhost.)',
      );
    }
  }

  arreterScan(): void {
    this.scanEnCours.set(false);
    if (this.animation) {
      cancelAnimationFrame(this.animation);
      this.animation = 0;
    }
    this.flux?.getTracks().forEach((piste) => piste.stop());
    this.flux = undefined;
  }

  /** Décode chaque image de la caméra avec jsQR (fonctionne sur tous les navigateurs). */
  private boucleScan(): void {
    const video = this.cameraRef?.nativeElement;
    const canvas = this.canvasRef?.nativeElement;
    if (!this.scanEnCours() || !video || !canvas) {
      return;
    }

    if (video.readyState === video.HAVE_ENOUGH_DATA && video.videoWidth && video.videoHeight) {
      const largeur = video.videoWidth;
      const hauteur = video.videoHeight;
      canvas.width = largeur;
      canvas.height = hauteur;
      const contexte = canvas.getContext('2d', { willReadFrequently: true });
      if (contexte) {
        contexte.drawImage(video, 0, 0, largeur, hauteur);
        const image = contexte.getImageData(0, 0, largeur, hauteur);
        const code = jsQR(image.data, largeur, hauteur, { inversionAttempts: 'dontInvert' });
        if (code && code.data) {
          this.token = code.data;
          this.arreterScan();
          this.validerEnLigne();
          return;
        }
      }
    }

    this.animation = requestAnimationFrame(() => this.boucleScan());
  }
}
