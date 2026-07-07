import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'recherche',
    canActivate: [roleGuard('CLIENT')],
    loadComponent: () =>
      import('./features/recherche/resultats.component').then((m) => m.ResultatsComponent),
  },
  {
    path: 'reservation/:trajetId',
    canActivate: [roleGuard('CLIENT')],
    loadComponent: () =>
      import('./features/reservation/reservation.component').then((m) => m.ReservationComponent),
  },
  {
    path: 'confirmation/:id',
    canActivate: [roleGuard('CLIENT')],
    loadComponent: () =>
      import('./features/reservation/confirmation.component').then((m) => m.ConfirmationComponent),
  },
  {
    path: 'reclamations',
    canActivate: [roleGuard('CLIENT')],
    loadComponent: () =>
      import('./features/reclamation/reclamation.component').then((m) => m.ReclamationComponent),
  },
  {
    path: 'vitrine/:compagnie',
    loadComponent: () =>
      import('./features/compagnie-vitrine/compagnie-vitrine.component').then(
        (m) => m.CompagnieVitrineComponent,
      ),
  },
  {
    path: 'compagnies/:compagnie',
    redirectTo: 'vitrine/:compagnie',
  },
  {
    path: 'connexion',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'inscription',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'reinitialiser-mot-de-passe',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
  },
  {
    path: 'mon-compte',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/compte/mon-compte.component').then((m) => m.MonCompteComponent),
  },
  {
    path: 'espace-client',
    canActivate: [roleGuard('CLIENT')],
    loadComponent: () =>
      import('./features/espace-client/espace-client.component').then(
        (m) => m.EspaceClientComponent,
      ),
  },
  {
    path: 'espace-compagnie',
    canActivate: [roleGuard('COMPAGNIE')],
    loadComponent: () =>
      import('./features/espace-compagnie/espace-compagnie.component').then(
        (m) => m.EspaceCompagnieComponent,
      ),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
