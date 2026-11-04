# Anonymous Reporting JavaFX UI

Functional JavaFX screens using the current AEPM top bars, sidebars and existing app.css. No whole-application redesign, backend/service/repository changes, CSS changes, dependency changes or Supabase writes were made for this UI task. Applied migrations 001/002/003 are unchanged.

## Employee

- Submit using the six suggested categories, subject, description and optional evidence URL/path/file name. Required fields and lengths are validated before service calls. Success displays the server-generated secret reference prominently with the requested keep-safe warning. Successful content fields are cleared to discourage duplicate submissions.
- Copy Reference uses the system clipboard only when the employee clicks it. Clear Form clears the submission fields/receipt; the separate tracking section remains independent. References are never written to files, preferences, static variables or session/profile data.
- Browse Evidence uses JavaFX FileChooser and records the filename only, avoiding an identifying local home-directory path. It does not upload a file. The UI explains this and permits a shareable URL/path to be entered manually.
- Tracking displays subject, category, status and timestamps. Resolution caption/value are shown only when returned by the server. Changing the reference clears previous results and disables Open Chat until the new reference is validated.
- Chat supports in-memory reference handoff or manual entry, ordered messages with sender/timestamp, send, refresh and back. The back action hands the reference directly to the report controller in memory. No employee ID, administrator sender ID, reference hash or private investigation fields are rendered.
- Closed conversations follow current backend acceptance behavior. A later backend rejection is handled with a readable message and retains the unsent draft.

## Administrator

- Report list supports status/category/inclusive date filters, typed-date validation, clear, refresh, 25-row pagination, selection/double-click and detail. Editable category filtering also supports pre-existing categories outside the suggestions. Pending filter changes clear stale results. The table displays only Submitted Date, Category, Subject, Status and Updated At.
- Detail shows report information, evidence text, published resolution and conversation. Evidence is displayed as selectable text; untrusted paths are not automatically opened or fetched.
- Reply/refresh conversation do not discard private investigation drafts. Successful sends clear the composer before refreshing so a failed refresh does not encourage resending the same content.
- Investigation status choices contain only the current status and the next allowed status. Resolution is required for Resolved/Closed. History displays Date, Previous Status, New Status, private Investigation Note and Resolution. Closed investigation editing is disabled, matching the Java service. No plaintext reference or hash is present in administrator screens.

## Navigation and lifecycle

Existing Employee Anonymous Report handlers point to employeeAnonymousReport.fxml; existing Admin Anonymous Reports handlers point to adminAnonymousReports.fxml. AdminHome's obsolete singular path is corrected. The existing Employee AI controller receives its missing Anonymous action using its own navigation helper. Only Anonymous handlers are changed in existing page controllers; other navigation methods remain intact. No Manager resources/controllers are changed, and Manager FXML menus contain no Anonymous entry.

NavigationManager's existing two-argument API delegates to a backward-compatible overload that configures the destination controller before display. Employee chat receives only the reference; administrator detail receives only report UUID. No static/session transfer cache is introduced. Existing top-bar/notification installation and scene/style handling remain in NavigationManager.

AnonymousUiController is UI-only support for background Tasks, safe messages, role/session guards and navigation. Network/service work runs outside the FX thread; the page is disabled during a request to prevent duplicate actions or navigating away while a receipt is arriving. Stale results are discarded after navigation/session changes. Page detachment wipes references, report text and private notes, including center-panel replacements. A session watcher also clears the page after an external session change. Exceptions are not printed or shown as SQL/RLS/RPC details.

## Existing service methods used

| Service | Methods |
|---|---|
| AnonymousReportService | submit, review, detail |
| AnonymousReportTrackingService | track |
| AnonymousReportChatService | employeeHistory, sendEmployee, administratorHistory, sendAdministrator |
| AnonymousInvestigationService | history, save |

Controllers do not import or call SupabaseClient, AppConfig, REST endpoints or repositories. Repository substitution exists only in the test fixture; production controllers use the existing services unchanged.

## Validation results

- Required `mvn clean compile`: **BUILD SUCCESS**, 149 main source files compiled. Existing unrelated warnings remain unchanged.
- **127 JavaFX/FXML/controller checks passed** with real FXML, real controllers, real services and an in-memory repository. No live Supabase requests, real-account credentials, production data writes or visible test windows were used.
- Employee submission/receipt, tracking, invalid references, chat handoff/send/back and receipt erasure after leaving passed.
- Administrator filtering/date validation/clear, empty results, next/previous pagination, detail, reply, note preservation, workflow through Closed, resolution and five history snapshots passed.
- Manager access to all four screens was blocked without backend calls; Manager FXML navigation has no Anonymous menu. A delayed result after a session switch did not display its secret reference.
- Simulated backend SQL/private error text was not exposed to the user. A simulated post-closure rejection retained the unsent message.
- Four fixture screenshots were rendered in a temporary directory and visually inspected. Additional contrast assertions prevent white-on-white data labels under the current theme. Native Browse/clipboard dialogs are not automated by this test.
- All applied migration fingerprints match their task-start values:
  - 001: 64E79735606DB5BDF6B7F00700327EBA88458B1701A3D5E31675A0D4664BD113
  - 002: 9BDA93CACE013E18601CD257D33FFFE7B48FB9923A112E65E30BA01FD9BA48D1
  - 003: 91EE16C7B23EE20378818002E9A288A948C18A969173E985EE9C0B3D76730F69

```powershell
mvn clean compile
mvn test-compile dependency:build-classpath '-Dmdep.outputFile=target/anonymous-ui-test-classpath.txt'
$anonymousUiClasspath = 'target/test-classes;target/classes;' + (Get-Content target/anonymous-ui-test-classpath.txt -Raw).Trim()
$anonymousUiScreenshots = Join-Path $env:TEMP 'aepm-anonymous-ui-preview'
java --enable-native-access=ALL-UNNAMED -cp $anonymousUiClasspath com.aepm.ui.anonymous.AnonymousUiSmokeTest $anonymousUiScreenshots
```

This is an explicit standalone smoke-test main, consistent with existing project tests; mvn test does not automatically execute it. The prior 36-test live backend result remains separate; this UI task did not rerun live writes. Only Maven manages generated target files.

## Exact files created in this UI task

- `src/main/resources/fxml/employee/employeeAnonymousReport.fxml`
- `src/main/resources/fxml/employee/employeeAnonymousChat.fxml`
- `src/main/resources/fxml/admin/adminAnonymousReports.fxml`
- `src/main/resources/fxml/admin/adminAnonymousReportDetail.fxml`
- `src/main/java/com/aepm/ui/employee/EmployeeAnonymousReportController.java`
- `src/main/java/com/aepm/ui/employee/EmployeeAnonymousChatController.java`
- `src/main/java/com/aepm/ui/admin/AdminAnonymousReportsController.java`
- `src/main/java/com/aepm/ui/admin/AdminAnonymousReportDetailController.java`
- `src/main/java/com/aepm/ui/common/AnonymousUiController.java`
- `src/test/java/com/aepm/ui/anonymous/AnonymousUiSmokeTest.java`
- `docs/anonymous-reporting-ui.md`

## Exact existing files modified in this UI task

- `src/main/java/com/aepm/ui/navigation/NavigationManager.java`
- `src/main/java/com/aepm/ui/employee/EmployeeHomeController.java`
- `src/main/java/com/aepm/ui/employee/EmployeeProfileController.java`
- `src/main/java/com/aepm/ui/employee/EmployeeTaskController.java`
- `src/main/java/com/aepm/ui/employee/EmployeePerformanceController.java`
- `src/main/java/com/aepm/ui/employee/EmployeeAttendanceController.java`
- `src/main/java/com/aepm/ui/employee/EmployeeAIRecommendationController.java`
- `src/main/java/com/aepm/ui/admin/AdminHomeController.java`
- `src/main/java/com/aepm/ui/admin/AdminProfileController.java`
- `src/main/java/com/aepm/ui/admin/AdminUserManagementController.java`
- `src/main/java/com/aepm/ui/admin/AdminTaskController.java`
- `src/main/java/com/aepm/ui/admin/AdminKpiController.java`
- `src/main/java/com/aepm/ui/admin/AdminReportController.java`
- `src/main/java/com/aepm/ui/admin/AdminAttendanceController.java`
- `src/main/java/com/aepm/ui/admin/AdminEscalationRulesController.java`
- `src/main/java/com/aepm/ui/admin/AdminAIConfigurationController.java`
- `src/main/java/com/aepm/ui/admin/AdminTaskCategoryController.java`
- `src/main/java/com/aepm/ui/admin/AdminTaskDependencyController.java`
- `src/main/java/com/aepm/ui/admin/SystemConfigurationController.java`

Earlier unrelated Reports export edits and existing untracked backend files are preserved; they are not new UI-task changes.
