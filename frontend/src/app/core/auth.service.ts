import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { LoginRequest, LoginResponse } from './models';

const TOKEN_KEY = 'fieldops.token';
const USER_KEY = 'fieldops.user';

/** Holds the bearer token and exposes who is signed in. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly tokenSignal = signal<string | null>(readStorage(TOKEN_KEY));
  private readonly usernameSignal = signal<string | null>(readStorage(USER_KEY));

  readonly username = this.usernameSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', request).pipe(
      tap((response) => {
        this.tokenSignal.set(response.token);
        this.usernameSignal.set(response.username);
        writeStorage(TOKEN_KEY, response.token);
        writeStorage(USER_KEY, response.username);
      }),
    );
  }

  logout(): void {
    this.tokenSignal.set(null);
    this.usernameSignal.set(null);
    removeStorage(TOKEN_KEY);
    removeStorage(USER_KEY);
  }

  token(): string | null {
    return this.tokenSignal();
  }
}

// localStorage is unavailable in some test and SSR contexts, so every access is guarded.
function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Not being able to persist the session is not worth failing the login over.
  }
}

function removeStorage(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    // Ignore.
  }
}
