import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccountResponse,
  AuthResponse,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  LoginRequest,
  RegisterResponse,
  RegisterRequest,
  ResetPasswordRequest,
  ResetPasswordResponse,
  Role,
  UpdateAccountRequest,
  VerifyEmailRequest,
  VerifyEmailResponse,
} from '../models/auth.model';

const STORAGE_KEY = 'gares_auth';
const RECHERCHE_EN_ATTENTE_KEY = 'gares_recherche_en_attente';

interface StoredSession {
  token: string;
  userId: number;
  email: string;
  nom: string;
  role: Role;
  expiresAt: string;
  rememberMe: boolean;
}

export interface RechercheEnAttente {
  villeDepart?: string;
  villeArrivee?: string;
  date?: string;
  tri?: 'prix_asc';
  requete?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly session = signal<StoredSession | null>(this.readFromStorage());

  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly currentUser = computed(() => this.session());
  readonly role = computed(() => this.session()?.role ?? null);

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http
      .post<RegisterResponse>(`${environment.apiUrl}/auth/register`, request)
      .pipe(timeout(8000));
  }

  verifyEmail(request: VerifyEmailRequest): Observable<VerifyEmailResponse> {
    return this.http
      .post<VerifyEmailResponse>(`${environment.apiUrl}/auth/verify-email`, request)
      .pipe(timeout(8000));
  }

  resendVerification(email: string): Observable<RegisterResponse> {
    return this.http
      .post<RegisterResponse>(`${environment.apiUrl}/auth/resend-verification`, { email })
      .pipe(timeout(8000));
  }

  login(request: LoginRequest, rememberMe = false): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, { ...request, rememberMe })
      .pipe(
        timeout(8000),
        tap((response) => this.persistSession(response, rememberMe)),
      );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ForgotPasswordResponse> {
    return this.http
      .post<ForgotPasswordResponse>(`${environment.apiUrl}/auth/forgot-password`, request)
      .pipe(timeout(8000));
  }

  resetPassword(request: ResetPasswordRequest): Observable<ResetPasswordResponse> {
    return this.http
      .post<ResetPasswordResponse>(`${environment.apiUrl}/auth/reset-password`, request)
      .pipe(timeout(8000));
  }

  getAccount(): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${environment.apiUrl}/users/me`).pipe(timeout(8000));
  }

  updateAccount(request: UpdateAccountRequest): Observable<AuthResponse> {
    return this.http
      .put<AuthResponse>(`${environment.apiUrl}/users/me`, request)
      .pipe(
        timeout(8000),
        tap((response) => this.refreshSession(response)),
      );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    this.router.navigateByUrl('/');
  }

  allerAuDashboard(role: Role = this.role() ?? 'CLIENT'): void {
    const destination = role === 'COMPAGNIE' ? '/espace-compagnie' : '/espace-client';
    this.router.navigateByUrl(destination);
  }

  memoriserRechercheEnAttente(recherche: RechercheEnAttente): void {
    sessionStorage.setItem(RECHERCHE_EN_ATTENTE_KEY, JSON.stringify(recherche));
  }

  consommerRechercheEnAttente(): RechercheEnAttente | null {
    const raw = sessionStorage.getItem(RECHERCHE_EN_ATTENTE_KEY);
    if (!raw) return null;

    sessionStorage.removeItem(RECHERCHE_EN_ATTENTE_KEY);

    try {
      return JSON.parse(raw) as RechercheEnAttente;
    } catch {
      return null;
    }
  }

  getToken(): string | null {
    return this.session()?.token ?? null;
  }

  private refreshSession(response: AuthResponse): void {
    const rememberMe = this.session()?.rememberMe ?? false;
    this.persistSession(response, rememberMe);
  }

  private persistSession(response: AuthResponse, rememberMe: boolean): void {
    const toStore: StoredSession = {
      token: response.token,
      userId: response.userId,
      email: response.email,
      nom: response.nom,
      role: response.role,
      expiresAt: response.expiresAt,
      rememberMe,
    };
    const storage = rememberMe ? localStorage : sessionStorage;
    const otherStorage = rememberMe ? sessionStorage : localStorage;
    otherStorage.removeItem(STORAGE_KEY);
    storage.setItem(STORAGE_KEY, JSON.stringify(toStore));
    this.session.set(toStore);
  }

  private readFromStorage(): StoredSession | null {
    const raw = localStorage.getItem(STORAGE_KEY) ?? sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      const session = JSON.parse(raw) as StoredSession;
      if (session.expiresAt && new Date(session.expiresAt).getTime() <= Date.now()) {
        localStorage.removeItem(STORAGE_KEY);
        sessionStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
