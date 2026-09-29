import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { WorkOrderService } from '../../../core/work-order.service';
import { Part, Technician, WorkOrderDetail as WorkOrder, WorkOrderStatus } from '../../../core/models';
import { ApiError } from '../../../core/models';

@Component({
  selector: 'app-work-order-detail',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './work-order-detail.html',
  styleUrl: './work-order-detail.css',
})
export class WorkOrderDetail implements OnInit {
  /** Bound from the :id route parameter. */
  readonly id = input.required<string>();

  private readonly service = inject(WorkOrderService);

  protected readonly workOrder = signal<WorkOrder | null>(null);
  protected readonly technicians = signal<Technician[]>([]);
  protected readonly parts = signal<Part[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected selectedTechnicianId = '';
  protected selectedPartId = '';
  protected partQuantity = 1;

  ngOnInit(): void {
    this.reload();
    this.service.technicians().subscribe({ next: (list) => this.technicians.set(list) });
    this.service.parts().subscribe({ next: (list) => this.parts.set(list) });
  }

  protected reload(): void {
    this.loading.set(true);
    this.service.get(Number(this.id())).subscribe({
      next: (detail) => {
        this.workOrder.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load this work order.');
        this.loading.set(false);
      },
    });
  }

  protected assign(): void {
    if (!this.selectedTechnicianId) {
      return;
    }
    this.apply(this.service.assignTechnician(Number(this.id()), Number(this.selectedTechnicianId)));
  }

  protected changeStatus(status: WorkOrderStatus): void {
    this.apply(this.service.changeStatus(Number(this.id()), status));
  }

  protected addPart(): void {
    if (!this.selectedPartId || this.partQuantity < 1) {
      return;
    }
    this.apply(this.service.addPart(Number(this.id()), Number(this.selectedPartId), this.partQuantity));
  }

  /** Applies a mutation, surfacing the backend's own message when it refuses. */
  private apply(request: Observable<WorkOrder>): void {
    this.error.set(null);
    request.subscribe({
      next: (detail) => this.workOrder.set(detail),
      error: (response: { error?: ApiError }) =>
        this.error.set(response.error?.message ?? 'The request was rejected.'),
    });
  }
}
