import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, RecipientOption } from '../../core/services/api.service';

@Component({
  selector: 'app-send',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="mx-auto max-w-2xl px-4 py-12 sm:px-6 lg:px-8">
      <h1 class="text-2xl font-bold text-slate-900">Send a secret message</h1>
      <p class="mt-1 text-slate-600">Only the recipient can open it, once. Message expires after the configured time.</p>

      @if (success) {
        <div class="mt-6 rounded-lg bg-green-50 p-4 text-green-800">
          Message sent. The recipient will receive an email with the link.
        </div>
      }

      @if (error) {
        <div class="mt-6 rounded-lg bg-red-50 p-4 text-red-800">{{ error }}</div>
      }

      <form (ngSubmit)="onSubmit()" class="mt-6 space-y-4" #f="ngForm">
        <div>
          <label for="recipient" class="block text-sm font-medium text-slate-700">Recipient</label>
          <select
            id="recipient"
            name="recipientEmail"
            [(ngModel)]="selectedEmail"
            required
            class="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
          >
            <option value="" disabled>Select a user</option>
            @for (r of recipients; track r.email) {
              <option [value]="r.email">{{ r.email }} {{ r.source ? '(' + r.source + ')' : '' }}</option>
            }
          </select>
          @if (recipients.length === 0 && !loadingRecipients) {
            <p class="mt-1 text-sm text-slate-500">No users in the app yet. Add users in Admin or sign in with SSO/Google.</p>
          }
          @if (loadingRecipients) {
            <p class="mt-1 text-sm text-slate-500">Loading users…</p>
          }
        </div>
        <div>
          <label for="message" class="block text-sm font-medium text-slate-700">Message</label>
          <textarea
            id="message"
            name="message"
            [(ngModel)]="message"
            required
            rows="4"
            class="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-indigo-500 focus:ring-indigo-500"
          ></textarea>
        </div>
        <button
          type="submit"
          [disabled]="f.invalid || sending || !selectedEmail"
          class="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500 disabled:opacity-50"
        >
          {{ sending ? 'Sending…' : 'Send secret message' }}
        </button>
      </form>
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
        this.error = err?.error?.message || err?.message || 'Failed to send.';
        this.sending = false;
      },
    });
  }
}
