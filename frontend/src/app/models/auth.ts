export interface LoginRequest {
  email: string;
  password: string;
}
export interface RegisterRequest {
  displayName: string;
  email: string;
  password: string;
}
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}
