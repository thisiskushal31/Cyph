import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, AuditLogPage, AuditLogEntry } from '../../core/services/api.service';

@Component({
  selector: 'app-log',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 class="text-2xl font-bold text-slate-900">Audit log</h1>
      <p class="mt-1 text-slate-600">No PII is stored. Only event type, time, message id, and group names.</p>

      <div class="mt-6 overflow-hidden rounded-lg border border-slate-200 bg-white shadow">
        <table class="min-w-full divide-y divide-slate-200">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Time (UTC)</th>
              <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Event</th>
              <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Message ID</th>
              <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Sender groups</th>
              <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Recipient groups</th>
              <th class="px-4 py-3 text-left text-xs font-medium uppercase text-slate-500">Same group</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200 bg-white">
            @for (e of entries; track e.occurredAt + (e.messageId ?? '') + e.eventType) {
              <tr>
                <td class="whitespace-nowrap px-4 py-3 text-sm text-slate-600">{{ formatDate(e.occurredAt) }}</td>
                <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-slate-900">{{ e.eventType }}</td>
                <td class="max-w-[8rem] truncate px-4 py-3 text-sm text-slate-500 font-mono" [title]="e.messageId ?? ''">{{ e.messageId ?? '—' }}</td>
                <td class="px-4 py-3 text-sm text-slate-600">{{ e.senderGroupNames ?? '—' }}</td>
                <td class="px-4 py-3 text-sm text-slate-600">{{ e.recipientGroupNames ?? '—' }}</td>
                <td class="px-4 py-3 text-sm">{{ e.sameGroup == null ? '—' : (e.sameGroup ? 'Yes' : 'No') }}</td>
              </tr>
            }
          </tbody>
        </table>
        @if (entries.length === 0 && !loading) {
          <p class="px-4 py-8 text-center text-slate-500">No audit entries yet.</p>
        }
      </div>
      @if (loading) {
        <p class="mt-2 text-slate-500">Loading…</p>
      }
      @if (page && (page.totalPages ?? 0) > 1) {
        <div class="mt-4 flex items-center justify-between">
          <p class="text-sm text-slate-600">
            Page {{ (page.number ?? 0) + 1 }} of {{ page.totalPages }} ({{ page.totalElements }} total)
          </p>
          <div class="flex gap-2">
            <button
              type="button"
              [disabled]="(page.number ?? 0) === 0"
              (click)="loadPage((page.number ?? 0) - 1)"
              class="rounded border border-slate-300 px-3 py-1 text-sm disabled:opacity-50"
            >
              Previous
            </button>
            <button
              type="button"
              [disabled]="(page.number ?? 0) >= (page.totalPages ?? 1) - 1"
              (click)="loadPage((page.number ?? 0) + 1)"
              class="rounded border border-slate-300 px-3 py-1 text-sm disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      }
    </div>
  `,
})
export class LogComponent implements OnInit {
  entries: AuditLogEntry[] = [];
  page: AuditLogPage | null = null;
  loading = false;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(pageIndex: number): void {
    this.loading = true;
    this.api.getAuditLog(pageIndex, 50).subscribe({
      next: (p) => {
        this.page = p;
        this.entries = p.content ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleString(undefined, { timeZone: 'UTC' });
    } catch {
      return iso;
    }
  }
}
