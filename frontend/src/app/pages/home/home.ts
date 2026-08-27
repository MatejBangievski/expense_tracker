import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { UserService } from '../../services/user.service';
import { ExpensesOverview } from './expenses-overview/expenses-overview';

@Component({
  selector: 'app-home',
  imports: [ExpensesOverview],
  templateUrl: './home.html',
})
export class Home {
  private userService = inject(UserService);

  user = toSignal(this.userService.getCurrentUser());

  greeting = computed(() => {
    const hour = new Date().getHours();
    if (hour < 12) {
      return 'Good morning';
    }
    if (hour < 18) {
      return 'Good afternoon';
    }
    return 'Good evening';
  });
}
