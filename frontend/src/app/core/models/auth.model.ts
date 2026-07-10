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
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
}

export interface ResetPasswordResponse {
  message: string;
}

export interface AccountResponse {
  id: number;
  email: string;
  nom: string;
  role: Role;
  emailVerified: boolean;
}

export interface UpdateAccountRequest {
  nom: string;
  email: string;
  motDePasseActuel?: string;
  nouveauMotDePasse?: string;
}

export interface RegisterResponse {
  message: string;
  email: string;
  verificationRequise: boolean;
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
