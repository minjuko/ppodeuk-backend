import request from 'supertest';
import { describe, expect, it } from 'vitest';
import { createApp } from './app.js';

describe('reservation API', () => {
  it('creates and lists a reservation', async () => {
    const app = createApp();
    const created = await request(app).post('/reservations').send({ bayId: 1, startTime: '2026-08-12T09:30', endTime: '2026-08-12T10:00' });
    expect(created.status).toBe(201);
    expect(created.body).toMatchObject({ id: 1, bayId: 1 });
    expect((await request(app).get('/reservations')).body).toHaveLength(1);
  });

  it('rejects invalid fields, malformed dates, reverse time, and non-30-minute increments', async () => {
    const app = createApp();
    const cases = [
      { bayId: 0, startTime: '2026-08-12T09:30', endTime: '2026-08-12T10:00' },
      { bayId: 1, startTime: '2026-02-30T09:30', endTime: '2026-02-30T10:00' },
      { bayId: 1, startTime: '2026-08-12T10:00', endTime: '2026-08-12T09:30' },
      { bayId: 1, startTime: '2026-08-12T09:15', endTime: '2026-08-12T10:00' },
    ];
    for (const body of cases) expect((await request(app).post('/reservations').send(body)).status).toBe(400);
    expect((await request(app).post('/reservations').set('Content-Type', 'application/json').send('{invalid')).status).toBe(400);
  });

  it('rejects same-bay overlap but permits touching boundaries and different bays', async () => {
    const app = createApp();
    const create = (bayId: number, startTime: string, endTime: string) => request(app).post('/reservations').send({ bayId, startTime, endTime });
    expect((await create(1, '2026-08-12T10:00', '2026-08-12T11:00')).status).toBe(201);
    expect((await create(1, '2026-08-12T10:30', '2026-08-12T11:30')).status).toBe(409);
    expect((await create(1, '2026-08-12T11:00', '2026-08-12T11:30')).status).toBe(201);
    expect((await create(2, '2026-08-12T10:00', '2026-08-12T11:00')).status).toBe(201);
  });

  it('deletes a reservation and returns 404 when it is missing', async () => {
    const app = createApp();
    await request(app).post('/reservations').send({ bayId: 1, startTime: '2026-08-12T09:30', endTime: '2026-08-12T10:00' });
    expect((await request(app).delete('/reservations/1')).status).toBe(204);
    expect((await request(app).delete('/reservations/1')).status).toBe(404);
  });
});
