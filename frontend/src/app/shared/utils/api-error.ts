export function apiErrorMessage(error: unknown, fallback: string): string {
  const response = error as { error?: { message?: string; fieldErrors?: Record<string, string> } };
  const fieldErrors = response?.error?.fieldErrors;
  if (fieldErrors && Object.keys(fieldErrors).length) {
    return Object.values(fieldErrors).join(' ');
  }
  return response?.error?.message || fallback;
}
