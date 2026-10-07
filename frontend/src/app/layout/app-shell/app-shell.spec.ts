import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppShell } from './app-shell';

describe('AppShell', () => {
  let component: AppShell;
  let fixture: ComponentFixture<AppShell>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: [
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppShell);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should switch from one open menu to another', () => {
    component.toggleMenu('registrations');
    expect(component.isMenuOpen('registrations')).toBe(true);

    component.toggleMenu('tests');
    expect(component.isMenuOpen('registrations')).toBe(false);
    expect(component.isMenuOpen('tests')).toBe(true);
  });

  it('should close an open menu when toggled again', () => {
    component.toggleMenu('payments');
    component.toggleMenu('payments');

    expect(component.isMenuOpen('payments')).toBe(false);
  });
});
