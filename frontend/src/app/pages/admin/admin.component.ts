import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, AllowedUserDto, MeResponse, GroupDto, GroupPermissionDto } from '../../core/services/api.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="w-full px-4 py-8 sm:px-6 lg:px-8 xl:px-10">
      <h1 class="text-2xl font-bold text-slate-900">Admin</h1>
      <p class="mt-1 text-slate-600">Manage groups, who can talk to whom, and users.</p>

      <div class="mt-8">
        <h2 class="text-lg font-semibold text-slate-800">Groups</h2>
        <p class="mt-1 text-sm text-slate-500">Pre-configure groups; assign them when adding users. Same-group messages are readable; use permissions below to allow cross-group.</p>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <input
            type="text"
            [(ngModel)]="newGroupName"
            name="newGroupName"
            placeholder="Group name"
            class="rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
          />
          <button
            type="button"
            (click)="onAddGroup()"
            [disabled]="!newGroupName.trim() || addingGroup"
            class="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
          >
            {{ addingGroup ? '…' : 'Add group' }}
          </button>
        </div>
        @if (groups.length > 0) {
          <p class="mt-2 text-sm text-slate-600">Existing: {{ groupNamesDisplay }}</p>
        }
        @if (groupError) {
          <p class="mt-2 text-sm text-red-600">{{ groupError }}</p>
        }
      </div>

      <div class="mt-8">
        <h2 class="text-lg font-semibold text-slate-800">Group permissions (who can send to whom)</h2>
        <p class="mt-1 text-sm text-slate-500">Allow one group to send readable messages to another. Without a permission, cross-group messages are locked.</p>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <select
            [(ngModel)]="permFromGroupName"
            name="permFrom"
            class="rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
          >
            <option value="">From group</option>
            @for (g of groups; track g.id) {
              <option [value]="g.name">{{ g.name }}</option>
            }
          </select>
          <span class="text-slate-500">→</span>
          <select
            [(ngModel)]="permToGroupName"
            name="permTo"
            class="rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
          >
            <option value="">To group</option>
            @for (g of groups; track g.id) {
              <option [value]="g.name">{{ g.name }}</option>
            }
          </select>
          <button
            type="button"
            (click)="onAddPermission()"
            [disabled]="!permFromGroupName || !permToGroupName || permFromGroupName === permToGroupName || addingPerm"
            class="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
          >
            {{ addingPerm ? '…' : 'Add' }}
          </button>
        </div>
        @if (groupPermissions.length > 0) {
          <div class="mt-3 overflow-x-auto rounded-lg border border-slate-200 bg-white shadow">
            <table class="min-w-full divide-y divide-slate-200">
              <thead class="bg-slate-50">
                <tr>
                  <th class="px-4 py-2 text-left text-xs font-medium uppercase text-slate-500">From</th>
                  <th class="px-4 py-2 text-left text-xs font-medium uppercase text-slate-500">To</th>
                  <th class="px-4 py-2 text-right text-xs font-medium uppercase text-slate-500">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200">
                @for (p of groupPermissions; track p.fromGroupId + '-' + p.toGroupId) {
                  <tr>
                    <td class="px-4 py-2 text-sm text-slate-900">{{ p.fromGroupName }}</td>
                    <td class="px-4 py-2 text-sm text-slate-600">{{ p.toGroupName }}</td>
                    <td class="px-4 py-2 text-right">
                      <button
                        type="button"
                        (click)="removePermission(p)"
                        class="text-red-600 hover:text-red-800 text-sm"
                      >
                        Remove
                      </button>
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
      </div>

      <div class="mt-8">
        <h2 class="text-lg font-semibold text-slate-800">Add user</h2>
        <p class="mt-1 text-sm text-slate-500">You will be recorded as the one who added this user.</p>
        <form (ngSubmit)="onAdd()" class="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label for="newEmail" class="block text-sm font-medium text-slate-700">Email</label>
            <input
              id="newEmail"
              type="email"
              [(ngModel)]="newEmail"
              name="newEmail"
              placeholder="user@example.com"
              class="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label for="newUsername" class="block text-sm font-medium text-slate-700">Username</label>
            <input
              id="newUsername"
              type="text"
              [(ngModel)]="newUsername"
              name="newUsername"
              placeholder="Display name"
              class="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label for="newPassword" class="block text-sm font-medium text-slate-700">Password</label>
            <input
              id="newPassword"
              type="password"
              [(ngModel)]="newPassword"
              name="newPassword"
              placeholder="Optional for form login"
              class="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label for="newGroup" class="block text-sm font-medium text-slate-700">Group</label>
            <select
              id="newGroup"
              [(ngModel)]="newGroup"
              name="newGroup"
              class="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
            >
              <option value="">No group</option>
              @for (g of groups; track g.id) {
                <option [value]="g.name">{{ g.name }}</option>
              }
            </select>
          </div>
          <div class="sm:col-span-2 lg:col-span-4 flex items-end">
            <button
              type="submit"
              [disabled]="!newEmail.trim() || adding"
              class="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
            >
              {{ adding ? 'Adding…' : 'Add user' }}
            </button>
          </div>
        </form>
        @if (addError) {
          <p class="mt-2 text-sm text-red-600">{{ addError }}</p>
        }
      </div>

      <div class="mt-8">
        <h2 class="text-lg font-semibold text-slate-800">Users</h2>
        <p class="mt-1 text-sm text-slate-500">Users can sign in via SSO, Google, or be added here. Toggle admin to allow access to this panel.</p>
        <div class="mt-2 overflow-x-auto rounded-lg border border-slate-200 bg-white shadow">
          <table class="min-w-full divide-y divide-slate-200">
            <thead class="bg-slate-50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Email</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Username</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Source</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">User ID (external)</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Admin</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Added by</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Last login</th>
                <th class="px-4 py-3 text-right text-xs font-medium uppercase text-slate-500">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-200 bg-white">
              @for (u of users; track u.email) {
                <tr>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-900">{{ u.email }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-600">{{ u.displayName || '—' }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-600">
                    <span [class]="u.source === 'SSO' ? 'rounded bg-sky-100 px-2 py-0.5 text-sky-800' : 'rounded bg-slate-100 px-2 py-0.5 text-slate-700'">
                      {{ u.source }}
                    </span>
                  </td>
                  <td class="max-w-[12rem] truncate px-4 py-3 text-sm text-slate-500" [title]="u.externalId || ''">{{ u.externalId || '—' }}</td>
                  <td class="whitespace-nowrap px-4 py-3">
                    <button
                      type="button"
                      (click)="toggleAdmin(u)"
                      [disabled]="toggling === u.email"
                      class="rounded px-2 py-1 text-sm font-medium"
                      [class.bg-amber-100]="u.admin"
                      [class.text-amber-800]="u.admin"
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
                      <button
                        type="button"
                        (click)="remove(u.email)"
                        [disabled]="removing === u.email"
                        class="text-red-600 hover:text-red-800 disabled:opacity-50"
                      >
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
          @if (users.length === 0 && !loading) {
            <p class="px-4 py-6 text-center text-slate-500">No users yet. Add one above or sign in with SSO/Google to populate.</p>
          }
        </div>
        @if (removeError) {
          <p class="mt-2 text-sm text-red-600">{{ removeError }}</p>
          <p class="mt-1 text-sm text-slate-500">
            If you see 403 Forbidden, the backend may not see your session.
            <a href="/api/auth/session-info" target="_blank" rel="noopener" class="text-indigo-600 hover:underline">Open session-info</a>
            in the same tab to check what the backend sees (principal and isAdmin).
          </p>
        }
        @if (loading) {
          <p class="mt-2 text-slate-500">Loading…</p>
        }
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

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getMe().subscribe({ next: (m) => (this.me = m) });
    this.loadGroups();
    this.loadGroupPermissions();
    this.loadUsers();
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
        this.groupError = err?.error?.message || err?.message || 'Failed to add group.';
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
        this.permError = err?.error?.message || err?.message || 'Failed to add permission.';
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
        this.addError = err?.error?.message || err?.message || 'Failed to add user.';
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
        this.removeError = err?.error?.message || err?.message || 'Failed to remove user.';
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
