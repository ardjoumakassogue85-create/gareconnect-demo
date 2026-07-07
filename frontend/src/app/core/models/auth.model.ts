export type Role = 'CLIENT' | 'COMPAGNIE';

export interface RegisterRequest {
  email: string;
  password: string;
  nom: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordResponse {
  message: string;
  temporaryPassword?: string;
}

export interface RegisterResponse {
  message: string;
  email: string;
}

export interface VerifyEmailRequest {
  email: string;
  code: string;
}

export interface VerifyEmailResponse {
  message: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  nom: string;
  role: Role;
  expiresAt: string;
}
