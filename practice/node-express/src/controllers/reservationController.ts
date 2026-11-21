import type { NextFunction, Request, Response } from 'express';
import { ReservationService } from '../services/reservationService.js';

export class ReservationController {
  constructor(private readonly service: ReservationService) {}

  list = async (_request: Request, response: Response, next: NextFunction): Promise<void> => {
    try {
      response.json(await this.service.list());
    } catch (error) { next(error); }
  };

  create = async (request: Request, response: Response, next: NextFunction): Promise<void> => {
    try {
      response.status(201).json(await this.service.create(request.body));
    } catch (error) { next(error); }
  };

  delete = async (request: Request, response: Response, next: NextFunction): Promise<void> => {
    try {
      const id = Number(request.params.id);
      await this.service.delete(id);
      response.status(204).send();
    } catch (error) { next(error); }
  };
}
