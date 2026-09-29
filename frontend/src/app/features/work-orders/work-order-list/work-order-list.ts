import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { WorkOrderService } from '../../../core/work-order.service';
import { WORK_ORDER_STATUSES, WorkOrderStatus, WorkOrderSummary } from '../../../core/models';

@Component({
  selector: 'app-work-order-list',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './work-order-list.html',
  styleUrl: './work-order-list.css',
})
export class WorkOrderList {
  private readonly service = inject(WorkOrderService);

  protected readonly statuses = WORK_ORDER_STATUSES;
  protected readonly workOrders = signal<WorkOrderSummary[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedStatus = signal<WorkOrderStatus | ''>('');
  protected readonly totalElements = signal(0);

  constructor() {
    this.load();
  }

  protected onStatusChange(value: string): void {
    this.selectedStatus.set(value as WorkOrderStatus | '');
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);

    const status = this.selectedStatus() === '' ? null : this.selectedStatus();

    this.service.list(status as WorkOrderStatus | null).subscribe({
      next: (page) => {
        this.workOrders.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load work orders. Is the backend running?');
        this.loading.set(false);
      },
    });
  }
}
