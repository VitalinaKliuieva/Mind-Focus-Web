import { Component, OnInit, inject } from '@angular/core';
import { ApiService, CalEvent } from '../core/api.service';
import { iso, today } from '../core/dates';

@Component({
  selector: 'app-calendar',
  standalone: true,
  styles: [`
    .head, .cells { display: grid; grid-template-columns: repeat(7, 1fr); }
    .head span { text-align: center; font-family: var(--font-head); padding: .3rem 0; }
    .cells { border-top: 2px solid var(--ink); border-left: 2px solid var(--ink); }
    .cell { min-height: 84px; border-right: 2px solid var(--ink); border-bottom: 2px solid var(--ink); padding: 3px 4px; background: #fffaf0; text-align: left; font: inherit; cursor: pointer; overflow: hidden; }
    .cell.blank { background: var(--paper-deep); cursor: default; }
    .cell.today { background: var(--butter); }
    .cell.sel { outline: 3px solid var(--ink); outline-offset: -3px; }
    .n { font-family: var(--font-head); }
    .ev { display: block; font-size: .7rem; line-height: 1.25; border: 1px solid var(--ink); margin-top: 2px; padding: 0 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .plan { background: var(--sky); } .todo { background: var(--pink); } .goal { background: var(--lavender); } .note { background: var(--butter); }
    .ev.done { text-decoration: line-through; opacity: .6; }
    @media (max-width: 800px) { .cell { min-height: 56px; } .ev { display: none; } }
  `],
  template: `
    <h2 class="page-title">Calendar</h2>
    <div class="row between" style="margin-bottom:1rem">
      <button class="btn sm" (click)="shift(-1)">Previous month</button>
      <h3 style="margin:0">{{ title }}</h3>
      <button class="btn sm" (click)="shift(1)">Next month</button>
    </div>

    <div class="head"><span>Mon</span><span>Tue</span><span>Wed</span><span>Thu</span><span>Fri</span><span>Sat</span><span>Sun</span></div>
    <div class="cells">
      @for (c of cells; track $index) {
        @if (c.date) {
          <button class="cell" [class.today]="c.date === todayStr" [class.sel]="c.date === selected" (click)="selected = c.date">
            <span class="n">{{ c.day }}</span>
            @for (e of eventsOn(c.date).slice(0, 3); track $index) {
              <span class="ev" [class]="'ev ' + e.type" [class.done]="e.done">{{ e.title }}</span>
            }
          </button>
        } @else {
          <div class="cell blank"></div>
        }
      }
    </div>

    <div class="card taped" style="margin-top:1.6rem">
      <h3>{{ selected }}</h3>
      @if (eventsOn(selected).length === 0) { <div class="empty">Nothing on this day.</div> }
      <ul class="items">
        @for (e of eventsOn(selected); track $index) {
          <li [class.done]="e.done">
            <span class="tag" [class]="'tag ' + e.type" [style.background]="color(e.type)">{{ e.type }}</span>
            <span class="grow">{{ e.title }}</span>
            @if (e.at) { <small>{{ e.at.slice(11, 16) }}</small> }
          </li>
        }
      </ul>
    </div>
  `,
})
export class CalendarComponent implements OnInit {
  private api = inject(ApiService);
  cursor = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  events: CalEvent[] = [];
  cells: { date: string | null; day: number }[] = [];
  selected = today();
  todayStr = today();

  get title(): string {
    return this.cursor.toLocaleDateString('en-GB', { month: 'long', year: 'numeric' });
  }

  ngOnInit(): void { this.load(); }

  shift(n: number): void {
    this.cursor = new Date(this.cursor.getFullYear(), this.cursor.getMonth() + n, 1);
    this.load();
  }

  load(): void {
    const y = this.cursor.getFullYear();
    const m = this.cursor.getMonth();
    const last = new Date(y, m + 1, 0).getDate();
    const offset = (new Date(y, m, 1).getDay() + 6) % 7; // week starts on Monday
    this.cells = [
      ...Array.from({ length: offset }, () => ({ date: null, day: 0 })),
      ...Array.from({ length: last }, (_, i) => ({ date: iso(new Date(y, m, i + 1)), day: i + 1 })),
    ];
    this.api.calendar(iso(new Date(y, m, 1)), iso(new Date(y, m, last))).subscribe((e) => (this.events = e));
  }

  eventsOn(date: string | null): CalEvent[] {
    return date ? this.events.filter((e) => e.date === date) : [];
  }

  color(type: string): string {
    return ({ plan: 'var(--sky)', todo: 'var(--pink)', goal: 'var(--lavender)', note: 'var(--butter)' } as Record<string, string>)[type];
  }
}
