import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, getApiErrorMessage, RecipientOption } from '../../core/services/api.service';

@Component({
  selector: 'app-send',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="w-full max-w-2xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <div class="app-card p-6 sm:p-8">
        <h1 class="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">Send a secret message</h1>
        <p class="mt-2 text-slate-600">Only the recipient can open it, once. Message expires after the configured time.</p>

        @if (success) {
          <div class="mt-6 rounded-xl bg-emerald-50 border border-emerald-200/80 p-4 text-emerald-800 text-sm font-medium">
            Message sent. The recipient will receive an email with the link.
          </div>
        }

        @if (error) {
          <div class="mt-6 rounded-xl bg-red-50 border border-red-200/80 p-4 text-red-800 text-sm">{{ error }}</div>
        }

        <form (ngSubmit)="onSubmit()" class="mt-6 space-y-5" #f="ngForm">
          <div>
            <label for="recipient" class="block text-sm font-semibold text-slate-700">Recipient</label>
            <select
              id="recipient"
              name="recipientEmail"
              [(ngModel)]="selectedEmail"
              required
              class="app-input mt-2 py-3"
            >
              <option value="" disabled>Select a user</option>
              @for (r of recipients; track r.email) {
                <option [value]="r.email">{{ recipientLabel(r) }}</option>
              }
            </select>
            @if (recipients.length === 0 && !loadingRecipients) {
              <p class="mt-2 text-sm text-slate-500">No users in the app yet. Add users in Admin or sign in with SSO/Google.</p>
            }
            @if (loadingRecipients) {
              <p class="mt-2 text-sm text-slate-500">Loading users…</p>
            }
          </div>
          <div>
            <label for="message" class="block text-sm font-semibold text-slate-700">Message</label>
            <textarea
              id="message"
              name="message"
              [(ngModel)]="message"
              required
              rows="5"
              placeholder="Type your secret message…"
              class="app-input mt-2 py-3 resize-y min-h-[120px]"
            ></textarea>
          </div>
          <button
            type="submit"
            [disabled]="f.invalid || sending || !selectedEmail"
            class="app-btn-primary w-full py-3.5"
          >
            {{ sending ? 'Sending…' : 'Send secret message' }}
          </button>
        </form>
      </div>
    </div>
  `,
})
export class SendComponent implements OnInit {
  recipients: RecipientOption[] = [];
  loadingRecipients = true;
  selectedEmail = '';
  message = '';
  success = false;
  error: string | null = null;
  sending = false;

  constructor(private api: ApiService) {}

  recipientLabel(r: RecipientOption): string {
    if (r.displayName?.trim()) return `${r.displayName} (${r.email})`;
    return r.source ? `${r.email} (${r.source})` : r.email;
  }

  ngOnInit(): void {
    this.api.getRecipients().subscribe({
      next: (list) => {
        this.recipients = list;
        this.loadingRecipients = false;
      },
      error: () => {
        this.loadingRecipients = false;
      },
    });
  }

  onSubmit(): void {
    if (!this.selectedEmail) return;
    this.error = null;
    this.success = false;
    this.sending = true;
    this.api.sendSecret({ recipientEmail: this.selectedEmail, message: this.message }).subscribe({
      next: () => {
        this.success = true;
        this.message = '';
        this.sending = false;
      },
      error: (err) => {
        this.error = getApiErrorMessage(err, 'Failed to send.');
        this.sending = false;
      },
    });
  }
}
