import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { map, catchError } from 'rxjs';
import { of } from 'rxjs';
import { ApiService } from '../services/api.service';

export const adminGuard: CanActivateFn = () => {
  const api = inject(ApiService);
  const router = inject(Router);
  return api.getMe().pipe(
    map((me) => {
      if (me.admin) return true;
      router.navigate(['/send']);
      return false;
    }),
    catchError(() => {
      router.navigate(['/send']);
      return of(false);
    }),
  );
};
