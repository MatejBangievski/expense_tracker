import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExpenseRow } from './expense-row';

describe('ExpenseRow', () => {
  let component: ExpenseRow;
  let fixture: ComponentFixture<ExpenseRow>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpenseRow],
    }).compileComponents();

    fixture = TestBed.createComponent(ExpenseRow);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
