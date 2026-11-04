# Anonymous Reporting backend

The configured live project uses the user's table-returning revision of migration 002. Migration 003 was applied by the user during live integration testing. Both applied migrations must remain unchanged; do not rerun migrations 001 or 002 on this live database.

Current references:

- [Live testing and deployed API contract](anonymous-reporting-live-testing.md)
- [36 live test results, retained report ID and build evidence](anonymous-reporting-live-integration-results.md)
- `supabase/migrations/202611040003_anonymous_reporting_live_fixes.sql`
- `supabase/tests/anonymous-reporting-live-fixes.test.mjs`

The backend flow remains UI -> logic.anonymous services -> data.repository.AnonymousReportRepository -> authenticated Supabase REST RPCs. SupabaseClient, AppConfig and SessionManager are reused. UI/FXML/CSS are unchanged.

Submission returns a server-issued secret reference once to the employee. Other report/message responses contain no submitter identity, plaintext reference or hash. Tracking/chat use the exact reference without binding the report to an employee ID. Private investigation notes are administrator-only; a published resolution is available through tracking after resolution. Normal table access is denied. Employee/Admin role checks run in both services and the database; Managers and unauthenticated callers are denied.

Statuses: Submitted, Under Review, Under Investigation, Resolved, Closed. Database senders: Anonymous Reporter, Administrator. Investigation history is append-only through the deployed save RPC. Evidence remains an optional URL/path string, not an upload feature.

Earlier `anonymous-reporting-schema-alignment.md`, migration 001 and the earlier alignment test/verification scripts describe a historical proposal where they differ from the applied revision; they must not be used as the deployed RPC contract. Use the live-testing document above for current parameter names, array response shapes, reproduction commands and remaining risks.
