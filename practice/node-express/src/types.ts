export interface Reservation {
  id: number;
  bayId: number;
  startTime: string;
  endTime: string;
}

export interface CreateReservationInput {
  bayId: number;
  startTime: string;
  endTime: string;
}
