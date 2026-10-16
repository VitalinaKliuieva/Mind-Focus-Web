import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Todo } from '../core/api.service';
import { errorText, today } from '../core/dates';

@Component({
  selector: 'app-todos',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h2 class="page-title">To-do list</h2>
    <form class="card taped row" (ngSubmit)="add()">
      <label class="field" style="flex:1; min-width:200px"><span>What needs doing?</span><input name="title" [(ngModel)]="title" required maxlength="200"></label>
      <label class="field"><span>Due (optional)</span><input name="due" type="date" [(ngModel)]="due"></label>
      <button class="btn mint" type="submit">Add</button>
    </form>
    @if (error) { <div class="error">{{ error }}</div> }

    <div class="tabs" style="margin-top:1.4rem">
      <button [class.on]="filter === 'open'" (click)="filter = 'open'">Open ({{ count(false) }})</button>
      <button [class.on]="filter === 'done'" (click)="filter = 'done'">Done ({{ count(true) }})</button>
      <button [class.on]="filter === 'all'" (click)="filter = 'all'">All</button>
    </div>

    @if (visible.length === 0) { <div class="empty">Nothing here. {{ filter === 'open' ? 'Add a task above.' : '' }}</div> }
    <ul class="items card">
      @for (t of visible; track t.id) {
        <li [class.done]="t.done">
          <input type="checkbox" class="check" [checked]="t.done" (change)="toggle(t)" [attr.aria-label]="'Done: ' + t.title">
          <span class="grow">{{ t.title }}</span>
          @if (t.dueDate) { <span class="tag" [class.late]="!t.done && t.dueDate < todayStr">{{ t.dueDate }}</span> }
          <button class="btn sm danger" (click)="remove(t)">Delete</button>
        </li>
      }
    </ul>
  `,
})
export class TodosComponent implements OnInit {
  private api = inject(ApiService);
  todos: Todo[] = [];
  title = '';
  due = '';
  filter: 'open' | 'done' | 'all' = 'open';
  error = '';
  todayStr = today();

  ngOnInit(): void { this.load(); }

  load(): void { this.api.todos.list().subscribe((t) => (this.todos = t)); }

  count(done: boolean): number { return this.todos.filter((t) => t.done === done).length; }

  get visible(): Todo[] {
    return this.todos.filter((t) => this.filter === 'all' || (this.filter === 'done') === t.done);
  }

  add(): void {
    this.error = '';
    this.api.todos.create({ title: this.title, done: false, dueDate: this.due || null }).subscribe({
      next: () => { this.title = ''; this.due = ''; this.load(); },
      error: (e) => (this.error = errorText(e)),
    });
  }

  toggle(t: Todo): void { this.api.todos.update({ ...t, done: !t.done }).subscribe(() => this.load()); }
  remove(t: Todo): void { this.api.todos.remove(t.id!).subscribe(() => this.load()); }
}
