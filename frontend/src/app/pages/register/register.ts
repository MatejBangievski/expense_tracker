import { Component, inject, signal } from '@angular/core';
import { FormField, FormRoot, form, minLength, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';

interface RegisterForm {
  displayName: string;
  email: string;
  password: string;
  apiKey: string;
}

@Component({
  selector: 'app-register',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './register.html'
})
export class Register {
  authService = inject(AuthService);
  userService = inject(UserService);
  router = inject(Router);

  errorMessage = signal('');

  registerModel = signal<RegisterForm>({ displayName: '', email: '', password: '', apiKey: '' });

  registerForm = form(this.registerModel, (schemaPath) => {
    required(schemaPath.displayName, { message: 'Name is required' });
    required(schemaPath.email, { message: 'Email is required' });
    required(schemaPath.password, { message: 'Password is required' });
    minLength(schemaPath.password, 6, { message: 'Password must be at least 6 characters' });
  }, {
    submission: {
      action: async (registerForm) => {
        this.errorMessage.set('');
        const { apiKey, ...registerRequest } = registerForm().value();
        this.authService.register(registerRequest).subscribe({
          next: () => {
            const key = apiKey.trim();
            if (key) {
              this.userService.setApiKey(key).subscribe({
                next: () => this.router.navigate(['/home']),
                error: () => this.router.navigate(['/home']),
              });
            } else {
              this.router.navigate(['/home']);
            }
          },
          error: () => this.errorMessage.set('Email is already in use'),
        });
      }
    }
  });
}
