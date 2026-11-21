import { Router } from 'express';
import { ReservationController } from '../controllers/reservationController.js';

export function reservationRoutes(controller: ReservationController): Router {
  const router = Router();
  router.get('/', controller.list);
  router.post('/', controller.create);
  router.delete('/:id', controller.delete);
  return router;
}
