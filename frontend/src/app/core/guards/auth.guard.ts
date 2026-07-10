import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/auth.model';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigateByUrl('/connexion');
  return false;
};

export function roleGuard(requiredRole: Role): CanActivateFn {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      if (requiredRole === 'CLIENT' && state.url.startsWith('/recherche')) {
        authService.memoriserRechercheEnAttente({
          villeDepart: route.queryParamMap.get('villeDepart') ?? undefined,
          villeArrivee: route.queryParamMap.get('villeArrivee') ?? undefined,
          date: route.queryParamMap.get('date') ?? undefined,
          tri: route.queryParamMap.get('tri') === 'prix_asc' ? 'prix_asc' : undefined,
          requete: route.queryParamMap.get('requete') ?? undefined,
        });
        router.navigate(['/connexion'], { queryParams: { retour: 'recherche' } });
        return false;
      }

      router.navigateByUrl('/connexion');
      return false;
    }

    if (authService.role() !== requiredRole) {
      router.navigateByUrl('/');
      return false;
    }

    return true;
  };
}
