import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Note } from '../core/api.service';
import { errorText, today } from '../core/dates';

@Component({
  selector: 'app-notes',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h2 class="page-title">Notes</h2>
    <form class="card taped stack" (ngSubmit)="save()">
      <h3>{{ form.id ? 'Edit note' : 'New note' }}</h3>
      <div class="row">
        <label class="field" style="flex:1"><span>Title</span><input name="title" [(ngModel)]="form.title" required maxlength="200"></label>
        <label class="field"><span>Date</span><input name="date" type="date" [(ngModel)]="form.noteDate"></label>
      </div>
      <label class="field"><span>Write here</span><textarea name="content" [(ngModel)]="form.content"></textarea></label>
      @if (error) { <div class="error">{{ error }}</div> }
      <div class="row">
        <button class="btn mint" type="submit">Save note</button>
        @if (form.id) { <button class="btn ghost" type="button" (click)="reset()">Cancel</button> }
      </div>
    </form>

    <div class="row" style="margin:1.6rem 0 .8rem">
      <label class="field" style="flex:1"><span>Search notes</span><input [(ngModel)]="query" name="q" placeholder="type a word"></label>
    </div>

    @if (visible.length === 0) {
      <div class="empty">{{ notes.length ? 'No note matches that word.' : 'No notes yet. Write your first one above.' }}</div>
    }
    <div class="grid cols-2">
      @for (n of visible; track n.id) {
        <article class="card" [class.butter]="$index % 3 === 0" [class.pink]="$index % 3 === 1" [class.sky]="$index % 3 === 2">
          <small>{{ n.noteDate }}</small>
          <h3>{{ n.title }}</h3>
          <p style="white-space: pre-wrap">{{ n.content }}</p>
          <div class="row">
            <button class="btn sm" (click)="edit(n)">Edit</button>
            <button class="btn sm danger" (click)="remove(n)">Delete</button>
          </div>
        </article>
      }
    </div>
  `,
})
export class NotesComponent implements OnInit {
  private api = inject(ApiService);
  notes: Note[] = [];
  form: Note = this.blank();
  query = '';
  error = '';

  ngOnInit(): void { this.load(); }

  blank(): Note { return { title: '', content: '', noteDate: today() }; }

  get visible(): Note[] {
    const q = this.query.trim().toLowerCase();
    return q ? this.notes.filter((n) => (n.title + ' ' + n.content).toLowerCase().includes(q)) : this.notes;
  }

  load(): void { this.api.notes.list().subscribe((n) => (this.notes = n)); }

  save(): void {
    this.error = '';
    const call = this.form.id ? this.api.notes.update(this.form) : this.api.notes.create(this.form);
    call.subscribe({ next: () => { this.reset(); this.load(); }, error: (e) => (this.error = errorText(e)) });
  }

  edit(n: Note): void { this.form = { ...n }; window.scrollTo({ top: 0, behavior: 'smooth' }); }
  reset(): void { this.form = this.blank(); }

  remove(n: Note): void {
    if (confirm(`Delete "${n.title}"?`)) this.api.notes.remove(n.id!).subscribe(() => this.load());
  }
}
