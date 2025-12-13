import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();
    const method = request.method;
    const url = request.url;

    console.log(`🔐 [JWT Interceptor] ${method} ${url}`);
    console.log(`🔐 [JWT Interceptor] Token exists: ${!!token}`);

    if (token) {
      console.log(`🔐 [JWT Interceptor] Token (first 50 chars): ${token.substring(0, 50)}...`);
      console.log(`🔐 [JWT Interceptor] Attaching Authorization header`);
      request = request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
      console.log(`🔐 [JWT Interceptor] Authorization header attached`);
    } else {
      console.warn(`🔐 [JWT Interceptor] ⚠️  No token found in localStorage for ${url}`);
    }

    return next.handle(request);
  }
}
