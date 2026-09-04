import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-icon',
  templateUrl: './icon.html',
  styleUrl: './icon.css',
  host: {
    '[style.width]': 'size()',
    '[style.height]': 'size()',
  },
})
export class Icon {
  name = input.required<string>();
  label = input('');
  size = input('1.4rem');

  protected href = computed(() => `/icons.svg#${this.name()}`);
}
