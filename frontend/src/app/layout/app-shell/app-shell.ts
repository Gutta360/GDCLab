import { Component } from '@angular/core';
import {
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
  Router
} from '@angular/router';

@Component({
  selector: 'app-app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss'
})
export class AppShell {
  openMenu: string | null = null;

  constructor(
    private router: Router
  ) {}

  toggleMenu(menu: string): void {
    this.openMenu = this.openMenu === menu
      ? null
      : menu;
  }

  closeMenu(): void {
    this.openMenu = null;
  }

  isMenuOpen(menu: string): boolean {
    return this.openMenu === menu;
  }

  isSectionActive(paths: string[]): boolean {
    return paths.some(path => this.router.url === path || this.router.url.startsWith(`${path}/`));
  }

  logout(): void {
    this.closeMenu();
    this.router.navigate(['/login']);
  }
}
