import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

/** Scroll threshold (px): above this, nav becomes a full-width bar instead of island */
const SCROLL_THRESHOLD_PX = 24;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'Cyph';
  navOpen = false;
  /** True when user has scrolled past threshold – show bar; else show island */
  isScrolled = false;
  private scrollListener: (() => void) | null = null;

  ngOnInit(): void {
    this.updateScrolledState();
    this.scrollListener = () => this.updateScrolledState();
    window.addEventListener('scroll', this.scrollListener, { passive: true });
  }

  ngOnDestroy(): void {
    if (this.scrollListener) {
      window.removeEventListener('scroll', this.scrollListener);
    }
  }

  private updateScrolledState(): void {
    const now = window.scrollY > SCROLL_THRESHOLD_PX;
    if (now !== this.isScrolled) {
      this.isScrolled = now;
    }
  }
}
