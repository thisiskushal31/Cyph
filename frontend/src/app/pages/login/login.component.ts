import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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
  imports: [CommonModule, FormsModule, RouterLink],
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
    const body = new URLSearchParams();
    body.set('username', this.username.trim());
    body.set('password', this.password);
    body.set('redirect', this.redirectUrl);

    console.log('[Cyph Login] POST /login', { username: this.username.trim(), redirectUrl: this.redirectUrl });

    this.http
      .post('/login', body.toString(), {
        headers: new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded'),
        withCredentials: true,
        observe: 'response',
        responseType: 'text',
      })
      .subscribe({
        next: (res) => {
          this.submitting = false;
          console.log('[Cyph Login] Response', {
            status: res.status,
            statusText: res.statusText,
            bodyLength: res.body?.length ?? 0,
            bodyPreview: res.body?.slice(0, 120) ?? '',
            hasSetCookie: res.headers.has('Set-Cookie'),
          });
          if (res.status === 200 && res.body) {
            try {
              const data = JSON.parse(res.body) as { redirectUrl?: string };
              const url = data?.redirectUrl ?? this.redirectUrl;
              const target = url.startsWith('/') ? url : '/' + url;
              console.log('[Cyph Login] Success, navigating to', target);
              this.router.navigateByUrl(target);
              return;
            } catch (e) {
              console.warn('[Cyph Login] Response was 200 but body is not JSON', e);
            }
          }
          console.warn('[Cyph Login] Treating as failure (non-200 or non-JSON)');
          this.formError = 'Invalid username or password.';
        },
        error: (err) => {
          this.submitting = false;
          console.error('[Cyph Login] Request failed', {
            status: err?.status,
            statusText: err?.statusText,
            message: err?.message,
            error: err,
          });
          this.formError = 'Invalid username or password.';
        },
      });
  }
}
