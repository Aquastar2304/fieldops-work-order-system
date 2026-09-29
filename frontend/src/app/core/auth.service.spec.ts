import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';
import { LoginResponse } from './models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const response: LoginResponse = {
    token: 'a.b.c',
    username: 'supervisor',
    role: 'SUPERVISOR',
    expiresInSeconds: 3600,
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts out unauthenticated', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('stores the token and username after a successful login', () => {
    service.login({ username: 'supervisor', password: 'super123' }).subscribe();
    httpMock.expectOne('/api/auth/login').flush(response);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('a.b.c');
    expect(service.username()).toBe('supervisor');
  });

  it('persists the session so a reload stays signed in', () => {
    service.login({ username: 'supervisor', password: 'super123' }).subscribe();
    httpMock.expectOne('/api/auth/login').flush(response);

    expect(localStorage.getItem('fieldops.token')).toBe('a.b.c');
  });

  it('stays unauthenticated when the credentials are rejected', () => {
    service.login({ username: 'supervisor', password: 'wrong' }).subscribe({ error: () => {} });
    httpMock
      .expectOne('/api/auth/login')
      .flush({ message: 'Invalid username or password' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
  });

  it('clears the session on sign out', () => {
    service.login({ username: 'supervisor', password: 'super123' }).subscribe();
    httpMock.expectOne('/api/auth/login').flush(response);

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('fieldops.token')).toBeNull();
  });
});
