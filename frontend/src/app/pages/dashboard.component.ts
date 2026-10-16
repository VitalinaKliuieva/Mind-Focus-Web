import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { merge, of } from 'rxjs';
import { ApiService, Summary } from '../core/api.service';
import { RefreshService } from '../core/refresh.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink],
  styles: [`
    .chart { display: flex; align-items: flex-end; gap: 6px; height: 150px; padding-top: 8px; }
    .col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; font-size: .75rem; }
    .stackbar { width: 100%; max-width: 26px; display: flex; flex-direction: column-reverse; border: 2px solid var(--ink); background: #fffaf0; min-height: 4px; }
    .stackbar i { display: block; }
    .t { background: var(--pink); } .p { background: var(--sky); } .h { background: var(--mint); }
    .legend { display: flex; gap: 1rem; font-size: .85rem; margin-top: .6rem; flex-wrap: wrap; }
    .legend b { display: inline-block; width: 12px; height: 12px; border: 1.5px solid var(--ink); margin-right: 4px; vertical-align: -1px; }
    .moods { display: flex; gap: 6px; margin-top: .4rem; }
    .moods span { flex: 1; text-align: center; font-size: .75rem; }
  `],
  template: `
    <h2 class="page-title">Today</h2>
    @if (s) {
      <section class="grid cols-4">
        <div class="card pink taped">
          <h3>To-dos</h3>
          <p class="big">{{ s.totals.todosDone }}<small>/{{ s.totals.todosDone + s.totals.todosOpen }}</small></p>
          <div class="bar pink"><i [style.width.%]="pct(s.totals.todosDone, s.totals.todosDone + s.totals.todosOpen)"></i></div>
        </div>
        <div class="card sky taped">
          <h3>Plans</h3>
          <p class="big">{{ s.totals.plansDone }}<small>/{{ s.totals.plansDone + s.totals.plansOpen }}</small></p>
          <div class="bar"><i [style.width.%]="pct(s.totals.plansDone, s.totals.plansDone + s.totals.plansOpen)"></i></div>
          @if (s.totals.plansOverdue > 0) { <p><span class="tag late">{{ s.totals.plansOverdue }} overdue</span></p> }
        </div>
        <div class="card lav taped">
          <h3>Goals</h3>
          <p class="big">{{ s.totals.goalsAverageProgress }}<small>%</small></p>
          <div class="bar lav"><i [style.width.%]="s.totals.goalsAverageProgress"></i></div>
        </div>
        <div class="card mint taped">
          <h3>Habits</h3>
          <p class="big">{{ s.totals.habitSuccessRate }}<small>%</small></p>
          <div class="bar"><i [style.width.%]="s.totals.habitSuccessRate"></i></div>
          <p class="muted">Mood average: {{ s.totals.moodAverage ?? 'not logged' }}</p>
        </div>
      </section>

      <section class="grid cols-2" style="margin-top:1.6rem">
        <div class="card">
          <h3>What your diary says</h3>
          <ul class="typed">
            @for (i of s.insights; track i) { <li>{{ i }}</li> }
          </ul>
        </div>

        <div class="card">
          <h3>Next deadlines</h3>
          @if (s.upcoming.length === 0) {
            <div class="empty">No deadlines ahead. <a routerLink="/planner">Plan something</a>.</div>
          }
          <ul class="items">
            @for (u of s.upcoming; track u.id) {
              <li><span class="grow">{{ u.title }}</span><span class="tag">{{ u.deadline | date: 'EEE d MMM, HH:mm' }}</span></li>
            }
          </ul>
        </div>
      </section>

      <section class="card" style="margin-top:1.6rem">
        <h3>Last {{ s.series.length }} days: things finished</h3>
        <div class="chart">
          @for (d of s.series; track d.date) {
            <div class="col" [title]="d.date">
              <div class="stackbar" [style.height.px]="barHeight(d)">
                <i class="t" [style.flex-grow]="d.todosCompleted"></i>
                <i class="p" [style.flex-grow]="d.plansCompleted"></i>
                <i class="h" [style.flex-grow]="d.habitsDone"></i>
              </div>
              <span>{{ d.date.slice(8) }}</span>
            </div>
          }
        </div>
        <div class="moods" title="Mood per day, 1 to 5">
          @for (d of s.series; track d.date) { <span>{{ d.mood ?? '-' }}</span> }
        </div>
        <div class="legend">
          <span><b class="t"></b>to-dos</span><span><b class="p"></b>plans</span><span><b class="h"></b>habits</span><span>numbers below = mood</span>
        </div>
      </section>
    } @else {
      <div class="empty">Loading your diary...</div>
    }
  `,
})
export class DashboardComponent {
  private api = inject(ApiService);
  private refresh = inject(RefreshService);
  s: Summary | null = null;

  constructor() {
    // reload after every change anywhere in the app
    merge(of(0), this.refresh.changes$).pipe(takeUntilDestroyed()).subscribe(() => {
      this.api.summary(14).subscribe((r) => (this.s = r));
    });
  }

  pct(a: number, b: number): number {
    return b ? Math.round((100 * a) / b) : 0;
  }

  barHeight(d: { todosCompleted: number; plansCompleted: number; habitsDone: number }): number {
    const n = d.todosCompleted + d.plansCompleted + d.habitsDone;
    return n === 0 ? 4 : Math.min(120, 14 + n * 18);
  }
}
