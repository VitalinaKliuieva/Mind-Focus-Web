import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Mood } from '../core/api.service';
import { errorText, today } from '../core/dates';

@Component({
  selector: 'app-mood',
  standalone: true,
  imports: [FormsModule],
  styles: [`
    .faces { display: flex; gap: .6rem; flex-wrap: wrap; }
    .face { font-family: var(--font-head); font-size: 1.5rem; padding: .5rem .9rem; cursor: pointer; border: 2px solid var(--ink); background: #fffaf0; box-shadow: 3px 3px 0 var(--ink); }
    .face.on { box-shadow: none; transform: translate(3px, 3px); }
  `],
  template: `
    <h2 class="page-title">Mood</h2>
    <form class="card taped stack" (ngSubmit)="save()">
      <div class="row">
        <label class="field"><span>Day</span><input type="date" name="date" [(ngModel)]="date" (change)="pick()"></label>
      </div>
      <div class="faces" role="radiogroup" aria-label="Mood from 1 to 5">
        @for (f of faces; track f.score) {
          <button type="button" class="face" role="radio" [attr.aria-checked]="score === f.score" [class.on]="score === f.score"
                  [style.background]="score === f.score ? f.color : ''" (click)="score = f.score" [title]="f.label">{{ f.face }}</button>
        }
      </div>
      <label class="field"><span>One line about the day (optional)</span><input name="note" [(ngModel)]="note" maxlength="500"></label>
      @if (error) { <div class="error">{{ error }}</div> }
      <button class="btn mint" type="submit" [disabled]="!score">Save mood</button>
    </form>

    <h3 style="margin-top:1.6rem">Recent days</h3>
    @if (entries.length === 0) { <div class="empty">No mood saved yet. Pick a face above.</div> }
    <ul class="items card">
      @for (m of entries; track m.id) {
        <li>
          <span class="tag" [style.background]="faceOf(m.score).color">{{ faceOf(m.score).face }}</span>
          <span class="grow">{{ m.entryDate }} {{ m.note ? ' - ' + m.note : '' }}</span>
          <button class="btn sm danger" (click)="remove(m)">Delete</button>
        </li>
      }
    </ul>
  `,
})
export class MoodComponent implements OnInit {
  private api = inject(ApiService);
  faces = [
    { score: 1, face: ':(', label: 'rough', color: '#f2a7a7' },
    { score: 2, face: ':/', label: 'meh', color: 'var(--peach)' },
    { score: 3, face: ':|', label: 'okay', color: 'var(--butter)' },
    { score: 4, face: ':)', label: 'good', color: 'var(--mint)' },
    { score: 5, face: ':D', label: 'great', color: 'var(--sky)' },
  ];
  entries: Mood[] = [];
  date = today();
  score = 0;
  note = '';
  error = '';

  ngOnInit(): void { this.load(); }

  faceOf(score: number) { return this.faces.find((f) => f.score === score) ?? this.faces[2]; }

  load(): void {
    this.api.moods().subscribe((m) => { this.entries = m.slice(0, 30); this.pick(); });
  }

  /** Show the saved mood of the chosen day, if there is one. */
  pick(): void {
    const m = this.entries.find((e) => e.entryDate === this.date);
    this.score = m?.score ?? 0;
    this.note = m?.note ?? '';
  }

  save(): void {
    this.error = '';
    this.api.setMood(this.date, this.score, this.note).subscribe({
      next: () => this.load(),
      error: (e) => (this.error = errorText(e)),
    });
  }

  remove(m: Mood): void { this.api.removeMood(m.entryDate).subscribe(() => this.load()); }
}
