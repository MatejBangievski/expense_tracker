import { Component, OnInit, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, minLength, required } from '@angular/forms/signals';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '../../services/user.service';

interface PasswordForm {
  currentPassword: string;
  newPassword: string;
}

@Component({
  selector: 'app-profile',
  imports: [FormField, FormRoot],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  userService = inject(UserService);

  passwordError = signal('');
  passwordSuccess = signal('');
  passwordModel = signal<PasswordForm>({ currentPassword: '', newPassword: '' });

  passwordForm = form(
    this.passwordModel,
    (schemaPath) => {
      required(schemaPath.currentPassword, { message: 'Current password is required' });
      required(schemaPath.newPassword, { message: 'New password is required' });
      minLength(schemaPath.newPassword, 6, { message: 'New password must be at least 6 characters' });
    },
    {
      submission: {
        action: async (passwordForm) => {
          this.passwordError.set('');
          this.passwordSuccess.set('');
          this.userService.changePassword(passwordForm().value()).subscribe({
            next: () => {
              this.passwordSuccess.set('Password updated');
              passwordForm().reset({ currentPassword: '', newPassword: '' });
            },
            error: (err: HttpErrorResponse) =>
              this.passwordError.set(
                err.status === 400 ? 'Current password is incorrect' : 'Could not change password',
              ),
          });
        },
      },
    },
  );

  apiKeyMasked = signal<string | null>(null);
  apiKeyInput = signal('');
  apiKeyMessage = signal('');

  ngOnInit(): void {
    this.loadApiKey();
  }

  loadApiKey(): void {
    this.userService.getApiKey().subscribe((key) => this.apiKeyMasked.set(key));
  }

  saveApiKey(): void {
    const key = this.apiKeyInput().trim();
    if (!key) {
      return;
    }
    this.userService.setApiKey(key).subscribe((response) => {
      this.apiKeyMasked.set(response.apiKey);
      this.apiKeyInput.set('');
      this.apiKeyMessage.set('API key saved');
    });
  }

  removeApiKey(): void {
    this.userService.clearApiKey().subscribe(() => {
      this.apiKeyMasked.set(null);
      this.apiKeyMessage.set('API key removed');
    });
  }
}
