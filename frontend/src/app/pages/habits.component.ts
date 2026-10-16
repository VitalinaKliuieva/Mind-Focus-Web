import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService, Habit, HabitLog, HabitStat } from '../core/api.service';
import { addDays, errorText, today } from '../core/dates';

@Component({
  selector: 'app-habits',
  standalone: true,
  imports: [FormsModule],
  styles: [`
    .days { display: flex; gap: 6px; flex-wrap: wrap; margin: .5rem 0; }
    .day { width: 44px; text-align: center; font-size: .72rem; }
    .dot { display: block; width: 100%; height: 34px; margin-top: 2px; font: inherit; font-size: 1.1rem; cursor: pointer; border: 2px solid var(--ink); background: #fffaf0; }
    .dot.ok { background: var(--mint); }
    .dot.slip { background: #f2a7a7; }
  `],
  template: `
    <h2 class="page-title">Habits</h2>

    <form class="card taped row" (ngSubmit)="add()">
      <label class="field" style="flex:1; min-width:200px"><span>Habit</span><input name="name" [(ngModel)]="name" required maxlength="120" placeholder="e.g. Read 10 pages / No sugary drinks"></label>
      <label class="field"><span>Type</span>
        <select name="kind" [(ngModel)]="kind">
          <option value="BUILD">Build a habit</option>
          <option value="QUIT">Quit a bad habit</option>
        </select>
      </label>
      <button class="btn mint" type="submit">Add</button>
    </form>
    @if (error) { <div class="error">{{ error }}</div> }
    <p class="muted" style="margin-top:1rem">Click a day to cycle: done (✓), missed or slipped (✗), empty.</p>

    <div class="grid cols-2">
      @for (group of groups; track group.kind) {
        <section>
          <h3>{{ group.title }}</h3>
          @if (listFor(group.kind).length === 0) { <div class="empty">{{ group.empty }}</div> }
          @for (h of listFor(group.kind); track h.id) {
            <article class="card" [class.mint]="group.kind === 'BUILD'" [class.pink]="group.kind === 'QUIT'" style="margin-bottom:1.2rem">
              <div class="row between">
                <strong>{{ h.name }}</strong>
                <button class="btn sm danger" (click)="remove(h)">Delete</button>
              </div>
              <div class="days">
                @for (d of days; track d) {
                  <div class="day">{{ d.slice(5) }}
                    <button class="dot" [class.ok]="state(h, d) === 'ok'" [class.slip]="state(h, d) === 'slip'"
                            (click)="cycle(h, d)" [attr.aria-label]="h.name + ' on ' + d">{{ mark(h, d) }}</button>
                  </div>
                }
              </div>
              @if (stat(h); as st) {
                <p>
                  {{ group.kind === 'QUIT' ? 'Clean streak' : 'Streak' }}: <strong>{{ st.currentStreak }}</strong> days,
                  best {{ st.bestStreak }}, last 30 days {{ st.successRate30 }}%
                </p>
              }
            </article>
          }
        </section>
      }
    </div>
  `,
})
export class HabitsComponent implements OnInit {
  private api = inject(ApiService);
  habits: Habit[] = [];
  logs: HabitLog[] = [];
  stats: HabitStat[] = [];
  name = '';
  kind: 'BUILD' | 'QUIT' = 'BUILD';
  error = '';

  days: string[] = Array.from({ length: 7 }, (_, i) => addDays(today(), i - 6));

  groups = [
    { kind: 'BUILD' as const, title: 'Habits to build', empty: 'No habits yet. Add one you want to repeat.' },
    { kind: 'QUIT' as const, title: 'Habits to quit', empty: 'Nothing to quit yet. Add a habit you want to leave behind.' },
  ];

  ngOnInit(): void { this.load(); }

  load(): void {
    forkJoin({
      habits: this.api.habits.list(),
      logs: this.api.habitLogs(this.days[0], this.days[this.days.length - 1]),
      stats: this.api.habitStats(),
    }).subscribe((r) => { this.habits = r.habits; this.logs = r.logs; this.stats = r.stats; });
  }

  listFor(kind: 'BUILD' | 'QUIT'): Habit[] { return this.habits.filter((h) => h.kind === kind); }
  stat(h: Habit): HabitStat | undefined { return this.stats.find((s) => s.habitId === h.id); }
  private log(h: Habit, d: string): HabitLog | undefined { return this.logs.find((l) => l.habitId === h.id && l.logDate === d); }

  state(h: Habit, d: string): 'ok' | 'slip' | 'none' {
    const l = this.log(h, d);
    return !l ? 'none' : l.success ? 'ok' : 'slip';
  }
  mark(h: Habit, d: string): string { const s = this.state(h, d); return s === 'ok' ? '✓' : s === 'slip' ? '✗' : ''; }

  cycle(h: Habit, d: string): void {
    const s = this.state(h, d);
    const call = s === 'none' ? this.api.setHabitLog(h.id!, d, true)
      : s === 'ok' ? this.api.setHabitLog(h.id!, d, false)
      : this.api.clearHabitLog(h.id!, d);
    call.subscribe(() => this.load());
  }

  add(): void {
    this.error = '';
    this.api.habits.create({ name: this.name, kind: this.kind }).subscribe({
      next: () => { this.name = ''; this.load(); },
      error: (e) => (this.error = errorText(e)),
    });
  }

  remove(h: Habit): void {
    if (confirm(`Delete "${h.name}" and its history?`)) this.api.habits.remove(h.id!).subscribe(() => this.load());
  }
}
