import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'today' },
      { path: 'today', loadComponent: () => import('./pages/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'notes', loadComponent: () => import('./pages/notes.component').then((m) => m.NotesComponent) },
      { path: 'planner', loadComponent: () => import('./pages/planner.component').then((m) => m.PlannerComponent) },
      { path: 'todos', loadComponent: () => import('./pages/todos.component').then((m) => m.TodosComponent) },
      { path: 'goals', loadComponent: () => import('./pages/goals.component').then((m) => m.GoalsComponent) },
      { path: 'habits', loadComponent: () => import('./pages/habits.component').then((m) => m.HabitsComponent) },
      { path: 'mood', loadComponent: () => import('./pages/mood.component').then((m) => m.MoodComponent) },
      { path: 'calendar', loadComponent: () => import('./pages/calendar.component').then((m) => m.CalendarComponent) },
    ],
  },
  { path: '**', redirectTo: '' },
];
