import { dateRangeFor } from './date-presets';

describe('dateRangeFor', () => {
  const wednesday = new Date(2026, 9, 7, 15, 30);

  it('keeps local calendar dates without UTC conversion', () => {
    expect(dateRangeFor('TODAY', wednesday)).toEqual({ fromDate: '2026-10-07', toDate: '2026-10-07' });
    expect(dateRangeFor('YESTERDAY', wednesday)).toEqual({ fromDate: '2026-10-06', toDate: '2026-10-06' });
  });

  it('handles week and month boundaries', () => {
    expect(dateRangeFor('THIS_WEEK', wednesday)).toEqual({ fromDate: '2026-10-05', toDate: '2026-10-07' });
    expect(dateRangeFor('LAST_MONTH', wednesday)).toEqual({ fromDate: '2026-09-01', toDate: '2026-09-30' });
  });
});
