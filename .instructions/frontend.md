# React frontend instructions

Use React, TypeScript, Vite and MUI. Use a shared MUI theme and reusable HR interaction components; avoid adding another component framework. React Router handles routes and TanStack Query handles server state when needed. Keep local UI state local. Pin dependencies and commit the lockfile.

Make an authenticated conversational workspace the primary landing experience. Start with suggested role-appropriate questions, response status and typed result cards. Build the smallest review surfaces required by the active slice: absent-today list, expiring contracts, leave preview, payroll-variable table, CNSS/CIMR breakdown and exception queue. Forms support corrections and accessible fallback; do not force routine tasks through menu navigation first.

Show the stages Ask -> Analyze -> Calculate -> Prepare -> Confirm -> Execute as user-visible task status where useful, without exposing hidden model reasoning. Display source inputs and calculation breakdowns instead. A conversational request may lead to a table or form for efficient review. Do not fake completed features or provider results.

Use typed API contracts. Show loading, empty, validation, error, saving and completed states. Distinguish submitted, approved, rejected and cancelled. Never optimistically display an approval or final balance before the server confirms it. Preserve recoverable user input on failure.

Use Keycloak OIDC authorization code flow with PKCE through a maintained client integration. Follow .instructions/reference/security.md for token handling. Frontend route guards improve navigation; the backend remains authoritative. Never trust a selected company until the backend validates membership.

Support French and Arabic translation keys, RTL layout, keyboard navigation, visible labels/focus and mixed-direction identifiers. Use text with status colors. Keep leave dates as date-only values across API boundaries; do not shift them by converting to UTC timestamps.

Policy answers show citations; operational answers show authorized record provenance and as-of time. AI proposals show exact dates, type, unit and calculated impact. Payroll and social-contribution results show period, employee/employer portions, reviewed rule version, exclusions and provisional/final status. Financial cards are permission-gated. Confirmation is a deliberate control linked to the persisted proposal. Show unavailable evidence and provider errors honestly. Avoid unrestricted HTML rendering of model or document text.

Test forms and error states with Vitest/React Testing Library; test complete employee and manager journeys with Playwright as screens become available. Check RTL, keyboard operation and mobile request/approval flows. Do not duplicate legal calculations in frontend code. Measure task completion and elapsed interaction time using non-sensitive event metadata.
