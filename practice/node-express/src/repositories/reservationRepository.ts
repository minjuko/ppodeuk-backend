import type { Reservation } from '../types.js';

export class ReservationRepository {
  private readonly reservations: Reservation[] = [];
  private nextId = 1;

  async findAll(): Promise<Reservation[]> {
    return this.reservations.map((reservation) => ({ ...reservation }));
  }

  async save(reservation: Omit<Reservation, 'id'>): Promise<Reservation> {
    const saved = { ...reservation, id: this.nextId++ };
    this.reservations.push(saved);
    return { ...saved };
  }

  async deleteById(id: number): Promise<boolean> {
    const index = this.reservations.findIndex((reservation) => reservation.id === id);
    if (index < 0) return false;
    this.reservations.splice(index, 1);
    return true;
  }
}
