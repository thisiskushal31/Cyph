import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService, AuthMethodsResponse } from '../../core/services/api.service';

/**
 * Login page: one screen with three sign-in options.
 * 1. Username & password (admin account)
 * 2. Sign in with SSO (organization IdP)
 * 3. Sign in with Google
 * SSO and Google buttons are shown always; if not configured in backend they appear disabled with a hint.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private api = inject(ApiService);
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  authMethods: AuthMethodsResponse | null = null;
  apiError = false;
  username = '';
  password = '';
  submitting = false;
  formError = '';

  ngOnInit(): void {
    // Show error when redirected back after failed login (e.g. Spring sends 302 to /login?error)
    if (this.route.snapshot.queryParamMap.get('error') != null) {
      this.formError = 'Invalid username or password.';
    }
    this.api.getAuthMethods().subscribe({
      next: (m) => {
        this.authMethods = m;
        this.apiError = false;
      },
      error: () => {
        this.apiError = true;
        this.authMethods = { oauth2RegistrationIds: [], formLogin: false };
      },
    });
  }

  get oauth2Ids(): string[] {
    return this.authMethods?.oauth2RegistrationIds ?? [];
  }

  isSsoConfigured(): boolean {
    return this.oauth2Ids.some((id) => id.toLowerCase() === 'sso');
  }

  isGoogleConfigured(): boolean {
    return this.oauth2Ids.some((id) => id.toLowerCase() === 'google');
  }

  getSsoRegistrationId(): string | null {
    return this.oauth2Ids.find((id) => id.toLowerCase() === 'sso') ?? null;
  }

  getGoogleRegistrationId(): string | null {
    return this.oauth2Ids.find((id) => id.toLowerCase() === 'google') ?? null;
  }

  get redirectUrl(): string {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    let path = redirect?.startsWith('/') ? redirect : (redirect ? '/' + redirect : '/send');
    const pathOnly = path.indexOf('?') >= 0 ? path.slice(0, path.indexOf('?')) : path;
    if (pathOnly === '/login' || pathOnly.startsWith('/login')) path = '/send';
    return path;
  }

  getOAuthUrl(registrationId: string): string {
    const base = `/oauth2/authorization/${registrationId}`;
    const r = this.redirectUrl;
    return r !== '/send' ? `${base}?redirect=${encodeURIComponent(r)}` : base;
  }

  labelFor(registrationId: string): string {
    const lower = registrationId.toLowerCase();
    if (lower === 'sso') return 'SSO';
    if (lower === 'google') return 'Google';
    return registrationId.charAt(0).toUpperCase() + registrationId.slice(1);
  }

  onAdminSubmit(): void {
    this.formError = '';
    this.submitting = true;
    const body = {
      username: this.username.trim(),
      password: this.password,
      redirectUrl: this.redirectUrl,
    };

    this.http
      .post<{ redirectUrl: string }>('/api/auth/login', body, {
        withCredentials: true,
      })
      .subscribe({
        next: (data) => {
          this.submitting = false;
          const target = data?.redirectUrl?.startsWith('/') ? data.redirectUrl : '/' + (data?.redirectUrl ?? this.redirectUrl);
          this.router.navigateByUrl(target);
        },
        error: (err) => {
          this.submitting = false;
          this.formError = err?.error?.message || 'Invalid username or password.';
        },
      });
  }
}
