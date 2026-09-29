import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { LoginResponse } from './models';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  function signIn(): void {
    auth.login({ username: 'admin', password: 'admin123' }).subscribe();
    httpMock.expectOne('/api/auth/login').flush({
      token: 'a.b.c',
      username: 'admin',
      role: 'ADMIN',
      expiresInSeconds: 3600,
    } as LoginResponse);
  }

  it('sends no Authorization header while signed out', () => {
    http.get('/api/work-orders').subscribe();

    const req = httpMock.expectOne('/api/work-orders');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('attaches the bearer token once signed in', () => {
    signIn();

    http.get('/api/work-orders').subscribe();

    const req = httpMock.expectOne('/api/work-orders');
    expect(req.request.headers.get('Authorization')).toBe('Bearer a.b.c');
    req.flush([]);
  });

  it('never attaches the token to the login request itself', () => {
    signIn();

    auth.login({ username: 'admin', password: 'admin123' }).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ token: 'x', username: 'admin', role: 'ADMIN', expiresInSeconds: 3600 });
  });

  it('stops attaching the token after sign out', () => {
    signIn();
    auth.logout();

    http.get('/api/work-orders').subscribe();

    const req = httpMock.expectOne('/api/work-orders');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });
});
