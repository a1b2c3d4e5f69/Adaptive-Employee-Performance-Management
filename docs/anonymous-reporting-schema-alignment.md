# Live Anonymous Reporting schema alignment

The live DDL supplied by the user is the source of truth for migration 002. No live Supabase connection or schema changes were made by this implementation. The isolated PostgreSQL tests recreate the supplied schema, seed test-only legacy records, apply the actual upgrade and run the actual verification SQL.

## Execution and data impact

1. Review and execute **only** `supabase/migrations/202611040002_anonymous_reporting_schema_alignment.sql` for these existing live tables. Do **not** execute migration 001 first: it is an immutable historical proposal that creates incompatible tables. Do not run a migration runner that blindly replays 001 against this project; reconcile its migration history/baseline first using your normal deployment process.
2. Take an access-controlled database backup and use a maintenance window. The migration locks all three module tables, with a 10-second lock timeout, and runs in a single transaction.
3. **The successful upgrade removes recoverable plaintext `reference_number`.** It first adds `reference_hash`, computes SHA-256 over the exact existing UTF-8 values (no trim, case change or token replacement), validates all hashes, applies NOT NULL/format/unique checks, and only then drops plaintext using RESTRICT. No CASCADE, table drops, row deletions, history collapse or ID resets occur. Existing reference strings continue to work. Administrators cannot recover lost references after this upgrade. Do not execute if you need plaintext recovery; review that requirement before deployment. Protected backups/history may still contain prior plaintext.
4. Unknown callable legacy routines whose source mentions the module tables/reference fields, exposed dependent views (including nested views), conflicting proposal-schema objects, unusable blank legacy references, invalid existing status/sender values, or dependencies preventing RESTRICT cause rollback. No unsafe routine/view is silently deleted or granted extra permissions. Review these dependencies and rerun only after resolving the cause. Static catalog checks cannot prove safety of arbitrary dynamic SQL or wrapper functions; review the deployed API inventory too.
5. Run `supabase/verification/202611040002_anonymous_reporting_verification.sql` in SQL Editor as the owner. It is read-only, raises on failed checks and displays definitions/privileges without report data, plaintext references or hashes. Also test real employee/admin/manager JWTs through PostgREST before enabling the module UI.

All reports, messages and investigation rows retain their IDs, content and submitted/created times. Existing reports receive subject `Legacy report (title not supplied)` because no subject was previously recorded; this is a migration label, not an employee-authored title. No resolution is fabricated for old Resolved/Closed cases. Existing report updated_at triggers, if present, can advance updated_at during hash/subject backfill; submitted_at is not changed. Investigation updated_at is backfilled from created_at.

## Exact mismatches and decisions

| Existing Java/proposal assumption | Supplied live schema | Alignment |
|---|---|---|
| Report `category`, `description`, `evidence_path`, `status` | `report_category`, `report_description`, `evidence_file`, `case_status` | Keep live names; repository explicitly maps them to domain record fields |
| Report `subject` and `resolution` already present | Neither exists | Add `subject varchar(200) NOT NULL` and nullable `resolution text`; new submissions require a real subject |
| Hash in private `report_reference` mapping; no plaintext report column | Unique, required `reference_number varchar(50)` directly in report | Add/validate unique SHA-256 `reference_hash` in the existing report; remove plaintext only after conversion; all client table access denied |
| Java/proposal only accepts newly generated 43-character references | Historical varchar(50) reference format is unspecified | Tracking/chat accept exact nonblank legacy values up to 50 Unicode characters; no trimming. New submissions still use 32 SecureRandom bytes / 43 URL-safe Base64 characters |
| `Investigating` | `Under Investigation` | Keep Java constant INVESTIGATING for compatibility, change its database/display value to `Under Investigation` |
| `anonymous_report_message_tbl` | `anonymous_message_tbl` | Use the live table |
| Message UUID ID | bigint identity message_id | Map to Java long and parse numerically without floating-point conversion |
| Message `content`; enum-named database sender | `message_text`; `Anonymous Reporter` / `Administrator` | Explicit live-column/sender mapping; logical Java enum names remain stable |
| No `sender_admin_id` or `is_read` in proposal | Nullable admin FK and is_read exist | Preserve both; admin sends record auth.uid internally, employee sends NULL; safe DTO exposes is_read but omits sender_admin_id. Retrieval does not silently mark messages read |
| `anonymous_investigation_tbl`, one row per report, upsert/report_id PK | `investigation_tbl`, bigint investigation_id PK, many rows per report | Preserve every historical row and append a snapshot for each save; add administrator-only history RPC/service method |
| `notes`, `assigned_administrator`, `status` | Nullable `investigation_note`, `administrator_id`, `previous_status`, `new_status` | Reuse live fields; administrator_id is the responsible/assigned administrator and remains nullable; current case status stays in report.case_status |
| Investigation assignee FK to auth.users | FK to user_tbl | Keep the existing user_tbl FK; RPC independently requires an active administrator assignee |
| No history ID, previous status or created time in DTO | Those history fields exist | Extend the investigation DTO; preserve nullable historical note/status fields rather than inventing values |
| Investigation resolution/updated_at already present | Neither exists | Add nullable `resolution text` and updated_at, backfilled from created_at; no duplicate status or note column |
| Explicit updated_at assignments only | No automatic update behavior was provided | Add BEFORE UPDATE triggers on report and investigation so all future status/resolution/record updates touch updated_at |
| Admin direct SELECT and whole-row to_jsonb serialization | New hash on report would leak through SELECT/to_jsonb | RPC-only table/column access, restrictive RLS barriers, explicit JSON allowlists for every RPC |
| Original migration CREATE TABLE statements | Tables already exist | New ALTER migration 002; original 001 is unchanged and must not be run on this live schema |

`AnonymousReport.java` keeps its safe domain names and never gains reference/identity fields; the repository adapts physical names. `AnonymousReportSubmission.java` still returns the generated plaintext receipt only to the submitter and redacts it in toString. `AnonymousReportService`, `AnonymousReportTrackingService` and `AnonymousReportChatService` retain their public APIs and secure generation/authorization; the changed repository and shared reference validator supply compatibility. `AnonymousReportPage` pagination remains unchanged. `AnonymousInvestigationService` adds history retrieval and retains the save API, now backed by append-only database history. Existing 3-layer flow and SupabaseClient/AppConfig/SessionManager reuse are preserved.

## Final workflow, records and privacy

Statuses: **Submitted -> Under Review -> Under Investigation -> Resolved -> Closed**. These exact live values are used in constraints, Java mappings and RPC filters. Same-status note changes are allowed before Closed. Skips, backwards transitions, stale expected status, blank resolution on resolve/close and updates/messages after Closed are rejected. Report locks serialize chat/status updates. The expected-status check detects concurrent workflow changes, but is not a full revision check for same-status notes. Every save appends history instead of overwriting an old note.

Database message sender values: **Anonymous Reporter**, **Administrator**. The server sets them; clients cannot pass sender_admin_id or spoof a sender type. The sender-admin FK is never included in conversation output. Message IDs remain numeric identities and histories use sent_at/message_id ordering.

Investigation records retain investigation_id, report_id, administrator_id, investigation_note, previous_status, new_status, created_at and add resolution/updated_at. Latest detail orders by created_at DESC, investigation_id DESC; history orders ascending. Historical NULL audit statuses stay NULL. Report.case_status is the canonical current state. Employee tracking never calls an investigation RPC. Draft resolution remains in private investigation snapshots; report.resolution is published only when Resolved/Closed, and safe report projection also gates it by status.

New references have 256 bits of randomness. Legacy short/predictable references retain their original strength: hashing storage does not make them harder to guess. They are not silently regenerated because that would invalidate employee receipts. Any rotation/reissue policy must be a separate reviewed operation. Reference lookup is exact SHA-256 over UTF-8, computed inside the guarded RPC path. Only hashes are stored after successful alignment. Application administrators cannot SELECT the hash or obtain it through normal RPCs; database owners/service-role operators inherently retain privileged access. An active employee login authorizes tracking/chat but the report is not tied to that employee's identity. Managers and unauthenticated callers are denied. User-authored content/evidence and infrastructure logs are outside application-layer identity suppression.

## Permissions and RPCs

RLS is enabled on all three existing tables. `aepm_anonymous_rpc_only` is a restrictive ALL policy with USING(false)/WITH CHECK(false), added without removing old policies. All table-level AND column-level privileges, plus identity-sequence privileges, are revoked from PUBLIC/anon/authenticated. This deliberately discontinues any old direct-table client path, including administrator SELECT, so every normal application request uses an explicit safe RPC projection. No unrelated table policies or global default privileges are modified.

Private helpers are in a non-exposed schema with no client schema usage or EXECUTE grants. Public RPCs are SECURITY DEFINER with empty search_path, database-role checks and explicit projections. Only authenticated has EXECUTE; inside each function an active role 3 employee or role 1 administrator is required. All raw reference/hash fields and submitter identity fields are excluded from administrator-facing JSON. No reference helper can be invoked directly by normal clients. These patterns follow [Supabase function guidance](https://supabase.com/docs/guides/database/functions) and [PostgreSQL RESTRICT behavior](https://www.postgresql.org/docs/current/sql-altertable.html).

| RPC | Authorized caller |
|---|---|
| aepm_submit_anonymous_report | Employee; new secure reference, category, subject, description, evidence |
| aepm_track_anonymous_report | Employee plus exact secret reference; unknown reference returns null |
| aepm_employee_report_messages | Employee plus exact secret reference |
| aepm_send_employee_report_message | Employee plus exact secret reference |
| aepm_list_anonymous_reports | Administrator; AND filters by status/category/inclusive submitted dates, limit 1-100/offset |
| aepm_get_anonymous_report | Administrator |
| aepm_admin_report_messages | Administrator |
| aepm_send_admin_report_message | Administrator |
| aepm_get_anonymous_investigation | Administrator; latest snapshot |
| aepm_save_anonymous_investigation | Administrator; append snapshot and atomically advance/report status |
| aepm_anonymous_investigation_history | Administrator; complete ordered history |

Evidence remains an optional URL/path string; there is no upload implementation or storage policy change. Categories remain free text because no category lookup table was supplied. Existing content is not truncated to fit new input validation. Date filters retain inclusive Asia/Kuala_Lumpur local-day boundaries (UTC+08:00).

## Checks and reproducible commands

```powershell
mvn clean compile
mvn test-compile dependency:build-classpath '-Dmdep.outputFile=target/anonymous-test-classpath.txt'
$anonymousClasspath = 'target/test-classes;target/classes;' + (Get-Content target/anonymous-test-classpath.txt -Raw).Trim()
java -cp $anonymousClasspath com.aepm.logic.anonymous.AnonymousReportingSmokeTest
$anonymousSqlTools = Join-Path $env:TEMP 'aepm-anonymous-sql-test-tools'
npm.cmd install --prefix $anonymousSqlTools --ignore-scripts --no-audit --no-fund @electric-sql/pglite
node supabase/tests/anonymous-reporting-alignment.test.mjs $anonymousSqlTools
```

The SQL test runner uses only an isolated in-memory PostgreSQL engine, no live connection. It tests populated legacy preservation, multiple investigations per report, exact spaced/Unicode references, hash-only storage, abort/rollback for unsafe legacy RPCs and dependent views (including nested views and RESTRICT), the actual verification SQL, column-level privilege revocation despite old permissive policies, role restrictions, safe JSON, chat, filters, pagination, workflow and both timestamp triggers. The old `anonymous-reporting.test.mjs` is retained only for the historical migration 001; use the alignment runner for the supplied live schema. Standalone Java tests use a stub transport and are run explicitly, not automatically by mvn test. Only Maven manages generated target files.

No UI/FXML/CSS implementation or changes are part of this alignment.
