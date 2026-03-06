import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ApiService,
  getApiErrorMessage,
  StoredCredentialDto,
  CreateCredentialRequest,
  UpdateCredentialRequest,
} from '../../core/services/api.service';

@Component({
  selector: 'app-credentials',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="w-full max-w-4xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold tracking-tight text-slate-100 sm:text-3xl">My credentials</h1>
        <p class="mt-2 text-slate-400">
          Your personal credentials only (shared credentials from admin are not shown here). Edit or add below; they sync with the Cyph extension. In the extension you will see both shared and personal credentials for the same site.
        </p>
      </div>

      <div class="app-card p-6">
        <h2 class="text-lg font-semibold text-slate-800">Add credential</h2>
        <p class="mt-1 text-sm text-slate-500">Label, URL, username, and secret. Stored encrypted; only you can see the secret.</p>
        <form (ngSubmit)="onAdd()" class="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <label for="newLabel" class="block text-sm font-semibold text-slate-700">Label</label>
            <input id="newLabel" type="text" [(ngModel)]="newLabel" name="newLabel" placeholder="e.g. Grafana - Personal" class="app-input mt-2 w-full" required />
          </div>
          <div>
            <label for="newUrl" class="block text-sm font-semibold text-slate-700">URL</label>
            <input id="newUrl" type="url" [(ngModel)]="newUrl" name="newUrl" placeholder="https://monitor.example.com" class="app-input mt-2 w-full" />
          </div>
          <div>
            <label for="newUsername" class="block text-sm font-semibold text-slate-700">Username</label>
            <input id="newUsername" type="text" [(ngModel)]="newUsername" name="newUsername" placeholder="Login name for this site" class="app-input mt-2 w-full" />
          </div>
          <div>
            <label for="newSecret" class="block text-sm font-semibold text-slate-700">Secret / password</label>
            <input id="newSecret" type="password" [(ngModel)]="newSecret" name="newSecret" placeholder="••••••••" class="app-input mt-2 w-full" />
          </div>
          <div class="sm:col-span-2">
            <button type="submit" [disabled]="!newLabel.trim() || adding" class="app-btn-primary">
              {{ adding ? '…' : 'Add credential' }}
            </button>
          </div>
        </form>
        @if (addError) {
          <p class="mt-2 text-sm text-red-600">{{ addError }}</p>
        }
      </div>

      <div class="mt-6 app-card overflow-hidden">
        <div class="p-6 pb-4">
          <h2 class="text-lg font-semibold text-slate-800">Your credentials</h2>
          <p class="mt-1 text-sm text-slate-500">Edit or delete below. Use the Chrome extension to reveal and autofill.</p>
        </div>
        @if (loading) {
          <p class="px-6 pb-6 text-slate-500 text-sm">Loading…</p>
        } @else if (credentials.length === 0) {
          <p class="px-6 pb-6 text-slate-500 text-sm">No credentials yet. Add one above.</p>
        } @else {
          <div class="overflow-x-auto">
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
                @for (c of credentials; track c.id) {
                  <tr class="hover:bg-slate-50/80 transition">
                    <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900">{{ c.label }}</td>
                    <td class="max-w-[14rem] truncate px-4 py-3 text-sm text-slate-600" [title]="c.url || ''">{{ c.url || '—' }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-600">{{ c.usernameMeta || '—' }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-right">
                      <button type="button" (click)="startEdit(c)" class="rounded-lg px-2.5 py-1.5 text-sm font-medium text-indigo-600 hover:bg-indigo-50 mr-2">Edit</button>
                      <button type="button" (click)="deleteCred(c)" [disabled]="deleting === c.id" class="rounded-lg px-2.5 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50">
                        {{ deleting === c.id ? '…' : 'Delete' }}
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
        @if (deleteError) {
          <p class="px-6 pb-6 text-sm text-red-600">{{ deleteError }}</p>
        }
      </div>

      @if (editing) {
        <div class="fixed inset-0 z-40 flex items-center justify-center bg-black/50 p-4" (click)="cancelEdit()">
          <div class="app-card p-6 max-w-md w-full shadow-xl" (click)="$event.stopPropagation()">
            <h3 class="text-lg font-semibold text-slate-800">Edit credential</h3>
            <form (ngSubmit)="saveEdit()" class="mt-4 space-y-4">
              <div>
                <label class="block text-sm font-semibold text-slate-700">Label</label>
                <input type="text" [(ngModel)]="editLabel" name="editLabel" class="app-input mt-2 w-full" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-700">URL</label>
                <input type="url" [(ngModel)]="editUrl" name="editUrl" class="app-input mt-2 w-full" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-700">Username</label>
                <input type="text" [(ngModel)]="editUsername" name="editUsername" class="app-input mt-2 w-full" />
              </div>
              <div>
                <label class="block text-sm font-semibold text-slate-700">Secret (leave blank to keep current)</label>
                <input type="password" [(ngModel)]="editSecret" name="editSecret" placeholder="••••••••" class="app-input mt-2 w-full" />
              </div>
              <div class="flex gap-2 justify-end">
                <button type="button" (click)="cancelEdit()" class="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Cancel</button>
                <button type="submit" [disabled]="saving" class="app-btn-primary">{{ saving ? '…' : 'Save' }}</button>
              </div>
            </form>
            @if (editError) {
              <p class="mt-2 text-sm text-red-600">{{ editError }}</p>
            }
          </div>
        </div>
      }
    </div>
  `,
})
export class CredentialsComponent implements OnInit {
  credentials: StoredCredentialDto[] = [];
  loading = false;
  adding = false;
  addError: string | null = null;
  deleting: number | null = null;
  deleteError: string | null = null;

  newLabel = '';
  newUrl = '';
  newUsername = '';
  newSecret = '';

  editing: StoredCredentialDto | null = null;
  editLabel = '';
  editUrl = '';
  editUsername = '';
  editSecret = '';
  saving = false;
  editError: string | null = null;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api.listCredentials().subscribe({
      next: (list) => {
        this.credentials = list;
        this.loading = false;
      },
      error: () => {
        this.credentials = [];
        this.loading = false;
      },
    });
  }

  onAdd(): void {
    if (!this.newLabel.trim()) return;
    this.addError = null;
    this.adding = true;
    const body: CreateCredentialRequest = {
      label: this.newLabel.trim(),
      url: this.newUrl.trim() || undefined,
      usernameMeta: this.newUsername.trim() || undefined,
      secret: this.newSecret,
    };
    this.api.createCredential(body).subscribe({
      next: () => {
        this.newLabel = '';
        this.newUrl = '';
        this.newUsername = '';
        this.newSecret = '';
        this.adding = false;
        this.load();
      },
      error: (err) => {
        this.addError = getApiErrorMessage(err, 'Failed to add credential.');
        this.adding = false;
      },
    });
  }

  startEdit(c: StoredCredentialDto): void {
    this.editing = c;
    this.editLabel = c.label;
    this.editUrl = c.url ?? '';
    this.editUsername = c.usernameMeta ?? '';
    this.editSecret = '';
    this.editError = null;
  }

  cancelEdit(): void {
    this.editing = null;
  }

  saveEdit(): void {
    if (!this.editing) return;
    this.editError = null;
    this.saving = true;
    const body: UpdateCredentialRequest = {
      label: this.editLabel.trim(),
      url: this.editUrl.trim() || undefined,
      usernameMeta: this.editUsername.trim() || undefined,
    };
    if (this.editSecret) body.secret = this.editSecret;
    this.api.updateCredential(this.editing.id, body).subscribe({
      next: () => {
        this.saving = false;
        this.editing = null;
        this.load();
      },
      error: (err) => {
        this.editError = getApiErrorMessage(err, 'Failed to update.');
        this.saving = false;
      },
    });
  }

  deleteCred(c: StoredCredentialDto): void {
    this.deleteError = null;
    this.deleting = c.id;
    this.api.deleteCredential(c.id).subscribe({
      next: () => {
        this.deleting = null;
        this.load();
      },
      error: (err) => {
        this.deleting = null;
        this.deleteError = getApiErrorMessage(err, 'Failed to delete.');
      },
    });
  }
}
