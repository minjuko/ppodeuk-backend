# KakaoPay test payment on localhost

The `local-external` Spring profile runs the real KakaoPay payment flow while
keeping the local database, local file storage, and local frontend CORS
settings. The ordinary `local` profile intentionally uses the fake payment
service and never calls KakaoPay.

## Before starting

- Run MariaDB and prepare the local `bdbd` database as described in the local
  reproduction guide.
- Run the USER frontend at `http://localhost:5173` with
  `VITE_API_BASE_URL=http://localhost:8080` and `VITE_DEMO_MODE=false`.
- Use the KakaoPay development/test key that is already verified in the
  deployed environment. Do not save the key in this repository or a committed
  `.env` file.
- Use the same PC browser for the application and the KakaoPay PC payment
  bridge. A callback containing `localhost` cannot return from a different
  phone to the development PC.

## Start the backend

Create an untracked `Team10_BE/.env` file. The `local-external` profile loads
this file as Java properties; it is ignored by Git. Keep payment secrets out of
the frontend `.env` even when they do not use the `VITE_` prefix.

```dotenv
KAKAOPAY_ADMIN_KEY=<KakaoPay development secret key>
BDBD_LOCAL_DB_PASSWORD=<local MariaDB password>
BDBD_LOCAL_DB_USERNAME=bdbd
BDBD_LOCAL_DB_URL=jdbc:mariadb://127.0.0.1:3306/bdbd
LOCAL_JWT_SECRET=<local JWT secret, at least 32 bytes>
PAYMENT_APPROVAL_URL=http://localhost:5173/paymentwaiting
PAYMENT_CANCEL_URL=http://localhost:5173/paymentfail
PAYMENT_FAIL_URL=http://localhost:5173/paymentfail
```

Then start the backend:

```powershell
./gradlew.bat bootRun --args='--spring.profiles.active=local-external'
```

Despite its legacy environment-variable name, `KAKAOPAY_ADMIN_KEY` is sent by
the current integration as the KakaoPay development `SECRET_KEY` authorization
credential. Use the same value format that works on Railway; do not add the
`SECRET_KEY ` prefix to the variable value.

## Browser smoke test

1. Open `http://localhost:5173`, sign in, and select a bay and available time.
2. Continue to payment and click KakaoPay payment.
3. Confirm that the browser opens the KakaoPay test bridge.
4. Complete authentication in the test flow.
5. Confirm that the browser returns to
   `http://localhost:5173/paymentwaiting?pg_token=...`.
6. Click the final approval button and confirm the reservation result and
   reservation history.
7. Repeat once with cancellation and confirm the browser returns to
   `/paymentfail` without creating a reservation.

The successful callback supplies only `pg_token`. The frontend retains the
KakaoPay `tid` and reservation selection in local storage across the external
redirect, then sends them with `pg_token` to the backend approval endpoint.

## Troubleshooting

- A fake immediate callback means the backend was started with `local`, not
  `local-external`.
- `401` or an authentication error from the ready API usually means the key is
  missing, has the wrong type, or includes an extra authorization prefix.
- A callback that cannot open usually means the payment bridge was opened on a
  different device. For mobile-device testing, use an HTTPS tunnel URL for the
  frontend and set all three callback variables to that public origin.
- If the callback page says payment state is missing, keep one browser profile
  and origin throughout the flow and do not clear site storage between ready
  and approval.
