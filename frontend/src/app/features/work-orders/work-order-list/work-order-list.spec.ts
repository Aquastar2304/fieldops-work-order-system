import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { WorkOrderList } from './work-order-list';
import { Page, WorkOrderSummary } from '../../../core/models';

describe('WorkOrderList', () => {
  let fixture: ComponentFixture<WorkOrderList>;
  let httpMock: HttpTestingController;

  const rows: WorkOrderSummary[] = [
    {
      id: 1,
      reference: 'WO-000001',
      title: 'Excessive vibration on start-up',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      assetTag: 'PMP-001',
      technicianName: 'Dana Okoro',
      openedAt: '2026-03-01T08:00:00Z',
      closedAt: null,
    },
    {
      id: 2,
      reference: 'WO-000003',
      title: 'Level transmitter reading erratic',
      status: 'OPEN',
      priority: 'MEDIUM',
      assetTag: 'SEP-003',
      technicianName: null,
      openedAt: '2026-03-02T08:00:00Z',
      closedAt: null,
    },
  ];

  function page(content: WorkOrderSummary[]): Page<WorkOrderSummary> {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20 };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkOrderList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkOrderList);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads work orders on creation', async () => {
    httpMock.expectOne((r) => r.url === '/api/work-orders').flush(page(rows));
    await fixture.whenStable();
    fixture.detectChanges();

    const body = fixture.nativeElement as HTMLElement;
    expect(body.querySelectorAll('tbody tr').length).toBe(2);
    expect(body.textContent).toContain('WO-000001');
    expect(body.textContent).toContain('Dana Okoro');
  });

  it('shows an em dash where no technician is assigned', async () => {
    httpMock.expectOne((r) => r.url === '/api/work-orders').flush(page(rows));
    await fixture.whenStable();
    fixture.detectChanges();

    const secondRow = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr')[1];
    expect(secondRow.textContent).toContain('—');
  });

  it('re-queries with the chosen status when the filter changes', async () => {
    httpMock.expectOne((r) => r.url === '/api/work-orders').flush(page(rows));
    await fixture.whenStable();
    fixture.detectChanges();

    const select = (fixture.nativeElement as HTMLElement).querySelector(
      '#status-filter',
    ) as HTMLSelectElement;
    select.value = 'OPEN';
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    const second = httpMock.expectOne((r) => r.url === '/api/work-orders');
    expect(second.request.params.get('status')).toBe('OPEN');
    second.flush(page([rows[1]]));

    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(1);
  });

  it('tells the user when nothing matches the filter', async () => {
    httpMock.expectOne((r) => r.url === '/api/work-orders').flush(page([]));
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'No work orders match this filter',
    );
  });

  it('shows an error message when the request fails', async () => {
    httpMock
      .expectOne((r) => r.url === '/api/work-orders')
      .flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Could not load work orders');
  });
});
