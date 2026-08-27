import { Component, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface LoginForm {
  email: string;
  password: string;
}


@Component({
  selector: 'app-login',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './login.html'
})
export class Login {
  authService = inject(AuthService);
  router = inject(Router);

  errorMessage = signal('');
  loginModel = signal<LoginForm>({ email: '', password: '' });

  loginForm = form(this.loginModel, (schemaPath) => {
    required(schemaPath.email, { message: 'Email is required' });
    required(schemaPath.password, { message: 'Password is required' });
  }, {
    submission: {
      action: async (loginForm) => {
        this.errorMessage.set('');
        this.authService.login(loginForm().value()).subscribe({
          next: () => this.router.navigate(['/home']),
          error: () => this.errorMessage.set('Invalid email or password'),
        });
      },
    },
  });
}
