import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Asset,
  CreateWorkOrderRequest,
  Page,
  Part,
  Site,
  Technician,
  WorkOrderDetail,
  WorkOrderStatus,
  WorkOrderSummary,
} from './models';

/** Single place that knows the shape of the work order API. */
@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  list(status?: WorkOrderStatus | null, page = 0, size = 20): Observable<Page<WorkOrderSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<WorkOrderSummary>>(`${this.baseUrl}/work-orders`, { params });
  }

  get(id: number): Observable<WorkOrderDetail> {
    return this.http.get<WorkOrderDetail>(`${this.baseUrl}/work-orders/${id}`);
  }

  create(request: CreateWorkOrderRequest): Observable<WorkOrderDetail> {
    return this.http.post<WorkOrderDetail>(`${this.baseUrl}/work-orders`, request);
  }

  assignTechnician(id: number, technicianId: number): Observable<WorkOrderDetail> {
    return this.http.post<WorkOrderDetail>(`${this.baseUrl}/work-orders/${id}/technician`, {
      technicianId,
    });
  }

  changeStatus(id: number, status: WorkOrderStatus): Observable<WorkOrderDetail> {
    return this.http.post<WorkOrderDetail>(`${this.baseUrl}/work-orders/${id}/status`, { status });
  }

  addPart(id: number, partId: number, quantity: number): Observable<WorkOrderDetail> {
    return this.http.post<WorkOrderDetail>(`${this.baseUrl}/work-orders/${id}/parts`, {
      partId,
      quantity,
    });
  }

  assets(): Observable<Asset[]> {
    return this.http.get<Asset[]>(`${this.baseUrl}/assets`);
  }

  technicians(): Observable<Technician[]> {
    return this.http.get<Technician[]>(`${this.baseUrl}/technicians`);
  }

  parts(): Observable<Part[]> {
    return this.http.get<Part[]>(`${this.baseUrl}/parts`);
  }

  sites(): Observable<Site[]> {
    return this.http.get<Site[]>(`${this.baseUrl}/sites`);
  }
}
