import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class PushService {
  private api = inject(ApiService);

  supported(): boolean {
    return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
  }

  /** Asks permission, subscribes this browser and sends the subscription to the backend. Returns a message for the user. */
  async enable(): Promise<string> {
    if (!this.supported()) return 'This browser does not support push notifications.';
    const key = (await firstValueFrom(this.api.pushKey())).trim();
    if (!key) return 'Push is not configured on the server yet (missing VAPID keys).';
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') return 'Notifications were blocked in the browser.';

    const reg = await navigator.serviceWorker.register('/sw.js');
    await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: this.toUint8(key),
    });
    const json = sub.toJSON();
    await firstValueFrom(
      this.api.subscribePush({
        endpoint: json.endpoint!,
        keys: { p256dh: json.keys!['p256dh'], auth: json.keys!['auth'] },
      }),
    );
    return 'Browser alerts are on.';
  }

  private toUint8(base64: string): Uint8Array {
    const padding = '='.repeat((4 - (base64.length % 4)) % 4);
    const raw = atob((base64 + padding).replace(/-/g, '+').replace(/_/g, '/'));
    return Uint8Array.from([...raw].map((c) => c.charCodeAt(0)));
  }
}
