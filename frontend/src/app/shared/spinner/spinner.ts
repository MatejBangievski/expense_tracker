import { Component, input } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-spinner',
  imports: [MatProgressSpinner],
  template: `
    <div class="d-flex justify-content-center py-5">
      <mat-progress-spinner mode="indeterminate" [diameter]="diameter()" aria-label="Loading" />
    </div>
  `,
})
export class Spinner {
  diameter = input(48);
}
