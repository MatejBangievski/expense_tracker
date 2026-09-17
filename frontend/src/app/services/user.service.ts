import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Result } from '../models/result';
import { ApiKeyResponse, ChangePasswordRequest, UpdateUserRequest, User } from '../models/user';

@Service()
export class UserService {
  http = inject(HttpClient);

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`/api/users/me`);
  }

  getCurrentUserResult(): Observable<Result<User>> {
    return this.http
      .get<User>(`/api/users/me`)
      .pipe(map((result) => ({ data: result, loading: false })));
  }

  updateProfile(request: UpdateUserRequest) {
    return this.http.put<User>(`/api/users/me`, request);
  }

  getApiKey(): Observable<string | null> {
    return this.http.get<ApiKeyResponse>(`/api/users/me/api-key`).pipe(map((r) => r.apiKey));
  }

  setApiKey(apiKey: string): Observable<ApiKeyResponse> {
    return this.http.put<ApiKeyResponse>(`/api/users/me/api-key`, { apiKey });
  }

  clearApiKey(): Observable<void> {
    return this.http.delete<void>(`/api/users/me/api-key`);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`/api/users/me/password`, request);
  }
}
