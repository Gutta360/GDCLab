export type DatePreset = 'TODAY' | 'YESTERDAY' | 'THIS_WEEK' | 'LAST_WEEK' | 'THIS_MONTH' | 'LAST_MONTH' | 'CUSTOM';

export interface DateRangeValue {
  fromDate: string;
  toDate: string;
}

export function dateRangeFor(preset: DatePreset, today = new Date()): DateRangeValue | null {
  if (preset === 'CUSTOM') return null;
  const start = atLocalMidnight(today);
  const end = atLocalMidnight(today);

  if (preset === 'YESTERDAY') {
    start.setDate(start.getDate() - 1);
    end.setDate(end.getDate() - 1);
  } else if (preset === 'THIS_WEEK') {
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
  } else if (preset === 'LAST_WEEK') {
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7) - 7);
    end.setTime(start.getTime());
    end.setDate(end.getDate() + 6);
  } else if (preset === 'THIS_MONTH') {
    start.setDate(1);
  } else if (preset === 'LAST_MONTH') {
    start.setMonth(start.getMonth() - 1, 1);
    end.setDate(0);
  }

  return { fromDate: localDate(start), toDate: localDate(end) };
}

export function localDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function localDateTime(date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${localDate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function atLocalMidnight(value: Date): Date {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}
