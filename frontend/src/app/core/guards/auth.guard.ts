import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { map, catchError, take, of } from 'rxjs';
import { ApiService } from '../services/api.service';

/**
 * Protects routes by verifying the session with the backend (GET /api/v1/me).
 * Redirects to /login if not authenticated.
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const api = inject(ApiService);
  const redirect = window.location.pathname + window.location.search;
  const loginUrl = router.createUrlTree(['/login'], {
    queryParams: redirect && redirect !== '/' ? { redirect } : {},
  });

  return api.getMe().pipe(
    take(1),
    map(() => true),
    catchError(() => of(loginUrl)),
  );
};
