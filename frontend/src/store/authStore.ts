import { create } from 'zustand';
import type { User } from '../types';
import { api } from '../api/client';
import type { LoginResponse } from '../types';

function readStoredUser(): User | null {
  const raw = localStorage.getItem('user');
  if (!raw) return null;

  try {
    return JSON.parse(raw) as User;
  } catch {
    localStorage.removeItem('user');
    return null;
  }
}

function readStoredAuth() {
  const token = localStorage.getItem('token');
  const user = readStoredUser();

  if (!token || !user) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    return { token: null, user: null };
  }

  return { token, user };
}

function persistAuth(token: string, user: User) {
  localStorage.setItem('token', token);
  localStorage.setItem('user', JSON.stringify(user));
}

function clearPersistedAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
}

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (email: string, password: string, name: string, role: User['role'], inviteCode?: string) => Promise<void>;
  logout: () => void;
  syncFromStorage: () => void;
}

const storedAuth = readStoredAuth();

export const useAuthStore = create<AuthState>((set) => ({
  user: storedAuth.user,
  token: storedAuth.token,
  isAuthenticated: storedAuth.token !== null && storedAuth.user !== null,

  login: async (email, password) => {
    const data = await api.post<LoginResponse>('/api/auth/login', { email, password });
    persistAuth(data.token, data.user);
    set({ user: data.user, token: data.token, isAuthenticated: true });
    return data.user;
  },

  register: async (email, password, name, role, inviteCode?) => {
    const body: Record<string, string> = { email, password, name, role };
    if (inviteCode) body.inviteCode = inviteCode;
    const data = await api.post<LoginResponse>('/api/auth/register', body);
    persistAuth(data.token, data.user);
    set({ user: data.user, token: data.token, isAuthenticated: true });
  },

  logout: () => {
    // Fire-and-forget: tell backend to revoke the token. We always clear local state
    // whether or not the network call succeeds (user expects to be logged out).
    const token = localStorage.getItem('token');
    if (token) {
      void api.post('/api/auth/logout').catch(() => { /* best-effort */ });
    }
    clearPersistedAuth();
    set({ user: null, token: null, isAuthenticated: false });
  },

  syncFromStorage: () => {
    const { token, user } = readStoredAuth();
    set({ user, token, isAuthenticated: token !== null && user !== null });
  },
}));
