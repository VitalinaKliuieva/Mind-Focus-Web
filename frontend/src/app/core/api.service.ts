import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { RefreshService } from './refresh.service';

// ---------- models ----------
export interface Note { id?: number; title: string; content: string; noteDate: string; }
export interface Plan {
  id?: number; title: string; description: string; type: 'DAY' | 'MONTH'; planDate: string;
  deadline: string | null; done: boolean; reminderMinutesBefore: number; reminderSent?: boolean;
}
export interface Todo { id?: number; title: string; done: boolean; dueDate: string | null; }
export interface GoalStep { id?: number; title: string; weight: number; done: boolean; }
export interface Goal { id?: number; title: string; description: string; targetDate: string | null; steps: GoalStep[]; progress?: number; }
export interface Habit { id?: number; name: string; kind: 'BUILD' | 'QUIT'; startDate?: string; }
export interface HabitLog { id?: number; habitId: number; logDate: string; success: boolean; }
export interface HabitStat { habitId: number; currentStreak: number; bestStreak: number; successRate30: number; }
export interface Mood { id?: number; entryDate: string; score: number; note: string; }
export interface CalEvent { type: 'plan' | 'todo' | 'goal' | 'note'; refId: number; title: string; date: string; at: string | null; done: boolean; }
export interface AppNotification { id: number; title: string; message: string; createdAt: string; seen: boolean; }
export interface Summary {
  totals: {
    todosDone: number; todosOpen: number; plansDone: number; plansOpen: number; plansOverdue: number;
    goalsAverageProgress: number; habitSuccessRate: number; moodAverage: number | null;
  };
  series: { date: string; todosCompleted: number; plansCompleted: number; habitsDone: number; mood: number | null }[];
  insights: string[];
  upcoming: { id: number; title: string; deadline: string }[];
}

// ---------- generic CRUD helper ----------
export class Crud<T extends { id?: number }> {
  constructor(private http: HttpClient, private refresh: RefreshService, private base: string) {}

  list(): Observable<T[]> { return this.http.get<T[]>(this.base); }
  create(body: T): Observable<T> { return this.http.post<T>(this.base, body).pipe(tap(() => this.refresh.bump())); }
  update(body: T): Observable<T> { return this.http.put<T>(`${this.base}/${body.id}`, body).pipe(tap(() => this.refresh.bump())); }
  remove(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/${id}`).pipe(tap(() => this.refresh.bump())); }
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private refresh = inject(RefreshService);

  notes = new Crud<Note>(this.http, this.refresh, '/api/notes');
  plans = new Crud<Plan>(this.http, this.refresh, '/api/plans');
  todos = new Crud<Todo>(this.http, this.refresh, '/api/todos');
  goals = new Crud<Goal>(this.http, this.refresh, '/api/goals');
  habits = new Crud<Habit>(this.http, this.refresh, '/api/habits');

  habitLogs(from: string, to: string) {
    return this.http.get<HabitLog[]>('/api/habits/logs', { params: new HttpParams().set('from', from).set('to', to) });
  }
  habitStats() { return this.http.get<HabitStat[]>('/api/habits/stats'); }
  setHabitLog(id: number, date: string, success: boolean) {
    return this.http.put<HabitLog>(`/api/habits/${id}/log`, { date, success }).pipe(tap(() => this.refresh.bump()));
  }
  clearHabitLog(id: number, date: string) {
    return this.http.delete<void>(`/api/habits/${id}/log`, { params: new HttpParams().set('date', date) }).pipe(tap(() => this.refresh.bump()));
  }

  moods() { return this.http.get<Mood[]>('/api/moods'); }
  setMood(date: string, score: number, note: string) {
    return this.http.put<Mood>(`/api/moods/${date}`, { score, note }).pipe(tap(() => this.refresh.bump()));
  }
  removeMood(date: string) {
    return this.http.delete<void>(`/api/moods/${date}`).pipe(tap(() => this.refresh.bump()));
  }

  calendar(from: string, to: string) {
    return this.http.get<CalEvent[]>('/api/calendar', { params: new HttpParams().set('from', from).set('to', to) });
  }
  summary(days = 14) { return this.http.get<Summary>('/api/analytics/summary', { params: new HttpParams().set('days', days) }); }

  notifications() { return this.http.get<AppNotification[]>('/api/notifications'); }
  markSeen(id: number) { return this.http.post<void>(`/api/notifications/${id}/seen`, {}); }
  markAllSeen() { return this.http.post<void>('/api/notifications/seen-all', {}); }
  pushKey() { return this.http.get('/api/push/public-key', { responseType: 'text' }); }
  subscribePush(sub: { endpoint: string; keys: { p256dh: string; auth: string } }) {
    return this.http.post<void>('/api/push/subscribe', sub);
  }
}
