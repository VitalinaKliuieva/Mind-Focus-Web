const pad = (n: number) => String(n).padStart(2, '0');

/** Local date as YYYY-MM-DD (not UTC, so "today" is the user's today). */
export function iso(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export const today = (): string => iso(new Date());

export function addDays(date: string, n: number): string {
  const d = new Date(date + 'T00:00:00');
  d.setDate(d.getDate() + n);
  return iso(d);
}

export function errorText(err: any): string {
  return err?.error?.message || (err?.status === 0 ? 'Cannot reach the server. Is the backend running?' : 'Something went wrong. Try again.');
}
