# Live Anonymous Reporting integration testing

Use `AnonymousReportingLiveIntegrationTest --live` only with explicit authorization to create one labeled integration report, two messages and five investigation snapshots in the configured real Supabase project. Test data is retained with case status Closed. No deletion/cleanup is attempted. Environment credentials (`AEPM_TEST_EMPLOYEE_EMAIL/PASSWORD`, `AEPM_TEST_ADMIN_EMAIL/PASSWORD`, `AEPM_TEST_MANAGER_EMAIL/PASSWORD`) are preferred; the runner can also read the existing local README account entries without echoing them. Do not commit real credentials.

## Verified deployed contract

The applied migration 002 in this workspace is the user's table-returning RPC revision, not the earlier JSON-returning proposal. Migration 002 is immutable after application. The Java repository now matches these signatures:

- Submit: p_report_category, p_subject, p_report_description, p_evidence_file. Returns one array row with a server-generated 48-hex-character secret reference_number. Only that employee submission receipt may contain plaintext. No hash is returned. Java does not generate a different unused reference.
- Track/history: p_reference_number. Invalid tracking references return an empty array, mapped to Optional.empty. Existing exact legacy values remain supported.
- Send: p_reference_number or p_report_id, plus p_message_text. Message rows omit report_id; the Java DTO uses the authorized request context (employee tracking resolves it first). Sender mappings and bigint ID precision are retained. Conversation RPCs mark the opposite party's messages read as defined by deployed 002.
- List: p_status, p_category, p_start_date, p_end_date, p_limit, p_offset. Returns rows with total_count per row. Empty pages at a nonzero offset perform a one-row count probe instead of inventing a zero total. Separate requests do not form a persistent snapshot.
- Save investigation: p_report_id, p_investigation_note, p_new_status, p_resolution. The server records the acting administrator. Java rejects requests to assign another administrator instead of silently ignoring the parameter. The compact save receipt is followed by history retrieval and matched by investigation_id to return the exact saved private snapshot. expectedStatus is checked against current detail before saving, but the deployed four-argument RPC has no atomic expected-status parameter.

All normal RPC results are arrays; the Java repository validates expected cardinality and maps empty arrays safely. Public service APIs remain stable. No UI/FXML/CSS changes are needed.

## Live defects and migration 003

`supabase/migrations/202611040003_anonymous_reporting_live_fixes.sql` was created after live testing found SQLSTATE 42883: unqualified digest() cannot be resolved by the deployed hash helper's search_path. A table-returning function's output variables also conflicted with UPDATE column names. The exact original functions reproduced 42702 in an isolated PostgreSQL engine for employee/admin conversation reads and investigation saves.

003 changes functions/grants only:

1. Hash helper uses schema-qualified PostgreSQL built-in SHA-256/UTF-8/hex functions. Existing stored hashes remain compatible; no references or report rows change.
2. Current-role helper requires an active profile; employee/admin guards use IS DISTINCT FROM and SQLSTATE 42501, rejecting missing/inactive/null profiles.
3. Qualify report_id/resolution in investigation UPDATE and sender_type/is_read/report_id in the message-read UPDATEs. Preserve table-return signatures, read receipts and investigation history.
4. Keep helpers unavailable to normal clients, preserve authenticated business-RPC execution and revoke PUBLIC/anon execution. Reload PostgREST schema cache.

The user confirmed applying 003. The following full LIVE run passed all 36 requested tests; see `anonymous-reporting-live-integration-results.md` for the timestamp, retained report ID and individual results. No privilege/owner token was used by the runner.

## Commands

```powershell
mvn clean compile
mvn test-compile dependency:build-classpath '-Dmdep.outputFile=target/anonymous-test-classpath.txt'
$anonymousClasspath = 'target/test-classes;target/classes;' + (Get-Content target/anonymous-test-classpath.txt -Raw).Trim()
java -cp $anonymousClasspath com.aepm.logic.anonymous.AnonymousReportingSmokeTest
# Read-only route/permission diagnostics, no submitted report:
java -cp $anonymousClasspath com.aepm.logic.anonymous.AnonymousReportingLiveIntegrationTest --diagnose
# LIVE writes: creates and closes one explicitly labeled test report; run only when authorized:
java -cp $anonymousClasspath com.aepm.logic.anonymous.AnonymousReportingLiveIntegrationTest --live
```

Isolated SQL regression (no live connection, test-only fixtures):

```powershell
$anonymousSqlTools = Join-Path $env:TEMP 'aepm-anonymous-sql-test-tools'
node supabase/tests/anonymous-reporting-live-fixes.test.mjs $anonymousSqlTools
```

The regression runner requires the separately installed @electric-sql/pglite test engine in that temporary directory. It recreates the final table shapes, loads the exact 002 function bodies, reproduces 42883/42702, applies 003, verifies hash compatibility/workflow/history and checks missing/null profile denial. No project runtime dependency is added.

## Scope and remaining risks

- Raw observed business-RPC responses are inspected before Java mapping, including nested fields and actual secret/hash values. The employee's one-time submission receipt is explicitly allowed; administrator responses, tracking and chat may not expose references/hashes or submitter identity fields.
- Normal account JWTs cannot audit hidden table columns or arbitrary database routines. Physical identity-storage guarantees and unknown extra endpoints require the owner-side catalog/SQL verification; passing response tests alone is not that privileged audit. The earlier verification/alignment scripts target the earlier proposed 002 layout and are historical, not a substitute for auditing this applied revision.
- Login credentials already present in README remain a repository risk. Move them to environment/secrets and rotate if shared/committed. This task does not copy credentials, tokens, raw responses or secret references into files/output.
- References are bearer secrets; user-written report/evidence text and infrastructure authentication/access logs can identify a reporter outside the application's field-level anonymity.
- The deployed investigation RPC lacks an atomic expected-status argument; Java's preflight check does not eliminate all concurrent same-status edits. Database row locking still enforces allowed transitions.
- Deployed 002 allows same-status Closed investigation edits and chat after closure through direct RPC calls; the 36 requested tests do not assert post-closure immutability. Java investigation validation rejects Closed edits, but further server-side workflow tightening would need a separate reviewed migration, not changes to applied 002/003.

Existing migrations and UI remain unchanged. Only Maven manages target build outputs.
