import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/components/layout/layout.component';
import { ReclamationService } from '../../core/services/reclamation.service';
import { Reclamation, StatutReclamation } from '../../core/models/metier.model';

@Component({
  selector: 'app-reclamation',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LayoutComponent],
  templateUrl: './reclamation.component.html',
  styleUrl: './reclamation.component.scss',
})
export class ReclamationComponent implements OnInit {
  chargement = signal(true);
  reclamations = signal<Reclamation[]>([]);
  reclamationActive = signal<Reclamation | null>(null);
  messageEnCours = '';
  envoiEnCours = signal(false);

  constructor(private readonly reclamationService: ReclamationService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(idAConserver?: string): void {
    this.reclamationService.listerMesReclamations().subscribe((liste) => {
      this.reclamations.set(liste);
      this.chargement.set(false);

      if (idAConserver) {
        this.reclamationActive.set(liste.find((r) => r.id === idAConserver) ?? null);
      }
    });
  }

  ouvrir(r: Reclamation): void {
    this.reclamationActive.set(r);
  }

  nouvelleReclamation(): void {
    this.reclamationActive.set(null);
    this.messageEnCours = '';
  }

  envoyer(): void {
    const texte = this.messageEnCours.trim();
    if (!texte || this.envoiEnCours()) return;

    this.envoiEnCours.set(true);
    const active = this.reclamationActive();

    const flux = active
      ? this.reclamationService.envoyerMessage(active.id, texte)
      : this.reclamationService.demarrerReclamation(texte);

    flux.subscribe({
      next: (reclamation) => {
        this.messageEnCours = '';
        this.envoiEnCours.set(false);
        this.charger(reclamation.id);
      },
      error: () => {
        this.envoiEnCours.set(false);
      },
    });
  }

  libelleStatut(statut: StatutReclamation): string {
    switch (statut) {
      case 'REPONDUE_IA':
        return 'Répondu';
      case 'EN_ATTENTE_ADMIN':
        return 'Transmis à un administrateur';
      case 'RESOLUE_ADMIN':
        return 'Résolu';
    }
  }

  classeStatut(statut: StatutReclamation): string {
    switch (statut) {
      case 'REPONDUE_IA':
        return 'repondue';
      case 'EN_ATTENTE_ADMIN':
        return 'attente';
      case 'RESOLUE_ADMIN':
        return 'resolue';
    }
  }
}
