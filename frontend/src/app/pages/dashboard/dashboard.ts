import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormField, FormRoot, form, required, min, minLength } from '@angular/forms/signals';
import { HttpErrorResponse } from '@angular/common/http';
import { ReplaySubject, mergeMap } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { ComparisonService } from '../../services/comparison.service';
import { Router } from '@angular/router';
import { CurrencyPipe } from '@angular/common';

interface ProfileForm {
  displayName: string;
  monthlySalary: number;
}

interface PasswordForm {
  currentPassword: string;
  newPassword: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [FormField, FormRoot, CurrencyPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  userService = inject(UserService);
  authService = inject(AuthService);
  comparisonService = inject(ComparisonService);
  router = inject(Router);

  hasPreviousMonth = signal(false);
  private readonly thisMonthStart = this.monthStart(0);
  private readonly prevMonthStart = this.monthStart(-1);

  reload$ = new ReplaySubject<void>();

  user = toSignal(
    this.reload$.pipe(mergeMap(() => this.userService.getCurrentUserResult())),
    { initialValue: { data: undefined, loading: true } },
  );

  editing = signal(false);
  successMessage = signal('');

  profileModel = signal<ProfileForm>({ displayName: '', monthlySalary: 0 });

  profileForm = form(this.profileModel, (schemaPath) => {
    required(schemaPath.displayName, { message: 'Name is required' });
    min(schemaPath.monthlySalary, 0, { message: 'Salary cannot be negative' });
  }, {
    submission: {
      action: async (profileForm) => {
        this.successMessage.set('');
        this.userService.updateProfile(profileForm().value()).subscribe(() => {
          this.successMessage.set('Profile updated');
          this.editing.set(false);
          this.reload$.next();
        });
      },
    },
  });

  passwordSuccess = signal('');
  passwordError = signal('');
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
    this.reload$.next();
    this.loadApiKey();
    this.comparisonService.availablePeriods('MONTH').subscribe((periods) => {
      this.hasPreviousMonth.set(periods.some((p) => p.periodStart === this.prevMonthStart));
    });
  }

  compareWithPreviousMonth(): void {
    this.router.navigate(['/comparison'], {
      queryParams: { type: 'MONTH', current: this.thisMonthStart, previous: this.prevMonthStart, run: 1 },
    });
  }

  private monthStart(offset: number): string {
    const now = new Date();
    const d = new Date(now.getFullYear(), now.getMonth() + offset, 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
  }

  onEdit(currentUser: { displayName: string; monthlySalary: number }) {
    this.profileModel.set({
      displayName: currentUser.displayName,
      monthlySalary: currentUser.monthlySalary,
    });
    this.successMessage.set('');
    this.passwordForm().reset({ currentPassword: '', newPassword: '' });
    this.passwordSuccess.set('');
    this.passwordError.set('');
    this.apiKeyInput.set('');
    this.apiKeyMessage.set('');
    this.editing.set(true);
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

  onCancelEdit() {
    this.editing.set(false);
  }

  onLogout() {
    this.authService.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
