import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly username = 'admin';
  private readonly password = 'admin123';

  login(username: string, password: string): boolean {
    return username === this.username && password === this.password;
  }
}