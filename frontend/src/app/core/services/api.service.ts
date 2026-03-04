import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SendSecretRequest {
  recipientEmail: string;
  message: string;
}

export interface SendSecretResponse {
  accessToken: string;
}

export interface ViewSecretResponse {
  message: string;
}

export interface MeResponse {
  email: string;
  name: string;
  admin: boolean;
}

export interface AuthMethodsResponse {
  oauth2RegistrationIds: string[];
  formLogin: boolean;
}

export interface RecipientOption {
  email: string;
  source: string;
  displayName?: string;
}

export interface AddUserRequest {
  email: string;
  username?: string;
  password?: string;
  group?: string;
}

export interface AllowedUserDto {
  email: string;
  displayName?: string | null;
  source: string;
  externalId: string | null;
  admin: boolean;
  createdAt: string;
  lastLoginAt: string | null;
  addedBy?: string | null;
}

export interface AuditLogEntry {
  eventType: string;
  occurredAt: string;
  messageId?: string;
  senderGroupNames?: string;
  recipientGroupNames?: string;
  sameGroup?: boolean;
  /** Who performed the action (e.g. admin email, or principal for LOGIN). */
  actorIdentifier?: string | null;
  /** Target of the action (e.g. user email, group name). */
  targetIdentifier?: string | null;
  /** Optional details (e.g. "admin=true", "fromGroup→toGroup"). */
  details?: string | null;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number?: number;
}

export interface GroupDto {
  id: number;
  name: string;
}

export interface GroupPermissionDto {
  fromGroupId: number;
  fromGroupName: string;
  toGroupId: number;
  toGroupName: string;
}

/** Backend errors return { message: string }. Use this for consistent user-facing error text. */
export function getApiErrorMessage(err: unknown, fallback = 'Something went wrong'): string {
  if (err && typeof err === 'object' && 'error' in err) {
    const body = (err as { error?: { message?: string } }).error;
    if (body && typeof body === 'object' && typeof body.message === 'string') return body.message;
  }
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}

/** API version base path. All backend REST calls use this prefix. */
export const API_BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = API_BASE;

  constructor(private http: HttpClient) {}

  getMe(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${this.base}/me`, { withCredentials: true });
  }

  /** List of users that can be selected as recipients (for Send page dropdown). */
  getRecipients(): Observable<RecipientOption[]> {
    return this.http.get<RecipientOption[]>(`${this.base}/recipients`, { withCredentials: true });
  }

  getAuthMethods(): Observable<AuthMethodsResponse> {
    return this.http.get<AuthMethodsResponse>(`${this.base}/public/auth-methods`);
  }

  sendSecret(body: SendSecretRequest): Observable<SendSecretResponse> {
    return this.http.post<SendSecretResponse>(`${this.base}/send`, body, { withCredentials: true });
  }

  /** POST with body so session cookie is sent reliably. */
  viewSecret(accessToken: string): Observable<ViewSecretResponse | { locked: boolean }> {
    return this.http.post<ViewSecretResponse | { locked: boolean }>(
      `${this.base}/view`,
      { accessToken },
      { withCredentials: true }
    );
  }

  listAdminUsers(): Observable<AllowedUserDto[]> {
    return this.http.get<AllowedUserDto[]>(`${this.base}/admin/users`, { withCredentials: true });
  }

  addUser(body: AddUserRequest): Observable<{ email: string; source: string }> {
    return this.http.post<{ email: string; source: string }>(`${this.base}/admin/users`, body, { withCredentials: true });
  }

  /** POST with body so session cookie is sent reliably. */
  removeUser(email: string): Observable<void> {
    return this.http.post<void>(`${this.base}/admin/users/remove`, { email }, { withCredentials: true });
  }

  /** POST with body for reliable auth/session handling. */
  setUserAdmin(email: string, admin: boolean): Observable<{ email: string; admin: boolean }> {
    return this.http.post<{ email: string; admin: boolean }>(
      `${this.base}/admin/users/set-admin`,
      { email, admin },
      { withCredentials: true }
    );
  }

  getAuditLog(page = 0, size = 50): Observable<AuditLogPage> {
    return this.http.get<AuditLogPage>(`${this.base}/admin/audit-log?page=${page}&size=${size}`, {
      withCredentials: true,
    });
  }

  listGroups(): Observable<GroupDto[]> {
    return this.http.get<GroupDto[]>(`${this.base}/admin/groups`, { withCredentials: true });
  }

  createGroup(name: string): Observable<GroupDto> {
    return this.http.post<GroupDto>(`${this.base}/admin/groups`, { name }, { withCredentials: true });
  }

  listGroupPermissions(): Observable<GroupPermissionDto[]> {
    return this.http.get<GroupPermissionDto[]>(`${this.base}/admin/group-permissions`, { withCredentials: true });
  }

  addGroupPermission(fromGroupName: string, toGroupName: string): Observable<GroupPermissionDto> {
    return this.http.post<GroupPermissionDto>(
      `${this.base}/admin/group-permissions`,
      { fromGroupName, toGroupName },
      { withCredentials: true }
    );
  }

  /** POST with body for reliable auth/session handling. */
  removeGroupPermission(fromGroupId: number, toGroupId: number): Observable<void> {
    return this.http.post<void>(
      `${this.base}/admin/group-permissions/remove`,
      { fromGroupId, toGroupId },
      { withCredentials: true }
    );
  }
}
