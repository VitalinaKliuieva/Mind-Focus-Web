import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, Plan } from '../core/api.service';
import { addDays, errorText, today } from '../core/dates';

@Component({
  selector: 'app-planner',
  standalone: true,
  imports: [FormsModule],
  template: `
    <h2 class="page-title">Planner</h2>
    <div class="tabs">
      <button [class.on]="tab === 'DAY'" (click)="setTab('DAY')">Day plan</button>
      <button [class.on]="tab === 'MONTH'" (click)="setTab('MONTH')">Month plan</button>
    </div>

    <div class="row" style="margin-bottom:1rem">
      @if (tab === 'DAY') {
        <button class="btn sm" (click)="day = shiftDay(-1)">Previous day</button>
        <input type="date" [(ngModel)]="day" name="day" aria-label="Day">
        <button class="btn sm" (click)="day = shiftDay(1)">Next day</button>
      } @else {
        <input type="month" [(ngModel)]="month" name="month" aria-label="Month">
      }
    </div>

    <form class="card taped stack" (ngSubmit)="save()">
      <h3>{{ form.id ? 'Edit item' : tab === 'DAY' ? 'Add to this day' : 'Add to this month' }}</h3>
      <label class="field"><span>Title</span><input name="title" [(ngModel)]="form.title" required maxlength="200"></label>
      <label class="field"><span>Details (optional)</span><textarea name="description" [(ngModel)]="form.description" style="min-height:70px"></textarea></label>
      <div class="row">
        <label class="field"><span>Deadline (optional)</span><input name="deadline" type="datetime-local" [(ngModel)]="form.deadline"></label>
        <label class="field"><span>Remind me</span>
          <select name="remind" [(ngModel)]="form.reminderMinutesBefore">
            <option [ngValue]="0">at the deadline</option>
            <option [ngValue]="15">15 minutes before</option>
            <option [ngValue]="60">1 hour before</option>
            <option [ngValue]="180">3 hours before</option>
            <option [ngValue]="1440">1 day before</option>
            <option [ngValue]="4320">3 days before</option>
          </select>
        </label>
      </div>
      @if (error) { <div class="error">{{ error }}</div> }
      <div class="row">
        <button class="btn mint" type="submit">Save</button>
        @if (form.id) { <button type="button" class="btn ghost" (click)="reset()">Cancel</button> }
      </div>
    </form>

    <h3 style="margin-top:1.6rem">{{ tab === 'DAY' ? 'Plan for ' + day : 'Plan for ' + month }}</h3>
    @if (visible.length === 0) { <div class="empty">Nothing planned yet. Add the first item above.</div> }
    <ul class="items card">
      @for (p of visible; track p.id) {
        <li [class.done]="p.done">
          <input type="checkbox" class="check" [checked]="p.done" (change)="toggle(p)" [attr.aria-label]="'Done: ' + p.title">
          <div class="grow">
            {{ p.title }}
            @if (p.description) { <br><small>{{ p.description }}</small> }
          </div>
          @if (p.deadline) {
            <span class="tag" [class.late]="overdue(p)">{{ p.deadline.replace('T', ' ').slice(0, 16) }}</span>
          }
          <button class="btn sm" (click)="edit(p)">Edit</button>
          <button class="btn sm danger" (click)="remove(p)">Delete</button>
        </li>
      }
    </ul>
  `,
})
export class PlannerComponent implements OnInit {
  private api = inject(ApiService);
  tab: 'DAY' | 'MONTH' = 'DAY';
  day = today();
  month = today().slice(0, 7);
  all: Plan[] = [];
  form!: Plan;
  error = '';

  ngOnInit(): void { this.reset(); this.load(); }

  setTab(t: 'DAY' | 'MONTH'): void { this.tab = t; this.reset(); }
  shiftDay(n: number): string { return addDays(this.day, n); }

  load(): void { this.api.plans.list().subscribe((p) => (this.all = p)); }

  get visible(): Plan[] {
    return this.all
      .filter((p) => p.type === this.tab && (this.tab === 'DAY' ? p.planDate === this.day : p.planDate.slice(0, 7) === this.month))
      .sort((a, b) => (a.deadline ?? '9999').localeCompare(b.deadline ?? '9999'));
  }

  reset(): void {
    this.form = { title: '', description: '', type: this.tab, planDate: this.day, deadline: null, done: false, reminderMinutesBefore: 60 };
  }

  save(): void {
    this.error = '';
    const body: Plan = {
      ...this.form,
      type: this.tab,
      planDate: this.form.id ? this.form.planDate : this.tab === 'DAY' ? this.day : this.month + '-01',
      deadline: this.form.deadline || null,
    };
    const call = body.id ? this.api.plans.update(body) : this.api.plans.create(body);
    call.subscribe({ next: () => { this.reset(); this.load(); }, error: (e) => (this.error = errorText(e)) });
  }

  edit(p: Plan): void {
    this.form = { ...p, deadline: p.deadline ? p.deadline.slice(0, 16) : null };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  toggle(p: Plan): void { this.api.plans.update({ ...p, done: !p.done }).subscribe(() => this.load()); }
  remove(p: Plan): void { this.api.plans.remove(p.id!).subscribe(() => this.load()); }
  overdue(p: Plan): boolean { return !p.done && !!p.deadline && new Date(p.deadline) < new Date(); }
}
