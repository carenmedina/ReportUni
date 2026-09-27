import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NavegacionResponse } from '../models/navegacion.model';

@Injectable({ providedIn: 'root' })
export class NavegacionService {
  constructor(private readonly http: HttpClient) {}

  obtenerMenu(): Observable<NavegacionResponse> {
    return this.http.get<NavegacionResponse>(`${environment.apiBaseUrl}/navegacion/menu`);
  }
}
