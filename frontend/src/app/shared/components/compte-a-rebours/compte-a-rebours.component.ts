import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output } from '@angular/core';

@Component({
  selector: 'app-compte-a-rebours',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './compte-a-rebours.component.html',
  styleUrl: './compte-a-rebours.component.scss',
})
export class CompteARceboursComponent implements OnChanges, OnDestroy {
  @Input() dateLimite: string | null = null;
  @Output() expire = new EventEmitter<void>();

  libelle = '';
  expiree = false;

  private intervalId: ReturnType<typeof setInterval> | null = null;

  ngOnChanges(): void {
    this.nettoyerIntervalle();

    if (!this.dateLimite) {
      this.expiree = true;
      this.libelle = '';
      return;
    }

    this.expiree = false;
    this.mettreAJour();
    this.intervalId = setInterval(() => this.mettreAJour(), 1000);
  }

  ngOnDestroy(): void {
    this.nettoyerIntervalle();
  }

  private mettreAJour(): void {
    if (!this.dateLimite) return;

    const restantMs = new Date(this.dateLimite).getTime() - Date.now();

    if (restantMs <= 0) {
      this.libelle = '00:00';
      this.expiree = true;
      this.nettoyerIntervalle();
      this.expire.emit();
      return;
    }

    const minutes = Math.floor(restantMs / 60000);
    const secondes = Math.floor((restantMs % 60000) / 1000);
    this.libelle = `${String(minutes).padStart(2, '0')}:${String(secondes).padStart(2, '0')}`;
  }

  private nettoyerIntervalle(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }
}
