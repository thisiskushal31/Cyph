import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { AdminComponent } from './pages/admin/admin.component';
import { LogComponent } from './pages/log/log.component';

export const routes: Routes = [
  { path: '', redirectTo: '/send', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent) },
  { path: 'send', loadComponent: () => import('./pages/send/send.component').then((m) => m.SendComponent), canActivate: [authGuard] },
  { path: 'view/:token', loadComponent: () => import('./pages/view/view.component').then((m) => m.ViewComponent), canActivate: [authGuard] },
  { path: 'credentials', loadComponent: () => import('./pages/credentials/credentials.component').then((m) => m.CredentialsComponent), canActivate: [authGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [authGuard, adminGuard] },
  { path: 'log', component: LogComponent, canActivate: [authGuard, adminGuard] },
  { path: '**', redirectTo: '/send' },
];
