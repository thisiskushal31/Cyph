import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="w-full max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
      @if (loading) {
        <p class="text-slate-600">Loading…</p>
      } @else if (locked) {
        <div class="rounded-lg border border-amber-200 bg-amber-50 p-6 text-amber-900">
          <p class="font-medium">This message is locked.</p>
          <p class="mt-2 text-sm">It was sent from a different group. Cross-group messages cannot be read.</p>
        </div>
      } @else if (error) {
        <div class="rounded-lg bg-red-50 p-4 text-red-800">
          {{ error }}
        </div>
        <p class="mt-4 text-sm text-slate-600">You may not be the recipient, or the message has expired or already been viewed.</p>
      } @else if (message) {
        <div class="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <p class="whitespace-pre-wrap text-slate-800">{{ message }}</p>
        </div>
      }
    </div>
  `,
})
export class ViewComponent implements OnInit {
  message: string | null = null;
  error: string | null = null;
  locked = false;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.error = 'Missing link.';
      this.loading = false;
      return;
    }
    this.api.viewSecret(token).subscribe({
      next: (res) => {
        if ('locked' in res && res.locked) {
          this.locked = true;
        } else if ('message' in res) {
          this.message = res.message;
        }
        this.loading = false;
      },
      error: (err) => {
        if (err?.status === 403 && err?.error?.locked) {
          this.locked = true;
        } else {
          this.error = 'Could not load message.';
        }
        this.loading = false;
      },
    });
  }
}
