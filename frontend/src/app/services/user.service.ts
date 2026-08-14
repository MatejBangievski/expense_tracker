import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Result } from '../models/result';
import { UpdateUserRequest, User } from '../models/user';

@Service()
export class UserService {
  http = inject(HttpClient);

  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`/api/users/me`);
  }

  getCurrentUserResult(): Observable<Result<User>> {
    return this.http.get<User>(`/api/users/me`).pipe(
      map((result) => ({ data: result, loading: false })),
    );
  }

  updateProfile(request: UpdateUserRequest) {
    return this.http.put<User>(`/api/users/me`, request);
  }
}
