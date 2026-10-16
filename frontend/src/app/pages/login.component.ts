import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { errorText } from '../core/dates';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  styles: [`
    :host { display: grid; place-items: center; min-height: 100vh; padding: 1.5rem; }
    .cover { width: min(440px, 100%); background: var(--ink); color: var(--paper); padding: 2.2rem 2rem; border: 2px solid var(--ink); box-shadow: 6px 6px 0 rgba(0,0,0,.35); }
    .label { background: var(--paper); color: var(--ink); border: 2px solid var(--ink); padding: 1.2rem; background-image: repeating-linear-gradient(to bottom, transparent 0 27px, rgba(0,0,0,.14) 27px 28px); }
    .cover h1 { font-size: 2.6rem; margin-bottom: 1.2rem; text-align: center; }
    .switch { background: none; border: 0; font: inherit; text-decoration: underline; cursor: pointer; padding: 0; color: inherit; }
  `],
  template: `
    <div class="cover">
      <h1>MindFocus</h1>
      <form class="label stack" (ngSubmit)="submit()">
        <h3>{{ register ? 'Start a new diary' : 'Open your diary' }}</h3>
        @if (register) {
          <label class="field">Your name <input name="name" [(ngModel)]="displayName" required maxlength="60"></label>
        }
        <label class="field">Email <input name="email" type="email" [(ngModel)]="email" required autocomplete="email"></label>
        <label class="field">Password @if (register) { <small>(at least 8 characters)</small> }
          <input name="password" type="password" [(ngModel)]="password" required minlength="8" [autocomplete]="register ? 'new-password' : 'current-password'">
        </label>
        @if (error) { <div class="error">{{ error }}</div> }
        <button class="btn mint" type="submit" [disabled]="busy">{{ register ? 'Create account' : 'Log in' }}</button>
        <p>
          @if (register) { Have a diary already? <button type="button" class="switch" (click)="register = false">Log in</button> }
          @else { New here? <button type="button" class="switch" (click)="register = true">Create an account</button> }
        </p>
      </form>
    </div>
  `,
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  register = false;
  displayName = '';
  email = '';
  password = '';
  error = '';
  busy = false;

  submit(): void {
    this.error = '';
    this.busy = true;
    const call = this.register
      ? this.auth.register(this.displayName, this.email, this.password)
      : this.auth.login(this.email, this.password);
    call.subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (e) => { this.error = errorText(e); this.busy = false; },
    });
  }
}
