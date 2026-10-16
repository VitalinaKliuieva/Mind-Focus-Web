import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export interface User { id: number; email: string; displayName: string; }
interface AuthResponse { token: string; user: User; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  get token(): string | null {
    return localStorage.getItem('mindfocus.token');
  }

  get user(): User | null {
    const raw = localStorage.getItem('mindfocus.user');
    return raw ? (JSON.parse(raw) as User) : null;
  }

  isLoggedIn(): boolean {
    return !!this.token;
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(tap((r) => this.store(r)));
  }

  register(displayName: string, email: string, password: string) {
    return this.http.post<AuthResponse>('/api/auth/register', { displayName, email, password }).pipe(tap((r) => this.store(r)));
  }

  logout(): void {
    localStorage.removeItem('mindfocus.token');
    localStorage.removeItem('mindfocus.user');
    this.router.navigateByUrl('/login');
  }

  private store(r: AuthResponse): void {
    localStorage.setItem('mindfocus.token', r.token);
    localStorage.setItem('mindfocus.user', JSON.stringify(r.user));
  }
}
