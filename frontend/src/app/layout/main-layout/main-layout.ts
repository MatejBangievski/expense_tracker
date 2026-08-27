import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
})
export class MainLayout {
  private userService = inject(UserService);

  user = toSignal(this.userService.getCurrentUser());

  initials = computed(() => {
    const name = this.user()?.displayName ?? '';
    return name
      .split(' ')
      .filter((part) => part.length > 0)
      .map((part) => part.charAt(0))
      .join('')
      .slice(0, 2)
      .toUpperCase();
  });
}
