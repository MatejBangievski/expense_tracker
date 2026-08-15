import { computed, inject, Service, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthTokens, LoginRequest, RegisterRequest } from '../models/auth';
import { catchError, of, tap } from 'rxjs';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

@Service()
export class AuthService {
  http = inject(HttpClient);
  accessToken = signal(localStorage.getItem(ACCESS_TOKEN_KEY));
  refreshToken = signal(localStorage.getItem(REFRESH_TOKEN_KEY));

  isAuthenticated = computed(() => !!this.accessToken());

  login(request: LoginRequest) {
    return this.http.post<AuthTokens>(`/api/auth/login`, request).pipe(
      tap((tokens) => this.storeTokens(tokens)),
    );
  }
  register(request: RegisterRequest) {
    return this.http.post<AuthTokens>(`/api/auth/register`, request).pipe(
      tap((tokens) => this.storeTokens(tokens)),
    );
  }

  logout() {
    const token = this.refreshToken();
    this.clearTokens();
    if (!token) {
      return of(undefined);
    }
    return this.http.post(`/api/auth/logout`, { refreshToken: token }).pipe(
      catchError(() => of(undefined)),
    );
  }
  private storeTokens(tokens: AuthTokens) {
    this.accessToken.set(tokens.accessToken);
    this.refreshToken.set(tokens.refreshToken);
    localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  }

  private clearTokens() {
    this.accessToken.set(null);
    this.refreshToken.set(null);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}
