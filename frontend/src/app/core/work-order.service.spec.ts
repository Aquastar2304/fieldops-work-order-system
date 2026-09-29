import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { WorkOrderService } from './work-order.service';
import { Page, WorkOrderDetail, WorkOrderSummary } from './models';

describe('WorkOrderService', () => {
  let service: WorkOrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WorkOrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  const emptyPage: Page<WorkOrderSummary> = {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 20,
  };

  it('requests the first page with no status filter by default', () => {
    service.list().subscribe();

    const req = httpMock.expectOne((r) => r.url === '/api/work-orders');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBe(false);
    req.flush(emptyPage);
  });

  it('passes the status through as a query parameter when one is given', () => {
    service.list('IN_PROGRESS').subscribe();

    const req = httpMock.expectOne((r) => r.url === '/api/work-orders');
    expect(req.request.params.get('status')).toBe('IN_PROGRESS');
    req.flush(emptyPage);
  });

  it('omits the status parameter when the filter is cleared to null', () => {
    service.list(null).subscribe();

    const req = httpMock.expectOne((r) => r.url === '/api/work-orders');
    expect(req.request.params.has('status')).toBe(false);
    req.flush(emptyPage);
  });

  it('returns the page content to the caller', () => {
    const page: Page<WorkOrderSummary> = {
      ...emptyPage,
      totalElements: 1,
      totalPages: 1,
      content: [
        {
          id: 1,
          reference: 'WO-000001',
          title: 'Pump vibration',
          status: 'OPEN',
          priority: 'HIGH',
          assetTag: 'PMP-001',
          technicianName: null,
          openedAt: '2026-03-01T08:00:00Z',
          closedAt: null,
        },
      ],
    };

    let received: Page<WorkOrderSummary> | undefined;
    service.list().subscribe((p) => (received = p));
    httpMock.expectOne((r) => r.url === '/api/work-orders').flush(page);

    expect(received?.content.length).toBe(1);
    expect(received?.content[0].reference).toBe('WO-000001');
  });

  it('posts a new work order to the collection endpoint', () => {
    service
      .create({
        title: 'Seal leak',
        description: null,
        assetId: 10,
        technicianId: null,
        priority: 'MEDIUM',
      })
      .subscribe();

    const req = httpMock.expectOne('/api/work-orders');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      title: 'Seal leak',
      description: null,
      assetId: 10,
      technicianId: null,
      priority: 'MEDIUM',
    });
    req.flush({} as WorkOrderDetail);
  });

  it('posts a status change to the status sub-resource', () => {
    service.changeStatus(100, 'CLOSED').subscribe();

    const req = httpMock.expectOne('/api/work-orders/100/status');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ status: 'CLOSED' });
    req.flush({} as WorkOrderDetail);
  });

  it('posts a technician assignment to the technician sub-resource', () => {
    service.assignTechnician(100, 20).subscribe();

    const req = httpMock.expectOne('/api/work-orders/100/technician');
    expect(req.request.body).toEqual({ technicianId: 20 });
    req.flush({} as WorkOrderDetail);
  });

  it('posts a part consumption with its quantity', () => {
    service.addPart(100, 30, 4).subscribe();

    const req = httpMock.expectOne('/api/work-orders/100/parts');
    expect(req.request.body).toEqual({ partId: 30, quantity: 4 });
    req.flush({} as WorkOrderDetail);
  });

  it('surfaces a backend error to the subscriber rather than swallowing it', () => {
    let status: number | undefined;
    service.get(404).subscribe({ error: (e) => (status = e.status) });

    httpMock
      .expectOne('/api/work-orders/404')
      .flush({ message: 'Work order 404 was not found' }, { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });
});
