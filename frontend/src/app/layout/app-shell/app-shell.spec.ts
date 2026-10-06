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
    component.toggleMenu('patients');
    expect(component.isMenuOpen('patients')).toBe(true);

    component.toggleMenu('tab-2');
    expect(component.isMenuOpen('patients')).toBe(false);
    expect(component.isMenuOpen('tab-2')).toBe(true);
  });

  it('should close an open menu when toggled again', () => {
    component.toggleMenu('patients');
    component.toggleMenu('patients');

    expect(component.isMenuOpen('patients')).toBe(false);
  });
});
