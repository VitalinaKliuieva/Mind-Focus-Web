import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Goal, GoalStep } from '../core/api.service';
import { errorText } from '../core/dates';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h2 class="page-title">Goals</h2>

    <form class="card taped stack" (ngSubmit)="create()">
      <h3>New goal</h3>
      <div class="row">
        <label class="field" style="flex:1; min-width:200px"><span>Goal</span><input name="title" [(ngModel)]="draft.title" required maxlength="200"></label>
        <label class="field"><span>Target date (optional)</span><input name="target" type="date" [(ngModel)]="draft.targetDate"></label>
      </div>
      <label class="field"><span>Why it matters (optional)</span><input name="desc" [(ngModel)]="draft.description"></label>

      <div>
        <strong>Steps</strong> <small>Weight shows how big a step is. A step of weight 3 counts three times as much as weight 1.</small>
        @for (s of draft.steps; track $index) {
          <div class="row" style="margin-top:.4rem">
            <input style="flex:1; min-width:160px" [name]="'st' + $index" [(ngModel)]="s.title" placeholder="Step title" aria-label="Step title">
            <input style="width:80px" type="number" min="1" max="100" [name]="'w' + $index" [(ngModel)]="s.weight" aria-label="Weight">
            <button type="button" class="btn sm danger" (click)="draft.steps.splice($index, 1)">Remove</button>
          </div>
        }
        <button type="button" class="btn sm lav" style="margin-top:.6rem" (click)="addRow()">Add a step</button>
      </div>
      @if (error) { <div class="error">{{ error }}</div> }
      <button class="btn mint" type="submit">Save goal</button>
    </form>

    @if (goals.length === 0) { <div class="empty" style="margin-top:1.4rem">No goals yet. Write your first one above.</div> }

    <div class="grid cols-2" style="margin-top:1.6rem">
      @for (g of goals; track g.id) {
        <article class="card" [class.lav]="g.progress! < 100" [class.mint]="g.progress === 100">
          <div class="row between">
            <h3>{{ g.title }}</h3>
            <strong>{{ g.progress }}%</strong>
          </div>
          <div class="bar"><i [style.width.%]="g.progress"></i></div>
          @if (g.targetDate) { <p class="muted" style="margin-top:.4rem">Target: {{ g.targetDate }}</p> }
          @if (g.description) { <p>{{ g.description }}</p> }

          <ul class="items">
            @for (s of g.steps; track s.id) {
              <li [class.done]="s.done">
                <input type="checkbox" class="check" [checked]="s.done" (change)="toggleStep(g, s)" [attr.aria-label]="'Step done: ' + s.title">
                <span class="grow">{{ s.title }}</span>
                <span class="tag">weight {{ s.weight }}</span>
                <button class="btn sm danger" (click)="removeStep(g, s)">x</button>
              </li>
            }
          </ul>
          <div class="row" style="margin-top:.6rem">
            <input style="flex:1; min-width:120px" [(ngModel)]="newTitle[g.id!]" [name]="'ns' + g.id" placeholder="Add a step" aria-label="New step">
            <input style="width:70px" type="number" min="1" max="100" [(ngModel)]="newWeight[g.id!]" [name]="'nw' + g.id" aria-label="Weight">
            <button class="btn sm" (click)="addStep(g)">Add</button>
          </div>
          <div class="row" style="margin-top:.8rem"><button class="btn sm danger" (click)="remove(g)">Delete goal</button></div>
        </article>
      }
    </div>
  `,
})
export class GoalsComponent implements OnInit {
  private api = inject(ApiService);
  goals: Goal[] = [];
  draft: Goal = this.blank();
  newTitle: Record<number, string> = {};
  newWeight: Record<number, number> = {};
  error = '';

  ngOnInit(): void { this.load(); }

  blank(): Goal {
    return { title: '', description: '', targetDate: null, steps: [{ title: '', weight: 1, done: false }] };
  }

  load(): void {
    this.api.goals.list().subscribe((g) => {
      this.goals = g;
      g.forEach((x) => (this.newWeight[x.id!] ??= 1));
    });
  }

  addRow(): void { this.draft.steps.push({ title: '', weight: 1, done: false }); }

  create(): void {
    this.error = '';
    const body: Goal = { ...this.draft, steps: this.draft.steps.filter((s) => s.title.trim()) };
    this.api.goals.create(body).subscribe({
      next: () => { this.draft = this.blank(); this.load(); },
      error: (e) => (this.error = errorText(e)),
    });
  }

  private save(g: Goal): void { this.api.goals.update(g).subscribe(() => this.load()); }

  toggleStep(g: Goal, s: GoalStep): void { s.done = !s.done; this.save(g); }
  removeStep(g: Goal, s: GoalStep): void { g.steps = g.steps.filter((x) => x !== s); this.save(g); }

  addStep(g: Goal): void {
    const title = (this.newTitle[g.id!] ?? '').trim();
    if (!title) return;
    g.steps = [...g.steps, { title, weight: this.newWeight[g.id!] || 1, done: false }];
    this.newTitle[g.id!] = '';
    this.save(g);
  }

  remove(g: Goal): void {
    if (confirm(`Delete goal "${g.title}"?`)) this.api.goals.remove(g.id!).subscribe(() => this.load());
  }
}
