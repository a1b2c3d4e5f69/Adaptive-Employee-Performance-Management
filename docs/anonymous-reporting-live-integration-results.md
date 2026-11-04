# Live Anonymous Reporting integration results

Executed: 2026-11-04T14:20:21.775594400+08:00

Live Supabase requests via existing AuthService, anonymous services/repository, SupabaseClient and AppConfig. No fake transport. No migrations or UI changes executed.

Test report ID: 59e132ac-9893-4f3b-920a-cf0507c52bf8. The labeled test report is retained; no rows were deleted. Secret reference and credentials are intentionally omitted.

| # | Test | Result | Evidence |
|---|---|---|---|
| 1 | Employee login | PASS | Verified live |
| 2 | Submit category, subject, description and optional evidence | PASS | Verified live |
| 3 | Submission succeeds | PASS | Verified live |
| 4 | Employee receives plaintext secret reference | PASS | Verified live |
| 5 | Submission RPC does not expose reference_hash | PASS | Verified live |
| 6 | Employee tracks using returned reference | PASS | Verified live |
| 7 | Tracked status is Submitted | PASS | Verified live |
| 8 | Invalid reference returns no private data | PASS | Verified live |
| 9 | Employee sends anonymous message | PASS | Verified live |
| 10 | Employee retrieves ordered conversation | PASS | Verified live |
| 11 | Report request/response contains no employee identity | PASS | Verified live |
| 12 | Administrator login | PASS | Verified live |
| 13 | Administrator lists reports with backend filters | PASS | Verified live |
| 14 | Submitted report appears in list | PASS | Verified live |
| 15 | Administrator retrieves report detail | PASS | Verified live |
| 16 | Administrator report responses exclude identity and secrets | PASS | Verified live |
| 17 | Administrator retrieves report messages | PASS | Verified live |
| 18 | Administrator replies | PASS | Verified live |
| 19 | Administrator saves private investigation note | PASS | Verified live |
| 20 | Submitted -> Under Review | PASS | Verified live |
| 21 | Under Review -> Under Investigation | PASS | Verified live |
| 22 | Under Investigation -> Resolved with resolution | PASS | Verified live |
| 23 | Employee sees published resolution | PASS | Verified live |
| 24 | Resolved -> Closed | PASS | Verified live |
| 25 | Investigation history is preserved | PASS | Verified live |
| 26 | Manager login | PASS | Verified live |
| 27 | Manager admin-list RPC denied | PASS | Verified live |
| 28 | Manager detail RPC denied | PASS | Verified live |
| 29 | Manager admin messaging RPC denied | PASS | Verified live |
| 30 | Manager investigation RPCs denied | PASS | Verified live |
| 31 | Employee direct report SELECT denied | PASS | Verified live |
| 32 | Employee direct INSERT/UPDATE/DELETE denied on all three tables | PASS | Verified live |
| 33 | Manager direct reads/writes denied on all three tables | PASS | Verified live |
| 34 | Anon/public table and all module RPC access denied | PASS | Verified live |
| 35 | No reference_hash in observed RPC responses; plaintext receipt is employee-only | PASS | Verified live |
| 36 | Investigation notes are administrator-only | PASS | Verified live |

Captured module RPC responses inspected: 59. No raw responses, access tokens, passwords or reference values are persisted.

Scope: storage privacy is checked through identity-free application payloads/responses and the deployed module contract; a database-owner catalog/storage audit is not available using application JWTs. These results cover the RPCs exercised, not arbitrary undiscovered endpoints or infrastructure logs.

## Final validation and fixes

- Full live suite: 36/36 PASS after the user confirmed applying migration 003.
- mvn clean compile: BUILD SUCCESS; 144 main source files compiled. Existing unrelated compiler warnings remain unchanged.
- Updated offline Java contract tests: 87 checks PASS.
- Exact deployed-function SQL regression: 16 checks PASS in isolated PostgreSQL. Original 42883/42702 failures reproduced before applying the patch.
- Migration 002 SHA-256 remained 9BDA93CACE013E18601CD257D33FFFE7B48FB9923A112E65E30BA01FD9BA48D1 throughout this task.
- Migration 003 changes functions/grants only; no table schema or existing data rewrite/deletion.

The employee-only submit receipt is the deliberate exception to plaintext-reference suppression: it returns reference_number once. Every other observed response excludes it, and all observed responses exclude reference_hash. Database-owner physical storage/catalog verification was not performed with normal application JWTs.

One labeled report, two chat messages and five investigation snapshots were retained by the successful run. Failed initial submissions and invalid permission probes did not create records. The successful report is Closed; its ID above permits administrator inspection. No secret reference is saved in this file.

Remaining risks: credentials already stored in README; bearer-reference handling and identifying free-text/evidence/infrastructure logs; client preflight expected-status checks are not atomic; the deployed workflow permits direct-RPC post-closure messaging/same-status Closed edits. See anonymous-reporting-live-testing.md for details and scope.
