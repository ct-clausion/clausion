import { toApiUrl } from '../lib/apiBase';

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

function getToken(): string | null {
  return localStorage.getItem('token');
}

// Guard against multiple concurrent 401s triggering duplicate logouts / navigations.
let unauthorizedHandled = false;

async function handleUnauthorized(): Promise<void> {
  if (unauthorizedHandled) return;
  unauthorizedHandled = true;
  // Dynamic import avoids a circular dep (authStore imports this module for api.post).
  const { useAuthStore } = await import('../store/authStore');
  useAuthStore.getState().logout();
  // AuthSessionSync in App.tsx reacts to the token becoming null and navigates via
  // react-router. Reset the guard after a tick so subsequent sessions can re-trigger.
  setTimeout(() => {
    unauthorizedHandled = false;
  }, 0);
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  const isAuthEndpoint = path.startsWith('/api/auth/login') || path.startsWith('/api/auth/register');
  const token = getToken();
  if (token && !isAuthEndpoint) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(toApiUrl(path), {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    if (res.status === 401 && !isAuthEndpoint && token) {
      await handleUnauthorized();
    }
    const errorBody = await res.json().catch(() => null);
    const message =
      errorBody?.message ?? errorBody?.error ?? `Request failed: ${res.status}`;
    throw new ApiError(res.status, message, errorBody);
  }

  // 204 No Content
  if (res.status === 204) {
    return undefined as T;
  }

  return res.json() as Promise<T>;
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  delete: <T>(path: string) => request<T>('DELETE', path),
};
