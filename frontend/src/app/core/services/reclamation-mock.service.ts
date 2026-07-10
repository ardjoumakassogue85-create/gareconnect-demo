import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { MessageReclamation, Reclamation } from '../models/metier.model';
import { ReclamationService } from './reclamation.service';

const LATENCE_SIMULEE_MS = 250;

@Injectable()
export class ReclamationMockService implements ReclamationService {
  private readonly reclamations: Reclamation[] = [
    {
      id: 'rec-demo-1',
      client: 'Compte demo',
      sujet: 'Annulation de reservation',
      statut: 'REPONDUE_IA',
      creeLe: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString(),
      majLe: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(),
      messages: [
        this.creerMessage(
          'CLIENT',
          'Je veux annuler ma reservation, comment faire ?',
          new Date(Date.now() - 1000 * 60 * 60 * 4),
        ),
        this.creerMessage(
          'ASSISTANT',
          "Tu peux annuler depuis l'espace client tant que le delai de 30 minutes n'est pas depasse.",
          new Date(Date.now() - 1000 * 60 * 60 * 3),
        ),
      ],
    },
  ];

  listerMesReclamations(): Observable<Reclamation[]> {
    return of(this.reclamations.map((r) => this.copierReclamation(r))).pipe(delay(LATENCE_SIMULEE_MS));
  }

  demarrerReclamation(message: string): Observable<Reclamation> {
    const maintenant = new Date();
    const reclamation: Reclamation = {
      id: crypto.randomUUID(),
       client: 'Compte demo',
      sujet: this.creerSujet(message),
      statut: this.detecterStatut(message),
      creeLe: maintenant.toISOString(),
      majLe: maintenant.toISOString(),
      messages: [this.creerMessage('CLIENT', message, maintenant)],
    };

    reclamation.messages.push(this.creerReponseAutomatique(message));
    this.reclamations.unshift(reclamation);

    return of(this.copierReclamation(reclamation)).pipe(delay(LATENCE_SIMULEE_MS));
  }

  envoyerMessage(reclamationId: string, message: string): Observable<Reclamation> {
    const reclamation = this.reclamations.find((r) => r.id === reclamationId);

    if (!reclamation) {
      return throwError(() => ({ error: { message: 'Reclamation introuvable.' } }));
    }

    const maintenant = new Date();
    reclamation.messages.push(this.creerMessage('CLIENT', message, maintenant));
    reclamation.messages.push(this.creerReponseAutomatique(message));
    reclamation.statut = this.detecterStatut(message);
    reclamation.majLe = new Date().toISOString();

    return of(this.copierReclamation(reclamation)).pipe(delay(LATENCE_SIMULEE_MS));
  }
listerPourCompagnie(): Observable<Reclamation[]> {
    const escaladees = this.reclamations.filter((r) => r.statut === 'EN_ATTENTE_ADMIN' || r.statut === 'RESOLUE_ADMIN');
    return of(escaladees.map((r) => this.copierReclamation(r))).pipe(delay(LATENCE_SIMULEE_MS));
  }

  repondre(reclamationId: string, reponse: string, statut?: string): Observable<Reclamation> {
    const reclamation = this.reclamations.find((r) => r.id === reclamationId);

    if (!reclamation) {
      return throwError(() => ({ error: { message: 'Reclamation introuvable.' } }));
    }

    const maintenant = new Date();
    if (reponse && reponse.trim()) {
      reclamation.messages.push(this.creerMessage('ADMIN', reponse, maintenant));
    }
    reclamation.statut = (statut as Reclamation['statut']) || 'RESOLUE_ADMIN';
    reclamation.majLe = maintenant.toISOString();

    return of(this.copierReclamation(reclamation)).pipe(delay(LATENCE_SIMULEE_MS));
  }
  private creerReponseAutomatique(message: string): MessageReclamation {
    const texte = message.toLowerCase();
    const date = new Date();

    if (this.detecterStatut(message) === 'EN_ATTENTE_ADMIN') {
      return this.creerMessage(
        'ASSISTANT',
        'Je transmets cette reclamation a un administrateur. Tu recevras une reponse ici.',
        date,
      );
    }

    if (texte.includes('annul')) {
      return this.creerMessage(
        'ASSISTANT',
        "Depuis ton espace client, ouvre la reservation puis choisis Annuler si le delai est encore valide.",
        date,
      );
    }

    if (texte.includes('rembours') || texte.includes('paiement')) {
      return this.creerMessage(
        'ASSISTANT',
        'Si le paiement est confirme, le remboursement apparait automatiquement apres une annulation acceptee.',
        date,
      );
    }

    return this.creerMessage(
      'ASSISTANT',
      'Merci pour ton message. Nous avons bien pris en compte ta demande.',
      date,
    );
  }

  private detecterStatut(message: string): Reclamation['statut'] {
    const texte = message.toLowerCase();
    return texte.includes('admin') || texte.includes('urgence') || texte.includes('plainte')
      ? 'EN_ATTENTE_ADMIN'
      : 'REPONDUE_IA';
  }

  private creerSujet(message: string): string {
    const propre = message.trim().replace(/\s+/g, ' ');
    return propre.length > 45 ? propre.slice(0, 42) + '...' : propre || 'Nouvelle reclamation';
  }

  private creerMessage(
    auteur: MessageReclamation['auteur'],
    texte: string,
    date: Date,
  ): MessageReclamation {
    return {
      id: crypto.randomUUID(),
      auteur,
      texte,
      envoyeLe: date.toISOString(),
    };
  }

  private copierReclamation(reclamation: Reclamation): Reclamation {
    return {
      ...reclamation,
      messages: reclamation.messages.map((message) => ({ ...message })),
    };
  }
}
