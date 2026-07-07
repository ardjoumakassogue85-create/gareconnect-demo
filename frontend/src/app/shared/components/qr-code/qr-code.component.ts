import { AfterViewInit, Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import * as QRCode from 'qrcode';

/**
 * Génère un QR code entièrement côté client, sans appel réseau.
 * Remplace la dépendance précédente à api.qrserver.com, qui rendait
 * l'affichage du billet fragile en cas de wifi absent ou instable.
 */
@Component({
  selector: 'app-qr-code',
  standalone: true,
  template: `<canvas #canevas [attr.aria-label]="'QR code ' + valeur"></canvas>`,
  styles: [
    `
      :host {
        display: inline-block;
        line-height: 0;
      }
      canvas {
        border-radius: 4px;
      }
    `,
  ],
})
export class QrCodeComponent implements AfterViewInit, OnChanges {
  @Input({ required: true }) valeur = '';
  @Input() taille = 140;

  @ViewChild('canevas') canevasRef?: ElementRef<HTMLCanvasElement>;

  ngAfterViewInit(): void {
    this.dessiner();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['valeur'] && !changes['taille']) return;
    this.dessiner();
  }

  private dessiner(): void {
    const canevas = this.canevasRef?.nativeElement;
    if (!canevas || !this.valeur) return;

    QRCode.toCanvas(canevas, this.valeur, {
      width: this.taille,
      margin: 1,
      color: { dark: '#0e2238', light: '#ffffff' },
    }).catch(() => {
      // Rendu silencieux : un échec de génération QR ne doit jamais casser
      // l'affichage du reste du billet (référence texte toujours visible).
    });
  }
}
