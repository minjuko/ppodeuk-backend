import express, { type ErrorRequestHandler } from 'express';
import { ReservationController } from './controllers/reservationController.js';
import { HttpError } from './errors.js';
import { ReservationRepository } from './repositories/reservationRepository.js';
import { reservationRoutes } from './routes/reservationRoutes.js';
import { ReservationService } from './services/reservationService.js';

export function createApp() {
  const app = express();
  const service = new ReservationService(new ReservationRepository());
  app.use(express.json());
  app.use('/reservations', reservationRoutes(new ReservationController(service)));
  const errors: ErrorRequestHandler = (error: unknown, _request, response, _next) => {
    if (error instanceof HttpError) {
      response.status(error.status).json({ error: error.message });
      return;
    }
    if (typeof error === 'object' && error !== null && 'status' in error && error.status === 400) {
      response.status(400).json({ error: 'Invalid JSON request body' });
      return;
    }
    response.status(500).json({ error: 'Internal server error' });
  };
  app.use(errors);
  return app;
}
