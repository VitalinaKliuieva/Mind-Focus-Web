import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Fires after every successful change (add, tick, edit, delete).
 * The dashboard and the reminder bell listen to it and reload, so the numbers
 * and insights adapt immediately after each action.
 */
@Injectable({ providedIn: 'root' })
export class RefreshService {
  private subject = new Subject<void>();
  readonly changes$ = this.subject.asObservable();

  bump(): void {
    this.subject.next();
  }
}
