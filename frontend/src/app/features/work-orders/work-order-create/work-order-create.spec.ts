import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { WorkOrderCreate } from './work-order-create';
import { Asset, Technician } from '../../../core/models';

describe('WorkOrderCreate', () => {
  let fixture: ComponentFixture<WorkOrderCreate>;
  let httpMock: HttpTestingController;

  const assets: Asset[] = [
    {
      id: 10,
      tag: 'PMP-001',
      name: 'Crude transfer pump',
      manufacturer: 'Sulzer',
      model: 'MSD',
      siteId: 1,
      siteName: 'Bakken North',
    },
  ];

  const technicians: Technician[] = [
    { id: 20, employeeNo: 'T-1001', fullName: 'Dana Okoro', email: null, specialty: 'Rotating' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkOrderCreate],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        // A stub target so the post-create navigate() resolves instead of rejecting.
        provideRouter([
          { path: 'work-orders', children: [] },
          { path: 'work-orders/:id', children: [] },
        ]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkOrderCreate);
    fixture.detectChanges();
    httpMock = TestBed.inject(HttpTestingController);

    httpMock.expectOne('/api/assets').flush(assets);
    httpMock.expectOne('/api/technicians').flush(technicians);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  /** Reaches the protected form the same way the template does. */
  function form() {
    return (fixture.componentInstance as unknown as { form: any }).form;
  }

  it('starts invalid because title and asset are required', () => {
    expect(form().invalid).toBe(true);
  });

  it('defaults the priority to MEDIUM', () => {
    expect(form().controls.priority.value).toBe('MEDIUM');
  });

  it('does not submit while the form is invalid', () => {
    const button = (fixture.nativeElement as HTMLElement).querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
    button.click();

    httpMock.expectNone('/api/work-orders');
  });

  it('shows the required-field messages after an invalid submit', async () => {
    const button = (fixture.nativeElement as HTMLElement).querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
    button.click();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('A title is required.');
    expect(text).toContain('Pick the asset this work order is against.');
  });

  it('becomes valid once title and asset are filled in', () => {
    form().patchValue({ title: 'Seal leak', assetId: '10' });

    expect(form().valid).toBe(true);
  });

  it('posts the form as a create request, normalising the empty optional fields', async () => {
    form().patchValue({ title: 'Seal leak', assetId: '10', priority: 'HIGH' });
    fixture.detectChanges();

    const button = (fixture.nativeElement as HTMLElement).querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
    button.click();
    await fixture.whenStable();

    const req = httpMock.expectOne('/api/work-orders');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      title: 'Seal leak',
      description: null,
      assetId: 10,
      technicianId: null,
      priority: 'HIGH',
    });
    req.flush({ id: 1 });
  });

  it('shows the backend message when the create is rejected', async () => {
    form().patchValue({ title: 'Seal leak', assetId: '10' });
    fixture.detectChanges();

    (
      (fixture.nativeElement as HTMLElement).querySelector(
        'button[type="submit"]',
      ) as HTMLButtonElement
    ).click();
    await fixture.whenStable();

    httpMock
      .expectOne('/api/work-orders')
      .flush({ message: 'Asset 10 was not found' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Asset 10 was not found');
  });
});
