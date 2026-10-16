import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval, merge, of } from 'rxjs';
import { ApiService, AppNotification } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { PushService } from '../core/push.service';
import { RefreshService } from '../core/refresh.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  styles: [`
    .bell-panel { position: absolute; right: 0; top: 2.8rem; width: min(360px, 90vw); z-index: 5; max-height: 60vh; overflow: auto; }
    .bell-panel li { display: block; }
    .bell-panel .unseen { font-weight: 700; }
  `],
  template: `
    <div class="layout">
      <aside class="side">
        <h1 class="logo">MindFocus</h1>
        <nav>
          @for (l of links; track l.path) {
            <a [routerLink]="l.path" routerLinkActive="active">{{ l.label }}</a>
          }
        </nav>
        <div class="side-foot">
          <span>{{ auth.user?.displayName }}</span>
          <button class="btn ghost sm" (click)="auth.logout()">Log out</button>
        </div>
      </aside>

      <main class="main">
        <header class="topbar">
          <span class="date">{{ now | date: 'fullDate' }}</span>
          <div class="actions">
            @if (pushSupported) {
              <button class="btn sm lav" (click)="enablePush()">Turn on browser alerts</button>
            }
            <button class="btn sm" (click)="toggleBell()">
              Reminders @if (unread > 0) { <span class="badge">{{ unread }}</span> }
            </button>
            @if (open) {
              <div class="card bell-panel">
                <div class="row between">
                  <h3>Reminders</h3>
                  <button class="btn sm" (click)="markAll()">Mark all as read</button>
                </div>
                @if (items.length === 0) {
                  <p class="muted">No reminders yet. Add a deadline to a plan and it will show up here.</p>
                }
                <ul class="items">
                  @for (n of items; track n.id) {
                    <li [class.unseen]="!n.seen" (click)="seen(n)">
                      <div class="grow">{{ n.title }}<br><small>{{ n.message }}</small></div>
                    </li>
                  }
                </ul>
              </div>
            }
          </div>
        </header>
        @if (pushMessage) { <div class="empty">{{ pushMessage }}</div> }
        <router-outlet />
      </main>
    </div>
  `,
})
export class ShellComponent {
  auth = inject(AuthService);
  private api = inject(ApiService);
  private push = inject(PushService);
  private refresh = inject(RefreshService);

  now = new Date();
  open = false;
  items: AppNotification[] = [];
  pushMessage = '';
  pushSupported = this.push.supported();

  links = [
    { path: '/today', label: 'Today' },
    { path: '/notes', label: 'Notes' },
    { path: '/planner', label: 'Planner' },
    { path: '/todos', label: 'To-do' },
    { path: '/goals', label: 'Goals' },
    { path: '/habits', label: 'Habits' },
    { path: '/mood', label: 'Mood' },
    { path: '/calendar', label: 'Calendar' },
  ];

  constructor() {
    // load now, after every user action, and once a minute (the server creates reminders on its own)
    merge(of(0), this.refresh.changes$, interval(60_000))
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.load());
  }

  get unread(): number {
    return this.items.filter((n) => !n.seen).length;
  }

  private load(): void {
    this.api.notifications().subscribe({ next: (n) => (this.items = n), error: () => {} });
  }

  toggleBell(): void { this.open = !this.open; }

  seen(n: AppNotification): void {
    if (n.seen) return;
    n.seen = true;
    this.api.markSeen(n.id).subscribe();
  }

  markAll(): void {
    this.items.forEach((n) => (n.seen = true));
    this.api.markAllSeen().subscribe();
  }

  async enablePush(): Promise<void> {
    try {
      this.pushMessage = await this.push.enable();
    } catch (e) {
      this.pushMessage = 'Could not turn on browser alerts: ' + (e as Error).message;
    }
  }
}
