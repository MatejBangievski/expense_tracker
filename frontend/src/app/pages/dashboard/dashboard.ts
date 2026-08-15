import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormField, FormRoot, form, required, min } from '@angular/forms/signals';
import { ReplaySubject, mergeMap } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { Router } from '@angular/router';
import { CurrencyPipe } from '@angular/common';

interface ProfileForm {
  displayName: string;
  monthlySalary: number;
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
  router = inject(Router);

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

  ngOnInit(): void {
    this.reload$.next();
  }

  onEdit(currentUser: { displayName: string; monthlySalary: number }) {
    this.profileModel.set({
      displayName: currentUser.displayName,
      monthlySalary: currentUser.monthlySalary,
    });
    this.successMessage.set('');
    this.editing.set(true);
  }

  onCancelEdit() {
    this.editing.set(false);
  }

  onLogout() {
    this.authService.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
