import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { WorkOrderService } from '../../../core/work-order.service';
import { ApiError, Asset, PRIORITIES, Priority, Technician } from '../../../core/models';

@Component({
  selector: 'app-work-order-create',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './work-order-create.html',
  styleUrl: './work-order-create.css',
})
export class WorkOrderCreate implements OnInit {
  private readonly service = inject(WorkOrderService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly priorities = PRIORITIES;
  protected readonly assets = signal<Asset[]>([]);
  protected readonly technicians = signal<Technician[]>([]);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(4000)]],
    assetId: ['', [Validators.required]],
    technicianId: [''],
    priority: ['MEDIUM' as Priority, [Validators.required]],
  });

  ngOnInit(): void {
    this.service.assets().subscribe({ next: (list) => this.assets.set(list) });
    this.service.technicians().subscribe({ next: (list) => this.technicians.set(list) });
  }

  protected get title() {
    return this.form.controls.title;
  }

  protected get assetId() {
    return this.form.controls.assetId;
  }

  protected submit(): void {
    if (this.form.invalid) {
      // Surface the messages for fields the user never touched.
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();
    this.service
      .create({
        title: value.title,
        description: value.description || null,
        assetId: Number(value.assetId),
        technicianId: value.technicianId ? Number(value.technicianId) : null,
        priority: value.priority,
      })
      .subscribe({
        next: (created) => {
          this.submitting.set(false);
          void this.router.navigate(['/work-orders', created.id]);
        },
        error: (response: { error?: ApiError }) => {
          this.submitting.set(false);
          this.error.set(response.error?.message ?? 'Could not create the work order.');
        },
      });
  }
}
