# Current API reference

Reviewed 2026-11-01 against controllers, services, models, security and tests. This describes current behavior, including defects. No payment endpoints currently exist.

## Contract and authentication

OpenAPI: `/v3/api-docs` (JSON), `/v3/api-docs.yaml` (YAML). Swagger: `/swagger-ui/index.html`. `OpenApi30Config` enriches operation security, response statuses and wire types and consolidates equivalent path templates. Category lookups have distinct ID and name routes.

Paths below are relative to the server. Bodies use JSON. Protected routes use `Authorization: Bearer <accessToken>`. Refresh uses the HttpOnly `refreshToken` cookie, scoped to `/user`. Cookie defaults are Secure=true, SameSite=None; cross-origin browser requests need credentials and an allowed origin. Configure local HTTPS/HTTP cookie behavior appropriately.

Access: **Public** = no bearer; **JWT** = any authenticated user; **A/M** = ADMIN or MANAGER; **Staff** = ADMIN, WAITER, MANAGER or KITCHEN. Roles are stored as `ROLE_*`; ADMIN does not inherit WAITER. These rules describe the normal profile, not `local-dev` HTTP Basic.

Listed statuses are explicit controller outcomes, not exhaustive infrastructure errors. Protected endpoints can return 401/403; malformed JSON/UUID/parameters return 400. Errors use `{"code":"...","traceId":"..."}`. Missing resources return 404, stale writes/invalid transitions 409, and database availability failures 503. The trace filter adds `X-Trace-Id` to requests passing through it.

## Authentication and users

| Method/path | Access | Body/result | Explicit statuses |
|---|---|---|---|
| POST `/user/signin` | Public | LoginRequest → accessToken, username (email), roles; sets refresh cookie | 200, 401 |
| POST `/user/signup` | Public | RegisterRequest → UserResponse with ROLE_GUEST | 201, 409 |
| POST `/user/refresh` | Refresh cookie | No body → accessToken, userId | 200, 401, 404 |
| POST `/user/signout` | JWT + CSRF header | Invalidates access token and refresh family, clears cookie | 200, 401, 403 |
| GET `/user/me` | JWT | Current UserResponse | 200, 404 |
| GET `/user/all` | ADMIN | UserResponse[] | 200 |
| PUT `/user/update` | ADMIN | UpdateUserRequest → user | 200, 404 |
| DELETE `/user/{username}` | ADMIN | username means email; absent user is a no-op | 200, empty body |

LoginRequest: email/password. RegisterRequest: email (6–45 chars/email format), password (5–25/nonblank), optional phoneNumber. UpdateUserRequest: email (lookup key), phoneNumber, roles. User responses omit passwords and provider credentials. Signup maps duplicate-key failures to 409; database availability errors reach the common 503 handler. Google OAuth2 redirects are managed separately by Spring Security.

Refresh tokens rotate on each successful POST `/user/refresh`; retain the replacement cookie. Reusing a consumed refresh token rejects the request and revokes further refreshes in that family. Existing access tokens remain valid until their own expiry or explicit logout revocation. Revocation records store credential hashes with full token expiry, and checks use MongoDB across instances. Refresh tokens cannot authenticate bearer requests.

Refresh and logout require `X-CSRF-Protection: 1`. Browsers must use an allowed Origin and credentials-enabled requests; CORS permits the custom header. Nonbrowser clients must also send this header. The refresh cookie is HttpOnly, scoped to `/user`, and uses configured Secure/SameSite settings. Logout is now POST; GET logout no longer changes state. Tokens issued before refresh families were introduced require a fresh sign-in.

Authentication POSTs, guest order lookup and public order creation have separate per-client-IP limits of 60 requests per minute, per application instance. Configure `zito.app.limits.requests-per-minute` and `zito.app.limits.max-body-bytes` (default 65536). Excess requests return 429 with `Retry-After`; oversized payloads return 413, including bodies without a Content-Length header. Client IP is the servlet remote address; forwarded headers are not trusted by this filter. Deployments behind a proxy share its address unless trusted proxy handling is configured, and multi-instance global limits require an ingress limiter.

## Menu and categories

| Method/path | Access | Body/result | Explicit statuses |
|---|---|---|---|
| GET `/api/v1/menu-items` | Public | MenuItem[] | 200 |
| GET `/api/v1/menu-items/{id}` | JWT | Menu item by string ID | 200, 404 |
| POST `/api/v1/menu-items` | A/M | **Array** of MenuItem → saved array | 201, 500 |
| PUT `/api/v1/menu-items/{name}` | A/M | name actually means ID; body ID must match | 200, 404 |
| DELETE `/api/v1/menu-items/{id}` | A/M | Delete ID | 204 |
| GET `/api/v1/categories` | Public | Category[] | 200 |
| GET `/api/v1/categories/{id}` | JWT | Look up by integer ID | 200, 404 |
| GET `/api/v1/categories/by-name/{name}` | JWT | Look up by name | 200, 404 |
| POST `/api/v1/categories` | A/M | Category → saved category | 201, 500 |
| PUT `/api/v1/categories/{name}` | A/M | Body name must match lookup name | 200, 404 |
| DELETE `/api/v1/categories/{id}` | A/M | Delete integer ID | 204 |

Category name lookups must migrate from `/api/v1/categories/{name}` to `/api/v1/categories/by-name/{name}`. The single-segment route is reserved for integer IDs.

OpenAPI consolidates equivalent templates under `{id}`; PUT categories still accepts a **name** in that segment. This changes documentation only, not runtime routing.

MenuItem: id, name, description, categoryName, categoryId, itemModifiers, netPrice, grossPrice, taxRate, available, imageUrl. Decimal outputs are strings. `grossPrice` takes precedence for ordering; netPrice is the fallback basis. VAT rates are percentages (`"24"`, not `"0.24"`). Availability is not checked when ordering. PUT persists all listed catalog fields with a matching body ID.

Category: integer id, name, description, sortOrder. PUT requires matching name and changes description/name only, so cannot rename or change sortOrder.

## Orders

| Method/path | Access | Body/result | Explicit statuses |
|---|---|---|---|
| POST `/api/v1/orders` | Public | CreateOrderRequest → OrderResponse, including a one-time `guestCredential` | 201, 400 |
| GET `/api/v1/orders/{orderId}` | Public | UUID orderId; `X-Order-Credential` header | 200, 404 |
| GET `/api/v1/orders` | Staff | All orders, unpaginated | 200 |
| PATCH `/api/v1/orders/{orderId}/status` | Staff (kitchen: PREPARING/READY only) | `{"version":0,"status":"CONFIRMED"}` → order | 200, 400, 403, 404, 409, 503 |
| GET `/api/v1/orders/waiter` | WAITER | Current waiter's orders | 200, 404 if empty |
| POST `/api/v1/orders/waiter` | ADMIN, MANAGER or WAITER | CreateOrderRequest → order | 201, 400 |
| PUT `/api/v1/orders/waiter/{orderId}` | A/M | CreateOrderRequest with current version → replaced lines/totals | 201, 400, 403, 404, 409, 503 |
| POST `/api/v1/orders/{orderId}/cancellation` | A/M | `{"version":0,"reason":"Customer request"}` → retained canceled order | 200, 400, 404, 409, 503 |
| DELETE `/api/v1/orders/waiter/{orderId}` | A/M | Deprecated cancellation alias; requires version and reason query parameters; retains document | 204, 400, 404, 409, 503 |
| GET `/api/v1/orders/order-point/{orderPointId}` | Staff | Orders for string order point | 200, 404 if empty |

Guest clients must save the credential returned at creation and send it in `X-Order-Credential` for retrieval. Only its hash is stored. Existing guest orders have no credential and cannot be retrieved through this endpoint; migration is deferred. ADMIN/MANAGER can edit unpaid orders in NEW/CONFIRMED regardless of their creator. ADMIN/MANAGER cancellation records the actor and reason; paid, completed and already-canceled orders reject cancellation with 409. Orders are never physically deleted through these endpoints. Refund endpoints remain unimplemented; future refund authorization is restricted to ADMIN/MANAGER.

Order responses now include `version`. Send the current version in PUT order bodies and PATCH status bodies. Missing/negative versions return 400; stale versions and invalid transitions return 409. Legacy orders without a version require an explicit migration before edits. Status transitions follow NEW → CONFIRMED → PREPARING → READY → COMPLETED; kitchen staff may only advance to PREPARING or READY. Paid orders cannot have their lines replaced. Versioned mutations and validation failures use an error body containing `code` and `traceId`; database dependency errors reaching the common handler return 503.

Current statuses: NEW, CONFIRMED, PREPARING, READY, COMPLETED, CANCELED. Normal status updates advance one step and cannot reopen terminal orders. Cancellation uses its dedicated operation. `paid` starts false and no endpoint settles an order. Completion does not imply payment.

### Create example

```json
{
  "cartId": "8fb44e6f-19cb-4ab3-9cb8-a87398c21496",
  "orderPointId": "table-4",
  "cartItems": [
    {"menuItemId": "existing-menu-id", "quantity": 2, "note": "No onion", "modifiers": [], "itemDiscount": 0}
  ],
  "guestEmail": "guest@example.com",
  "discountPercent": "0"
}
```

Replace menu ID with a persisted item. cartId is client-generated and required but **not** an idempotency key; retrying POST creates duplicates. orderPointId is required but not validated against tables. Cart items are required and validated recursively, including quantity bounds.

Accepted currency/customerNote fields are ignored. Item note is copied. Staff creation also ignores cartId/guestEmail. Guest item/order discounts must be zero/absent; staff may supply 0–100%. itemDiscount is an integer percentage; discountPercent is decimal. Modifiers accept id/name/netPrice/grossPrice/modifierGroupId; server replaces netPrice from catalog, but unknown IDs become free and other client metadata is retained.

### Financial and wire semantics

- All BigDecimals serialize as **strings**. Dates serialize as **epoch milliseconds** with the current plain Jackson ObjectMapper.
- Base gross × quantity is calculated first. Modifier gross amounts are added **once per line**, not per unit; clarify/fix before charging.
- Item discount applies to gross, HALF_UP to two decimals. Net = gross / (1 + rate/100), rounded to two decimals; tax = gross − net.
- Order percentage discounts allocate proportionally across line gross values; last line receives rounding remainder. Net/tax are recalculated and summed by rate. Remainder allocation can over-discount a small/zero final line. Calculation mutates lines, so a repeat call can discount twice.
- OrderResponse maps id/cartId/orderPointId/waiterId/createdAt/latestUpdate/grossTotal/netTotal/taxTotals/orderItems/paid/status. Currency, discount, tip and guest contact fields remain null. Item unitPrice/tax read snapshot fields never populated by creation; lineTotal is discounted line gross. Item discount is percentage, not monetary allocation.
- Creation does not save discountTotal; updates do not synchronize orderDiscount. Receipt-ready discount breakdowns are incomplete.

No jurisdiction/effective-date/fulfillment tax policy, payment balance, refunds or finalized receipts exist.

## Tables and branding

| Method/path | Access | Body/result | Explicit statuses |
|---|---|---|---|
| GET `/api/v1/table` | Staff | Table[] | 200 |
| GET `/api/v1/table/{id}` | Staff | Table by integer ID | 200, 404 |
| POST `/api/v1/table` | A/M | Table → saved table | 201 |
| PUT `/api/v1/table/{id}` | A/M | Saves body ID without checking path equality | 200, 404 |
| DELETE `/api/v1/table/{id}` | A/M | Delete | 204, 404 |
| GET `/api/v1/table/layout` | Staff | TableLayout[] | 200 |
| POST `/api/v1/table/layout` | A/M | Layout → saved layout | 201, 400, 503 |
| DELETE `/api/v1/table/layout/{id}` | A/M | Delete string layout ID | 204, 404 |
| GET `/api/v1/restaurants/{restaurantId}/branding` | Public | Branding; unknown ID gets sample fallback | 200 |

Table: id, orders (raw persisted orders), status FREE/OCCUPIED/RESERVED. Layout: id, tableNumber, coordx, coordy, chairs. Branding: id/name/logoUrl/faviconUrl/description/lightTheme/darkTheme. No table-session/bill model exists. Operational orders/menu/tables have no restaurant scope; branding IDs do not provide tenant isolation.

## Updates

Native WebSocket `/ws` is blocked by the normal security profile until step 4 supplies CONNECT/SUBSCRIBE and delivery authorization. Use credential-protected REST polling for guest updates. The configured broker prefix is `/topic`, application prefix `/app`; the following destinations describe internal publication, not publicly available subscriptions. Payload is OrderResponse.

| Topic | Purpose |
|---|---|
| `/topic/orders/{orderId}` | Single order |
| `/topic/tables/{orderPointId}` | Order-point orders |
| `/topic/orders` | Global kitchen/waiter dashboard |

Only staff creation and status changes emit events; guest creation, line edits and deletion do not. There are no event IDs, versions, replay, durable delivery or subscription authorization. Handshake is public with wildcard origins. Topic names are not access controls; secure subscriptions before exposing real customer data. The in-memory broker has no cross-instance propagation. Clients currently need REST re-fetch on reconnect and polling for omitted changes.

## Maintaining the contract

Keep controllers/models, OpenApi30Config and this document synchronized. Under JDK 21 run OpenAPI regression and HTTP contract tests, export `/v3/api-docs` and validate it after resolving springdoc compatibility. Check public security overrides, cookie auth, parameters, response statuses, decimal strings, timestamps and unique templates. Direct Java controller tests alone do not verify HTTP security, validation or serialization.
