import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/send', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent) },
  { path: 'send', loadComponent: () => import('./pages/send/send.component').then((m) => m.SendComponent), canActivate: [authGuard] },
  { path: 'view/:token', loadComponent: () => import('./pages/view/view.component').then((m) => m.ViewComponent), canActivate: [authGuard] },
  { path: 'admin', loadComponent: () => import('./pages/admin/admin.component').then((m) => m.AdminComponent), canActivate: [authGuard, adminGuard] },
  { path: 'log', loadComponent: () => import('./pages/log/log.component').then((m) => m.LogComponent), canActivate: [authGuard, adminGuard] },
  { path: '**', redirectTo: '/send' },
];
