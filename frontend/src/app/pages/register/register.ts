import { Component, inject, signal } from '@angular/core';
import { FormField, FormRoot, form, minLength, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface RegisterForm {
  displayName: string;
  email: string;
  password: string;
}

@Component({
  selector: 'app-register',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  authService = inject(AuthService);
  router = inject(Router);

  errorMessage = signal('');

  registerModel = signal<RegisterForm>({ displayName: '', email: '', password: '' });

  registerForm = form(this.registerModel, (schemaPath) => {
    required(schemaPath.displayName, { message: 'Name is required' });
    required(schemaPath.email, { message: 'Email is required' });
    required(schemaPath.password, { message: 'Password is required' });
    minLength(schemaPath.password, 6, { message: 'Password must be at least 6 characters' });
  }, {
    submission: {
      action: async (registerForm) => {
        this.errorMessage.set('');
        this.authService.register(registerForm().value()).subscribe({
          next: () => this.router.navigate(['/dashboard']),
          error: () => this.errorMessage.set('Email is already in use')
        });
      }
    }
  });
}
