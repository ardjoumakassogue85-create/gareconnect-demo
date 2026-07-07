import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

export interface LigneDepart {
  codeGare: string;
  ville: string;
  heure: string;
  compagnie: string;
  prix: number;
  statut: 'A_L_HEURE' | 'BIENTOT' | 'COMPLET';
}

@Component({
  selector: 'app-departure-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './departure-board.component.html',
  styleUrl: './departure-board.component.scss',
})
export class DepartureBoardComponent {
  @Input({ required: true }) lignes: LigneDepart[] = [];

  libelleStatut(statut: LigneDepart['statut']): string {
    switch (statut) {
      case 'A_L_HEURE':
        return 'À l\'heure';
      case 'BIENTOT':
        return 'Départ imminent';
      case 'COMPLET':
        return 'Complet';
    }
  }
}
