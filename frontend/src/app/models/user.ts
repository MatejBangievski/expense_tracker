export interface User {
  id: number;
  displayName: string;
  email: string;
  monthlySalary: number;
  totalSaved: number;
}

export interface UpdateUserRequest {
  displayName: string;
  monthlySalary: number;
}

export interface ApiKeyResponse {
  apiKey: string | null;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
