import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ApiService, AuditLogPage, AuditLogEntry } from '../../core/services/api.service';

@Component({
  selector: 'app-log',
  standalone: true,
  imports: [CommonModule],
  styles: [`
    .log-entry pre { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
    .log-json-key { color: #6366f1; font-weight: 600; }
    .log-json-string { color: #0d9488; }
    .log-json-number { color: #ea580c; }
    .log-json-literal { color: #7c3aed; }
  `],
  template: `
    <div class="w-full px-4 py-10 sm:px-6 lg:px-8 xl:px-10">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold tracking-tight text-slate-900">Audit log</h1>
          <p class="mt-1.5 text-sm text-slate-500">Pretty JSON per event. Newest first.</p>
        </div>
        <div class="flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1.5 text-sm text-slate-600">
          <span class="relative flex h-2 w-2" aria-hidden="true">
            <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>
            <span class="relative inline-flex h-2 w-2 rounded-full bg-emerald-500"></span>
          </span>
          <span>Live · refreshes every 10s</span>
        </div>
      </div>

      <div class="mt-8 space-y-4">
        @if (loading) {
          <div class="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
            <div class="inline-flex h-8 w-8 animate-spin rounded-full border-2 border-slate-200 border-t-indigo-500"></div>
            <p class="mt-3 text-sm text-slate-500">Loading log…</p>
          </div>
        } @else if (entries.length === 0) {
          <div class="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
              </svg>
            </div>
            <p class="mt-3 text-sm font-medium text-slate-700">No entries yet</p>
            <p class="mt-1 text-sm text-slate-500">Events will appear here after logins and message activity.</p>
          </div>
        } @else {
          <div class="overflow-auto max-h-[65vh] min-h-[12rem] space-y-4 pr-1">
            @for (e of entries; track e.occurredAt + (e.messageId ?? '') + e.eventType) {
              <article class="log-entry rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300 hover:shadow">
                <div class="mb-3 flex flex-wrap items-center gap-2">
                  <span class="inline-flex items-center rounded-lg bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700">{{ e.occurredAt }}</span>
                  <span class="rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">{{ e.eventType }}</span>
                </div>
                <div class="rounded-xl bg-slate-50/80 p-4 ring-1 ring-slate-200/60">
                  <pre class="m-0 min-w-0 overflow-x-auto whitespace-pre-wrap break-words text-[13px] leading-[1.6] text-slate-700" [innerHTML]="toPrettyJsonHtml(e)"></pre>
                </div>
              </article>
            }
          </div>
        }
      </div>

      @if (page && page.totalPages > 1) {
        <div class="mt-8 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
          <p class="text-sm text-slate-600">
            <span class="font-medium text-slate-800">Page {{ (page.number ?? 0) + 1 }}</span>
            <span class="mx-1.5 text-slate-300">/</span>
            <span>{{ page.totalPages }}</span>
            <span class="ml-2 text-slate-400">·</span>
            <span class="ml-2">{{ page.totalElements }} entries</span>
          </p>
          <div class="flex gap-2">
            <button
              type="button"
              [disabled]="(page.number ?? 0) === 0"
              (click)="loadPage((page.number ?? 0) - 1)"
              class="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 hover:border-slate-300 disabled:pointer-events-none disabled:opacity-50"
            >
              Previous
            </button>
            <button
              type="button"
              [disabled]="(page.number ?? 0) >= page.totalPages - 1"
              (click)="loadPage((page.number ?? 0) + 1)"
              class="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 hover:border-slate-300 disabled:pointer-events-none disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      }
    </div>
  `,
})
export class LogComponent implements OnInit, OnDestroy {
  entries: AuditLogEntry[] = [];
  page: AuditLogPage | null = null;
  loading = false;
  private currentPageIndex = 0;
  private refreshInterval: ReturnType<typeof setInterval> | null = null;

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.loadPage(0);
    this.refreshInterval = setInterval(() => this.refresh(), 10_000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  loadPage(pageIndex: number): void {
    this.currentPageIndex = pageIndex;
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

  /** Silent refresh for live reload (no loading spinner). */
  private refresh(): void {
    this.api.getAuditLog(this.currentPageIndex, 50).subscribe({
      next: (p) => {
        this.page = p;
        this.entries = p.content ?? [];
      },
    });
  }

  private buildEntryObj(e: AuditLogEntry): Record<string, unknown> {
    const obj: Record<string, unknown> = {
      eventType: e.eventType,
      occurredAt: e.occurredAt,
    };
    if (e.actorIdentifier != null && e.actorIdentifier !== '') obj['actorIdentifier'] = e.actorIdentifier;
    if (e.messageId != null && e.messageId !== '') obj['messageId'] = e.messageId;
    if (e.senderGroupNames != null && e.senderGroupNames !== '') obj['senderGroupNames'] = e.senderGroupNames;
    if (e.recipientGroupNames != null && e.recipientGroupNames !== '') obj['recipientGroupNames'] = e.recipientGroupNames;
    if (e.sameGroup != null) obj['sameGroup'] = e.sameGroup;
    return obj;
  }

  /** Pretty-printed JSON with simple syntax highlighting. */
  toPrettyJsonHtml(e: AuditLogEntry): SafeHtml {
    const pretty = JSON.stringify(this.buildEntryObj(e), null, 2);
    const escaped = pretty
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
    const highlighted = escaped
      .replace(/"([^"]+)"\s*:/g, '<span class="log-json-key">"$1"</span>:')
      .replace(/: "([^"]*)"/g, ': <span class="log-json-string">"$1"</span>')
      .replace(/: (true|false|null)\b/g, ': <span class="log-json-literal">$1</span>')
      .replace(/: (\d+\.?\d*)/g, ': <span class="log-json-number">$1</span>');
    return this.sanitizer.bypassSecurityTrustHtml(highlighted);
  }
}
