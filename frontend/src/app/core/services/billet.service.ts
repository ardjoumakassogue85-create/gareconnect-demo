import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BilletToken,
  ClePublique,
  ValidationBillet,
  VerificationHorsLigne,
} from '../models/billet.model';

const CLE_PUBLIQUE_CACHE = 'gares_cle_billet';

@Injectable({ providedIn: 'root' })
export class BilletService {
  constructor(private readonly http: HttpClient) {}

  /** Jeton signe a encoder dans le QR (client, sa propre reservation). */
  token(reservationId: string): Observable<BilletToken> {
    return this.http
      .get<BilletToken>(`${environment.apiUrl}/billets/token/${reservationId}`)
      .pipe(timeout(8000));
  }

  /** Validation EN LIGNE par un agent (signature + anti-reutilisation). */
  valider(token: string): Observable<ValidationBillet> {
    return this.http
      .post<ValidationBillet>(`${environment.apiUrl}/billets/valider`, { token })
      .pipe(timeout(8000));
  }

  /** Recupere la cle publique et la met en cache pour la verification hors-ligne. */
  clePublique(): Observable<ClePublique> {
    return this.http.get<ClePublique>(`${environment.apiUrl}/billets/cle-publique`).pipe(timeout(8000));
  }

  mettreEnCacheClePublique(cle: string): void {
    localStorage.setItem(CLE_PUBLIQUE_CACHE, cle);
  }

  clePubliqueEnCache(): string | null {
    return localStorage.getItem(CLE_PUBLIQUE_CACHE);
  }

  /**
   * Verifie HORS-LIGNE l'authenticite d'un billet : la signature RSA (RS256) du
   * jeton est verifiee avec la cle publique (Web Crypto), sans aucun reseau.
   * Ne peut pas detecter le double-usage (reconcilie a la reconnexion).
   */
  async verifierHorsLigne(token: string, cleBase64?: string): Promise<VerificationHorsLigne> {
    const cle = cleBase64 ?? this.clePubliqueEnCache();
    if (!cle || !token || token.split('.').length !== 3) {
      return { authentique: false, expire: false, donnees: null };
    }
    try {
      const spki = this.base64VersOctets(cle);
      const cleCrypto = await crypto.subtle.importKey(
        'spki',
        spki,
        { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
        false,
        ['verify'],
      );

      const [entete, charge, signature] = token.split('.');
      const donneesSignees = new TextEncoder().encode(`${entete}.${charge}`);
      const octetsSignature = this.base64UrlVersOctets(signature);

      const authentique = await crypto.subtle.verify(
        'RSASSA-PKCS1-v1_5',
        cleCrypto,
        octetsSignature,
        donneesSignees,
      );

      const donnees = JSON.parse(new TextDecoder().decode(this.base64UrlVersOctets(charge)));
      // Controle d'expiration hors-ligne (exp en secondes Unix).
      const exp = typeof donnees['exp'] === 'number' ? (donnees['exp'] as number) : null;
      const expire = exp !== null && exp * 1000 < Date.now();
      return { authentique, expire, donnees };
    } catch {
      return { authentique: false, expire: false, donnees: null };
    }
  }

  private base64VersOctets(base64: string): Uint8Array {
    const binaire = atob(base64);
    const octets = new Uint8Array(binaire.length);
    for (let i = 0; i < binaire.length; i++) {
      octets[i] = binaire.charCodeAt(i);
    }
    return octets;
  }

  private base64UrlVersOctets(base64url: string): Uint8Array {
    let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) {
      base64 += '=';
    }
    return this.base64VersOctets(base64);
  }
}
