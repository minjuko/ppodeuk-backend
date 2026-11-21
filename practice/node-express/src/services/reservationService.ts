import { HttpError } from '../errors.js';
import { ReservationRepository } from '../repositories/reservationRepository.js';
import type { CreateReservationInput, Reservation } from '../types.js';

const LOCAL_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/;

function parseLocalDateTime(value: string, field: string): number {
  if (!LOCAL_DATE_TIME.test(value)) throw new HttpError(400, `${field} must use YYYY-MM-DDTHH:mm format`);
  const date = new Date(`${value}:00Z`);
  if (Number.isNaN(date.getTime()) || date.toISOString().slice(0, 16) !== value) {
    throw new HttpError(400, `${field} is not a valid date and time`);
  }
  return date.getTime();
}

export class ReservationService {
  constructor(private readonly repository: ReservationRepository) {}

  list(): Promise<Reservation[]> {
    return this.repository.findAll();
  }

  async create(input: unknown): Promise<Reservation> {
    if (typeof input !== 'object' || input === null || Array.isArray(input)) {
      throw new HttpError(400, 'Request body must be an object');
    }
    const body = input as Partial<CreateReservationInput>;
    if (!Number.isSafeInteger(body.bayId) || (body.bayId as number) <= 0) {
      throw new HttpError(400, 'bayId must be a positive integer');
    }
    if (typeof body.startTime !== 'string' || typeof body.endTime !== 'string') {
      throw new HttpError(400, 'startTime and endTime are required');
    }

    const start = parseLocalDateTime(body.startTime, 'startTime');
    const end = parseLocalDateTime(body.endTime, 'endTime');
    if (end <= start) throw new HttpError(400, 'endTime must be after startTime');
    if (new Date(start).getUTCMinutes() % 30 !== 0 || new Date(end).getUTCMinutes() % 30 !== 0) {
      throw new HttpError(400, 'Reservation times must use 30-minute increments');
    }

    const reservations = await this.repository.findAll();
    const overlaps = reservations.some((reservation) => {
      if (reservation.bayId !== body.bayId) return false;
      const existingStart = parseLocalDateTime(reservation.startTime, 'stored startTime');
      const existingEnd = parseLocalDateTime(reservation.endTime, 'stored endTime');
      return start < existingEnd && end > existingStart;
    });
    if (overlaps) throw new HttpError(409, 'Reservation overlaps an existing reservation for this bay');

    return this.repository.save({
      bayId: body.bayId as number,
      startTime: body.startTime,
      endTime: body.endTime,
    });
  }

  async delete(id: number): Promise<void> {
    if (!Number.isSafeInteger(id) || id <= 0) throw new HttpError(400, 'id must be a positive integer');
    if (!(await this.repository.deleteById(id))) throw new HttpError(404, 'Reservation not found');
  }
}
