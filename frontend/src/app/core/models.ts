/** Mirrors the DTOs exposed by the Spring Boot API. */

export type WorkOrderStatus = 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'CLOSED';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export const WORK_ORDER_STATUSES: readonly WorkOrderStatus[] = [
  'OPEN',
  'ASSIGNED',
  'IN_PROGRESS',
  'CLOSED',
] as const;

export const PRIORITIES: readonly Priority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;

export interface Site {
  id: number;
  code: string;
  name: string;
  location: string | null;
}

export interface Asset {
  id: number;
  tag: string;
  name: string;
  manufacturer: string | null;
  model: string | null;
  siteId: number | null;
  siteName: string | null;
}

export interface Technician {
  id: number;
  employeeNo: string;
  fullName: string;
  email: string | null;
  specialty: string | null;
}

export interface Part {
  id: number;
  sku: string;
  name: string;
  quantityInStock: number;
}

export interface WorkOrderPartLine {
  partId: number;
  sku: string;
  partName: string;
  quantity: number;
}

export interface WorkOrderSummary {
  id: number;
  reference: string;
  title: string;
  status: WorkOrderStatus;
  priority: Priority;
  assetTag: string | null;
  technicianName: string | null;
  openedAt: string;
  closedAt: string | null;
}

export interface WorkOrderDetail {
  id: number;
  reference: string;
  title: string;
  description: string | null;
  status: WorkOrderStatus;
  priority: Priority;
  asset: Asset | null;
  technician: Technician | null;
  openedAt: string;
  closedAt: string | null;
  parts: WorkOrderPartLine[];
  allowedTransitions: WorkOrderStatus[];
  version: number | null;
}

export interface CreateWorkOrderRequest {
  title: string;
  description: string | null;
  assetId: number;
  technicianId: number | null;
  priority: Priority;
}

/** Spring Data page envelope, narrowed to what the list screen uses. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  role: 'ADMIN' | 'SUPERVISOR' | 'TECHNICIAN';
  expiresInSeconds: number;
}

/** The error body the backend returns for every failure. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  violations: { field: string; message: string }[];
}
