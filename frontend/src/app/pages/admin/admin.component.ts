import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, API_BASE, getApiErrorMessage, AllowedUserDto, MeResponse, GroupDto, GroupPermissionDto, StoredCredentialDto, CreateSharedCredentialRequest, SharedCredentialAdminDto, UpdateSharedCredentialRequest } from '../../core/services/api.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="w-full max-w-6xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold tracking-tight text-slate-100 sm:text-3xl">Admin</h1>
        <p class="mt-2 text-slate-400">Manage groups, who can talk to whom, and users.</p>
      </div>

      <div class="space-y-6">
        <section class="app-card p-6">
          <h2 class="text-lg font-semibold text-slate-800">Groups</h2>
          <p class="mt-1 text-sm text-slate-500">Pre-configure groups; assign them when adding users. Same-group messages are readable; use permissions below to allow cross-group.</p>
          <div class="mt-4 flex flex-wrap items-center gap-3">
            <input
              type="text"
              [(ngModel)]="newGroupName"
              name="newGroupName"
              placeholder="Group name"
              class="app-input min-w-[12rem]"
            />
            <button type="button" (click)="onAddGroup()" [disabled]="!newGroupName.trim() || addingGroup" class="app-btn-primary">
              {{ addingGroup ? '…' : 'Add group' }}
            </button>
          </div>
          @if (groups.length > 0) {
            <p class="mt-3 text-sm text-slate-600">Existing: {{ groupNamesDisplay }}</p>
          }
          @if (groupError) {
            <p class="mt-2 text-sm text-red-600">{{ groupError }}</p>
          }
        </section>

        <section class="app-card p-6">
          <h2 class="text-lg font-semibold text-slate-800">Group permissions (who can send to whom)</h2>
          <p class="mt-1 text-sm text-slate-500">Allow one group to send readable messages to another. Without a permission, cross-group messages are locked.</p>
          <div class="mt-4 flex flex-wrap items-center gap-3">
            <select [(ngModel)]="permFromGroupName" name="permFrom" class="app-input">
              <option value="">From group</option>
              @for (g of groups; track g.id) {
                <option [value]="g.name">{{ g.name }}</option>
              }
            </select>
            <span class="text-slate-500 font-medium">→</span>
            <select [(ngModel)]="permToGroupName" name="permTo" class="app-input">
              <option value="">To group</option>
              @for (g of groups; track g.id) {
                <option [value]="g.name">{{ g.name }}</option>
              }
            </select>
            <button type="button" (click)="onAddPermission()" [disabled]="!permFromGroupName || !permToGroupName || permFromGroupName === permToGroupName || addingPerm" class="app-btn-primary">
              {{ addingPerm ? '…' : 'Add' }}
            </button>
          </div>
          @if (groupPermissions.length > 0) {
            <div class="mt-4 overflow-x-auto rounded-xl border border-slate-200 bg-slate-50/50">
              <table class="min-w-full divide-y divide-slate-200">
                <thead class="bg-slate-100/80">
                  <tr>
                    <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">From</th>
                    <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">To</th>
                    <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">Actions</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200 bg-white">
                  @for (p of groupPermissions; track p.fromGroupId + '-' + p.toGroupId) {
                    <tr>
                      <td class="px-4 py-3 text-sm font-medium text-slate-900">{{ p.fromGroupName }}</td>
                      <td class="px-4 py-3 text-sm text-slate-600">{{ p.toGroupName }}</td>
                      <td class="px-4 py-3 text-right">
                        <button type="button" (click)="removePermission(p)" class="rounded-lg px-2.5 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50">Remove</button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
          @if (permError) {
            <p class="mt-2 text-sm text-red-600">{{ permError }}</p>
          }
        </section>

        <section class="app-card p-6">
          <h2 class="text-lg font-semibold text-slate-800">Add user</h2>
          <p class="mt-1 text-sm text-slate-500">You will be recorded as the one who added this user.</p>
          <form (ngSubmit)="onAdd()" class="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label for="newEmail" class="block text-sm font-semibold text-slate-700">Email</label>
              <input id="newEmail" type="email" [(ngModel)]="newEmail" name="newEmail" placeholder="user@example.com" class="app-input mt-2" />
            </div>
            <div>
              <label for="newUsername" class="block text-sm font-semibold text-slate-700">Username</label>
              <input id="newUsername" type="text" [(ngModel)]="newUsername" name="newUsername" placeholder="Display name" class="app-input mt-2" />
            </div>
            <div>
              <label for="newPassword" class="block text-sm font-semibold text-slate-700">Password</label>
              <input id="newPassword" type="password" [(ngModel)]="newPassword" name="newPassword" placeholder="Optional for form login" class="app-input mt-2" />
            </div>
            <div>
              <label for="newGroup" class="block text-sm font-semibold text-slate-700">Group</label>
              <select id="newGroup" [(ngModel)]="newGroup" name="newGroup" class="app-input mt-2">
                <option value="">No group</option>
                @for (g of groups; track g.id) {
                  <option [value]="g.name">{{ g.name }}</option>
                }
              </select>
            </div>
            <div class="sm:col-span-2 lg:col-span-4 flex items-end">
              <button type="submit" [disabled]="!newEmail.trim() || adding" class="app-btn-primary">
                {{ adding ? 'Adding…' : 'Add user' }}
              </button>
            </div>
          </form>
          @if (addError) {
            <p class="mt-2 text-sm text-red-600">{{ addError }}</p>
          }
        </section>

        <section class="app-card p-6">
          <h2 class="text-lg font-semibold text-slate-800">Shared credentials</h2>
          <p class="mt-1 text-sm text-slate-500">Push credentials to users or groups. They appear in the extension alongside each user's personal credentials.</p>
          <form (ngSubmit)="onAddCredential()" class="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
              <label for="credLabel" class="block text-sm font-semibold text-slate-700">Label</label>
              <input id="credLabel" type="text" [(ngModel)]="credLabel" name="credLabel" placeholder="e.g. Grafana - Engineering" class="app-input mt-2 w-full" />
            </div>
            <div>
              <label for="credUrl" class="block text-sm font-semibold text-slate-700">URL</label>
              <input id="credUrl" type="url" [(ngModel)]="credUrl" name="credUrl" placeholder="https://monitor.example.com" class="app-input mt-2 w-full" />
            </div>
            <div>
              <label for="credUsername" class="block text-sm font-semibold text-slate-700">Username</label>
              <input id="credUsername" type="text" [(ngModel)]="credUsername" name="credUsername" placeholder="Login name" class="app-input mt-2 w-full" />
            </div>
            <div>
              <label for="credSecret" class="block text-sm font-semibold text-slate-700">Secret / password</label>
              <input id="credSecret" type="password" [(ngModel)]="credSecret" name="credSecret" placeholder="••••••••" class="app-input mt-2 w-full" />
            </div>
            <div class="sm:col-span-2">
              <label class="block text-sm font-semibold text-slate-700">Assign to users</label>
              <div class="mt-2 flex flex-wrap items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/90 p-2 min-h-[2.75rem]">
                @for (email of credAssignUserEmails; track email) {
                  <span class="inline-flex items-center gap-1 rounded-lg bg-indigo-100 px-2.5 py-1 text-sm font-medium text-indigo-800">
                    {{ email }}
                    <button type="button" (click)="removeCredUser(email)" class="rounded p-0.5 hover:bg-indigo-200" aria-label="Remove">&#215;</button>
                  </span>
                }
                <select
                  (change)="addCredUserFromSelect($event)"
                  class="min-w-[10rem] flex-1 rounded-lg border-0 bg-transparent py-1 text-sm text-slate-600 focus:ring-0 focus:outline-none"
                >
                  <option value="">Add user...</option>
                  @for (u of users; track u.email) {
                    @if (!credAssignUserEmails.includes(u.email)) {
                      <option [value]="u.email">{{ u.email }}</option>
                    }
                  }
                </select>
              </div>
            </div>
            <div class="sm:col-span-2">
              <label class="block text-sm font-semibold text-slate-700">Assign to groups</label>
              <div class="mt-2 flex flex-wrap items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/90 p-2 min-h-[2.75rem]">
                @for (name of credAssignGroupNames; track name) {
                  <span class="inline-flex items-center gap-1 rounded-lg bg-slate-200 px-2.5 py-1 text-sm font-medium text-slate-800">
                    {{ name }}
                    <button type="button" (click)="removeCredGroup(name)" class="rounded p-0.5 hover:bg-slate-300" aria-label="Remove">&#215;</button>
                  </span>
                }
                <select
                  (change)="addCredGroupFromSelect($event)"
                  class="min-w-[10rem] flex-1 rounded-lg border-0 bg-transparent py-1 text-sm text-slate-600 focus:ring-0 focus:outline-none"
                >
                  <option value="">Add group...</option>
                  @for (g of groups; track g.id) {
                    @if (!credAssignGroupNames.includes(g.name)) {
                      <option [value]="g.name">{{ g.name }}</option>
                    }
                  }
                </select>
              </div>
            </div>
            <div class="sm:col-span-2">
              <button type="submit" [disabled]="!credLabel.trim() || addingCred" class="app-btn-primary">{{ addingCred ? '…' : 'Push shared credential' }}</button>
            </div>
          </form>
          @if (credError) {
            <p class="mt-2 text-sm text-red-600">{{ credError }}</p>
          }
          @if (sharedCredentials.length > 0) {
            <div class="mt-4 overflow-x-auto rounded-xl border border-slate-200 bg-slate-50/50">
              <table class="min-w-full divide-y divide-slate-200">
                <thead class="bg-slate-100/80">
                  <tr>
                    <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Label</th>
                    <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">URL</th>
                    <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Username</th>
                    <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">Actions</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-200 bg-white">
                  @for (c of sharedCredentials; track c.id) {
                    <tr>
                      <td class="px-4 py-3 text-sm font-medium text-slate-900">{{ c.label }}</td>
                      <td class="max-w-[14rem] truncate px-4 py-3 text-sm text-slate-600">{{ c.url || '—' }}</td>
                      <td class="px-4 py-3 text-sm text-slate-600">{{ c.usernameMeta || '—' }}</td>
                      <td class="px-4 py-3 text-right whitespace-nowrap">
                        <button type="button" (click)="startEditSharedCredential(c.id)" class="rounded-lg px-2.5 py-1.5 text-sm font-medium text-indigo-600 hover:bg-indigo-50 mr-2">Edit</button>
                        <button type="button" (click)="deleteCredential(c.id)" [disabled]="deletingCred === c.id" class="rounded-lg px-2.5 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50">{{ deletingCred === c.id ? '…' : 'Revoke' }}</button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        @if (editingSharedCredential) {
          <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" (click)="cancelEditSharedCredential()">
            <div class="app-card p-6 max-w-lg w-full shadow-xl max-h-[90vh] overflow-y-auto" (click)="$event.stopPropagation()">
              <h3 class="text-lg font-semibold text-slate-800">Edit shared credential</h3>
              <p class="mt-1 text-sm text-slate-500">Change label, URL, username, or assignments. Leave secret blank to keep the current password.</p>
              <form (ngSubmit)="saveEditSharedCredential()" class="mt-4 space-y-4">
                <div>
                  <label class="block text-sm font-semibold text-slate-700">Label</label>
                  <input type="text" [(ngModel)]="editCredLabel" name="editCredLabel" class="app-input mt-2 w-full" />
                </div>
                <div>
                  <label class="block text-sm font-semibold text-slate-700">URL</label>
                  <input type="url" [(ngModel)]="editCredUrl" name="editCredUrl" class="app-input mt-2 w-full" />
                </div>
                <div>
                  <label class="block text-sm font-semibold text-slate-700">Username</label>
                  <input type="text" [(ngModel)]="editCredUsername" name="editCredUsername" class="app-input mt-2 w-full" />
                </div>
                <div>
                  <label class="block text-sm font-semibold text-slate-700">Secret (leave blank to keep current)</label>
                  <input type="password" [(ngModel)]="editCredSecret" name="editCredSecret" placeholder="••••••••" class="app-input mt-2 w-full" />
                </div>
                <div>
                  <label class="block text-sm font-semibold text-slate-700">Assign to users</label>
                  <div class="mt-2 flex flex-wrap items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/90 p-2 min-h-[2.75rem]">
                    @for (email of editCredAssignUserEmails; track email) {
                      <span class="inline-flex items-center gap-1 rounded-lg bg-indigo-100 px-2.5 py-1 text-sm font-medium text-indigo-800">
                        {{ email }}
                        <button type="button" (click)="removeEditCredUser(email)" class="rounded p-0.5 hover:bg-indigo-200" aria-label="Remove">&#215;</button>
                      </span>
                    }
                    <select
                      (change)="addEditCredUserFromSelect($event)"
                      class="min-w-[10rem] flex-1 rounded-lg border-0 bg-transparent py-1 text-sm text-slate-600 focus:ring-0 focus:outline-none"
                    >
                      <option value="">Add user...</option>
                      @for (u of users; track u.email) {
                        @if (!editCredAssignUserEmails.includes(u.email)) {
                          <option [value]="u.email">{{ u.email }}</option>
                        }
                      }
                    </select>
                  </div>
                </div>
                <div>
                  <label class="block text-sm font-semibold text-slate-700">Assign to groups</label>
                  <div class="mt-2 flex flex-wrap items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/90 p-2 min-h-[2.75rem]">
                    @for (name of editCredAssignGroupNames; track name) {
                      <span class="inline-flex items-center gap-1 rounded-lg bg-slate-200 px-2.5 py-1 text-sm font-medium text-slate-800">
                        {{ name }}
                        <button type="button" (click)="removeEditCredGroup(name)" class="rounded p-0.5 hover:bg-slate-300" aria-label="Remove">&#215;</button>
                      </span>
                    }
                    <select
                      (change)="addEditCredGroupFromSelect($event)"
                      class="min-w-[10rem] flex-1 rounded-lg border-0 bg-transparent py-1 text-sm text-slate-600 focus:ring-0 focus:outline-none"
                    >
                      <option value="">Add group...</option>
                      @for (g of groups; track g.id) {
                        @if (!editCredAssignGroupNames.includes(g.name)) {
                          <option [value]="g.name">{{ g.name }}</option>
                        }
                      }
                    </select>
                  </div>
                </div>
                <div class="flex gap-2 justify-end pt-2">
                  <button type="button" (click)="cancelEditSharedCredential()" class="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Cancel</button>
                  <button type="submit" [disabled]="savingEditCred" class="app-btn-primary">{{ savingEditCred ? '…' : 'Save' }}</button>
                </div>
              </form>
              @if (editCredError) {
                <p class="mt-2 text-sm text-red-600">{{ editCredError }}</p>
              }
            </div>
          </div>
        }

        <section class="app-card overflow-hidden">
          <div class="p-6 pb-4">
            <h2 class="text-lg font-semibold text-slate-800">Users</h2>
            <p class="mt-1 text-sm text-slate-500">Users can sign in via SSO, Google, or be added here. Toggle admin to allow access to this panel.</p>
          </div>
          <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-slate-200">
            <thead class="bg-slate-100/80">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Email</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Username</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Source</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">User ID (external)</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Admin</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Added by</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Last login</th>
                <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200 bg-white">
              @for (u of users; track u.email) {
                <tr class="hover:bg-slate-50/80 transition">
                  <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900">{{ u.email }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-600">{{ u.displayName || '—' }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <span class="rounded-lg bg-indigo-100 px-2.5 py-1 text-xs font-medium text-indigo-700">{{ u.source }}</span>
                  </td>
                  <td class="max-w-[12rem] truncate px-4 py-3 text-sm text-slate-500" [title]="u.externalId || ''">{{ u.externalId || '—' }}</td>
                  <td class="whitespace-nowrap px-4 py-3">
                    <button
                      type="button"
                      (click)="toggleAdmin(u)"
                      [disabled]="toggling === u.email"
                      class="rounded-lg px-2.5 py-1.5 text-xs font-semibold transition"
                      [class.bg-indigo-100]="u.admin"
                      [class.text-indigo-700]="u.admin"
                      [class.bg-slate-100]="!u.admin"
                      [class.text-slate-600]="!u.admin"
                    >
                      {{ u.admin ? 'Admin' : 'User' }}
                    </button>
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-500">{{ addedByLabel(u) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-500">{{ formatDate(u.lastLoginAt) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-right">
                    @if (!u.admin) {
                      <button type="button" (click)="remove(u.email)" [disabled]="removing === u.email" class="rounded-lg px-2.5 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-50">
                        {{ removing === u.email ? '…' : 'Remove' }}
                      </button>
                    } @else {
                      <span class="text-slate-400 text-sm">—</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
          </div>
          @if (users.length === 0 && !loading) {
            <p class="px-6 py-8 text-center text-slate-500 text-sm">No users yet. Add one above or sign in with SSO/Google to populate.</p>
          }
          @if (removeError) {
            <div class="px-6 pb-6">
              <p class="text-sm text-red-600">{{ removeError }}</p>
              <p class="mt-1 text-sm text-slate-500">
                If you see 403 Forbidden, the backend may not see your session.
                <a [href]="apiBase + '/auth/session-info'" target="_blank" rel="noopener" class="text-indigo-600 hover:underline font-medium">Open session-info</a>
                in the same tab to check what the backend sees (principal and isAdmin).
              </p>
            </div>
          }
          @if (loading) {
            <p class="px-6 pb-6 text-slate-500 text-sm">Loading…</p>
          }
        </section>
      </div>
    </div>
  `,
})
export class AdminComponent implements OnInit {
  users: AllowedUserDto[] = [];
  loading = false;
  me: MeResponse | null = null;
  newEmail = '';
  newUsername = '';
  newPassword = '';
  newGroup = '';
  adding = false;
  addError: string | null = null;
  removing: string | null = null;
  removeError: string | null = null;
  toggling: string | null = null;

  groups: GroupDto[] = [];
  newGroupName = '';
  addingGroup = false;
  groupError: string | null = null;

  groupPermissions: GroupPermissionDto[] = [];
  permFromGroupName = '';
  permToGroupName = '';
  addingPerm = false;
  permError: string | null = null;

  sharedCredentials: StoredCredentialDto[] = [];
  credLabel = '';
  credUrl = '';
  credUsername = '';
  credSecret = '';
  credAssignUserEmails: string[] = [];
  credAssignGroupNames: string[] = [];
  addingCred = false;
  credError: string | null = null;
  deletingCred: number | null = null;

  editingSharedCredential: number | null = null;
  editCredLabel = '';
  editCredUrl = '';
  editCredUsername = '';
  editCredSecret = '';
  editCredAssignUserEmails: string[] = [];
  editCredAssignGroupNames: string[] = [];
  savingEditCred = false;
  editCredError: string | null = null;

  readonly apiBase = API_BASE;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getMe().subscribe({ next: (m) => (this.me = m) });
    this.loadGroups();
    this.loadGroupPermissions();
    this.loadUsers();
    this.loadSharedCredentials();
  }

  loadSharedCredentials(): void {
    this.api.listAdminCredentials().subscribe({
      next: (list) => (this.sharedCredentials = list),
      error: () => (this.sharedCredentials = []),
    });
  }

  addCredUserFromSelect(event: Event): void {
    const sel = event.target as HTMLSelectElement;
    const v = sel?.value?.trim();
    if (v && !this.credAssignUserEmails.includes(v)) this.credAssignUserEmails = [...this.credAssignUserEmails, v];
    if (sel) sel.value = '';
  }

  removeCredUser(email: string): void {
    this.credAssignUserEmails = this.credAssignUserEmails.filter((e) => e !== email);
  }

  addCredGroupFromSelect(event: Event): void {
    const sel = event.target as HTMLSelectElement;
    const v = sel?.value?.trim();
    if (v && !this.credAssignGroupNames.includes(v)) this.credAssignGroupNames = [...this.credAssignGroupNames, v];
    if (sel) sel.value = '';
  }

  removeCredGroup(name: string): void {
    this.credAssignGroupNames = this.credAssignGroupNames.filter((n) => n !== name);
  }

  onAddCredential(): void {
    if (!this.credLabel.trim()) return;
    this.credError = null;
    this.addingCred = true;
    const body: CreateSharedCredentialRequest = {
      label: this.credLabel.trim(),
      url: this.credUrl.trim() || undefined,
      usernameMeta: this.credUsername.trim() || undefined,
      secret: this.credSecret,
      assignToUserEmails: this.credAssignUserEmails.length ? [...this.credAssignUserEmails] : undefined,
      assignToGroupNames: this.credAssignGroupNames.length ? [...this.credAssignGroupNames] : undefined,
    };
    this.api.createAdminCredential(body).subscribe({
      next: () => {
        this.credLabel = '';
        this.credUrl = '';
        this.credUsername = '';
        this.credSecret = '';
        this.credAssignUserEmails = [];
        this.credAssignGroupNames = [];
        this.addingCred = false;
        this.loadSharedCredentials();
      },
      error: (err) => {
        this.credError = getApiErrorMessage(err, 'Failed to push credential.');
        this.addingCred = false;
      },
    });
  }

  deleteCredential(id: number): void {
    this.deletingCred = id;
    this.api.deleteAdminCredential(id).subscribe({
      next: () => {
        this.deletingCred = null;
        this.loadSharedCredentials();
      },
      error: () => (this.deletingCred = null),
    });
  }

  addEditCredUserFromSelect(event: Event): void {
    const sel = event.target as HTMLSelectElement;
    const v = sel?.value?.trim();
    if (v && !this.editCredAssignUserEmails.includes(v)) this.editCredAssignUserEmails = [...this.editCredAssignUserEmails, v];
    if (sel) sel.value = '';
  }

  removeEditCredUser(email: string): void {
    this.editCredAssignUserEmails = this.editCredAssignUserEmails.filter((e) => e !== email);
  }

  addEditCredGroupFromSelect(event: Event): void {
    const sel = event.target as HTMLSelectElement;
    const v = sel?.value?.trim();
    if (v && !this.editCredAssignGroupNames.includes(v)) this.editCredAssignGroupNames = [...this.editCredAssignGroupNames, v];
    if (sel) sel.value = '';
  }

  removeEditCredGroup(name: string): void {
    this.editCredAssignGroupNames = this.editCredAssignGroupNames.filter((n) => n !== name);
  }

  startEditSharedCredential(id: number): void {
    this.editingSharedCredential = id;
    this.editCredError = null;
    this.editCredSecret = '';
    this.api.getAdminCredential(id).subscribe({
      next: (d) => {
        this.editCredLabel = d.label ?? '';
        this.editCredUrl = d.url ?? '';
        this.editCredUsername = d.usernameMeta ?? '';
        this.editCredAssignUserEmails = [...(d.assignToUserEmails ?? [])];
        this.editCredAssignGroupNames = [...(d.assignToGroupNames ?? [])];
      },
      error: (err) => {
        this.editCredError = getApiErrorMessage(err, 'Failed to load credential.');
        this.editingSharedCredential = null;
      },
    });
  }

  cancelEditSharedCredential(): void {
    this.editingSharedCredential = null;
    this.editCredError = null;
  }

  saveEditSharedCredential(): void {
    if (this.editingSharedCredential == null) return;
    const label = this.editCredLabel.trim();
    if (!label) {
      this.editCredError = 'Label is required.';
      return;
    }
    this.editCredError = null;
    this.savingEditCred = true;
    const body: UpdateSharedCredentialRequest = {
      label,
      url: this.editCredUrl.trim() || undefined,
      usernameMeta: this.editCredUsername.trim() || undefined,
      assignToUserEmails: [...this.editCredAssignUserEmails],
      assignToGroupNames: [...this.editCredAssignGroupNames],
    };
    if (this.editCredSecret.trim()) body.secret = this.editCredSecret;
    this.api.updateAdminCredential(this.editingSharedCredential, body).subscribe({
      next: () => {
        this.savingEditCred = false;
        this.editingSharedCredential = null;
        this.loadSharedCredentials();
      },
      error: (err) => {
        this.editCredError = getApiErrorMessage(err, 'Failed to update.');
        this.savingEditCred = false;
      },
    });
  }

  get groupNamesDisplay(): string {
    return this.groups.map((g) => g.name).join(', ');
  }

  loadGroups(): void {
    this.api.listGroups().subscribe({
      next: (list) => (this.groups = list),
      error: () => (this.groups = []),
    });
  }

  loadGroupPermissions(): void {
    this.api.listGroupPermissions().subscribe({
      next: (list) => (this.groupPermissions = list),
      error: () => (this.groupPermissions = []),
    });
  }

  onAddGroup(): void {
    const name = this.newGroupName.trim();
    if (!name) return;
    this.groupError = null;
    this.addingGroup = true;
    this.api.createGroup(name).subscribe({
      next: () => {
        this.newGroupName = '';
        this.addingGroup = false;
        this.loadGroups();
      },
      error: (err) => {
        this.groupError = getApiErrorMessage(err, 'Failed to add group.');
        this.addingGroup = false;
      },
    });
  }

  onAddPermission(): void {
    if (!this.permFromGroupName || !this.permToGroupName || this.permFromGroupName === this.permToGroupName) return;
    this.permError = null;
    this.addingPerm = true;
    this.api.addGroupPermission(this.permFromGroupName, this.permToGroupName).subscribe({
      next: () => {
        this.addingPerm = false;
        this.loadGroupPermissions();
      },
      error: (err) => {
        this.permError = getApiErrorMessage(err, 'Failed to add permission.');
        this.addingPerm = false;
      },
    });
  }

  removePermission(p: GroupPermissionDto): void {
    this.api.removeGroupPermission(p.fromGroupId, p.toGroupId).subscribe({
      next: () => this.loadGroupPermissions(),
    });
  }

  loadUsers(): void {
    this.loading = true;
    this.api.listAdminUsers().subscribe({
      next: (list) => {
        this.users = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  onAdd(): void {
    const email = this.newEmail.trim();
    if (!email) return;
    this.addError = null;
    this.adding = true;
    this.api.addUser({
      email,
      username: this.newUsername.trim() || undefined,
      password: this.newPassword.trim() || undefined,
      group: this.newGroup ? this.newGroup.trim() || undefined : undefined,
    }).subscribe({
      next: () => {
        this.newEmail = '';
        this.newUsername = '';
        this.newPassword = '';
        this.newGroup = '';
        this.adding = false;
        this.loadUsers();
      },
      error: (err) => {
        this.addError = getApiErrorMessage(err, 'Failed to add user.');
        this.adding = false;
      },
    });
  }

  addedByLabel(u: AllowedUserDto): string {
    if (!u.addedBy) return '—';
    return u.addedBy === this.me?.email ? 'You' : u.addedBy;
  }

  remove(email: string): void {
    this.removeError = null;
    this.removing = email;
    this.api.removeUser(email).subscribe({
      next: () => {
        this.removing = null;
        this.loadUsers();
      },
      error: (err) => {
        this.removing = null;
        this.removeError = getApiErrorMessage(err, 'Failed to remove user.');
      },
    });
  }

  toggleAdmin(u: AllowedUserDto): void {
    this.toggling = u.email;
    this.api.setUserAdmin(u.email, !u.admin).subscribe({
      next: () => {
        this.toggling = null;
        this.loadUsers();
      },
      error: () => {
        this.toggling = null;
      },
    });
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    try {
      const d = new Date(iso);
      return d.toLocaleString();
    } catch {
      return iso;
    }
  }
}
