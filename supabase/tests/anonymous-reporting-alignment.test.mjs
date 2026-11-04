// Run against an isolated in-memory PostgreSQL engine, never the live project.
// Pass an external test-tools directory containing @electric-sql/pglite as argv[2].
import assert from 'node:assert/strict';
import { randomBytes, randomUUID, createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
const { PGlite } = await import(pathToFileURL(resolve(process.argv[2], 'node_modules/@electric-sql/pglite/dist/index.js')).href);
const db = new PGlite();
let checks = 0;
const admin = randomUUID(), employee = randomUUID(), secondEmployee = randomUUID(), manager = randomUUID();
const reference = randomBytes(32).toString('base64url');
const otherReference = randomBytes(32).toString('base64url');
const legacyId = randomUUID();
const legacyReference = ' Legacy #01 ';
const unicodeReference = '????-?';
const unicodeId = randomUUID();
async function check(value, message) { checks++; assert.ok(value, message); }
async function denied(sql, params = [], code) {
  try { await db.query(sql, params); }
  catch (error) { checks++; if (code) assert.equal(error.code, code); return; }
  assert.fail('Expected denial: ' + sql);
}
async function asUser(id, role = 'authenticated') {
  await db.exec('reset role');
  await db.query("select set_config('request.jwt.claim.sub', $1, false)", [id || '']);
  await db.exec('set role ' + role);
}
async function rpc(name, params = []) {
  const placeholders = params.map((_, i) => '$' + (i + 1)).join(',');
  return (await db.query('select public.' + name + '(' + placeholders + ') as result', params)).rows[0].result;
}
try {
  // Minimal Supabase auth and existing-user contract, restricted to this disposable engine.
  await db.exec(`create role anon nologin; create role authenticated nologin;
    create schema auth; create table auth.users(id uuid primary key);
    create function auth.uid() returns uuid language sql stable as
      $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
    grant usage on schema public, auth to anon, authenticated;
    grant execute on function auth.uid() to anon, authenticated;
    create table public.user_tbl(user_id uuid primary key,role_id bigint,account_status text);`);
  for (const [id, role] of [[admin,1],[employee,3],[secondEmployee,3],[manager,2]]) {
    await db.query('insert into auth.users values($1)',[id]);
    await db.query("insert into public.user_tbl values($1,$2,'Active')",[id,role]);
  }
  await db.exec(`create table public.anonymous_report_tbl(
    report_id uuid primary key default gen_random_uuid(),reference_number varchar(50) unique not null,
    report_category varchar(100) not null,report_description text not null,evidence_file text,
    case_status varchar(30) not null default 'Submitted' check(case_status in ('Submitted','Under Review','Under Investigation','Resolved','Closed')),
    submitted_at timestamptz not null default now(),updated_at timestamptz not null default now());
    create table public.anonymous_message_tbl(
      message_id bigint generated always as identity primary key,report_id uuid references public.anonymous_report_tbl(report_id),
      sender_type varchar(30) not null check(sender_type in ('Anonymous Reporter','Administrator')),
      sender_admin_id uuid references public.user_tbl(user_id),message_text text not null,
      sent_at timestamptz not null default now(),is_read boolean not null default false);
    create table public.investigation_tbl(
      investigation_id bigint generated always as identity primary key,report_id uuid references public.anonymous_report_tbl(report_id),
      administrator_id uuid references public.user_tbl(user_id),investigation_note text,previous_status varchar(30),new_status varchar(30),
      created_at timestamptz not null default now());
    grant all on public.anonymous_report_tbl,public.anonymous_message_tbl,public.investigation_tbl to anon,authenticated;
    grant select(investigation_note) on public.investigation_tbl to authenticated;
    grant select(reference_number) on public.anonymous_report_tbl to authenticated;
    alter table public.anonymous_report_tbl enable row level security;
    alter table public.anonymous_message_tbl enable row level security;
    alter table public.investigation_tbl enable row level security;
    create policy legacy_allow_all on public.anonymous_report_tbl for all to public using(true) with check(true);
    create policy legacy_allow_all on public.anonymous_message_tbl for all to public using(true) with check(true);
    create policy legacy_allow_all on public.investigation_tbl for all to public using(true) with check(true);`);
  await db.query("insert into public.anonymous_report_tbl(report_id,reference_number,report_category,report_description,case_status,submitted_at,updated_at) values($1,$2,'Legacy','Historical report','Under Investigation','2026-01-01Z','2026-01-02Z')",[legacyId,legacyReference]);
  await db.query("insert into public.anonymous_report_tbl(report_id,reference_number,report_category,report_description,case_status) values($1,$2,'Legacy','Historical Unicode report','Closed')",[unicodeId,unicodeReference]);
  await db.query("insert into public.anonymous_message_tbl(report_id,sender_type,sender_admin_id,message_text,is_read) values($1,'Administrator',$2,'Historical reply',true)",[legacyId,admin]);
  await db.query("insert into public.investigation_tbl(report_id,administrator_id,investigation_note,previous_status,new_status,created_at) values($1,$2,'Historical private note','Submitted','Under Review','2026-01-01Z'),($1,$2,'Second historical note','Under Review','Under Investigation','2026-01-02Z')",[legacyId,admin]);
  await db.query("insert into public.investigation_tbl(report_id,investigation_note,previous_status,new_status,created_at) values($1,null,null,null,'2026-01-03Z')",[unicodeId]);
  const migration = await readFile(new URL('../migrations/202611040002_anonymous_reporting_schema_alignment.sql',import.meta.url),'utf8');
  // An unsafe old callable RPC must cause rollback, not be silently overwritten or left exposing hashes.
  await db.exec(`create function public.legacy_exposes_report() returns jsonb language sql security definer as
      $$select to_jsonb(r) from public.anonymous_report_tbl r limit 1$$;`);
  try { await db.exec(migration); assert.fail('Unsafe legacy RPC should block upgrade'); }
  catch(error) { assert.match(error.message,/legacy RPC/); checks++; await db.exec('rollback'); }
  await check((await db.query("select count(*)::int n from information_schema.columns where table_schema='public' and table_name='anonymous_report_tbl' and column_name='reference_hash'")).rows[0].n===0,'Failed upgrade left a partial hash column');
  await check((await db.query('select reference_number from public.anonymous_report_tbl where report_id=$1',[legacyId])).rows[0].reference_number===legacyReference,'Failed upgrade lost a legacy reference');
  await db.exec('revoke all on function public.legacy_exposes_report() from public,anon,authenticated');
  // A dependent view blocks even a RESTRICT drop. It is removed only inside this disposable test database.
  await db.exec('create view public.legacy_reference_view as select reference_number from public.anonymous_report_tbl');
  await db.exec('grant select on public.legacy_reference_view to authenticated');
  try { await db.exec(migration); assert.fail('Exposed legacy view should block upgrade'); }
  catch(error) { assert.match(error.message,/dependent view/); checks++; await db.exec('rollback'); }
  await db.exec('revoke select on public.legacy_reference_view from authenticated');
  try { await db.exec(migration); assert.fail('RESTRICT must refuse a dependent view even after access is revoked'); }
  catch(error) { assert.equal(error.code,'2BP01'); checks++; await db.exec('rollback'); }
  await db.exec('drop view public.legacy_reference_view');
  await db.exec('create view public.legacy_private_notes_base as select investigation_note from public.investigation_tbl; create view public.legacy_nested_notes as select * from public.legacy_private_notes_base; grant select on public.legacy_nested_notes to authenticated');
  try { await db.exec(migration); assert.fail('Nested exposed view should block upgrade'); }
  catch(error) { assert.match(error.message,/dependent view/); checks++; await db.exec('rollback'); }
  await db.exec('drop view public.legacy_nested_notes; drop view public.legacy_private_notes_base');
  await db.exec(migration);
  await db.exec(await readFile(new URL('../verification/202611040002_anonymous_reporting_verification.sql',import.meta.url),'utf8'));
  checks++;
  await db.exec('create view public.audit_private_notes_base as select investigation_note from public.investigation_tbl; create view public.audit_nested_notes as select * from public.audit_private_notes_base; grant select on public.audit_nested_notes to authenticated');
  try { await db.exec(await readFile(new URL('../verification/202611040002_anonymous_reporting_verification.sql',import.meta.url),'utf8')); assert.fail('Verification must reject nested exposed views'); }
  catch(error) { assert.match(error.message,/Exposed dependent view/); checks++; await db.exec('rollback'); }
  await db.exec('drop view public.audit_nested_notes; drop view public.audit_private_notes_base');
  await check((await db.query('select count(*)::int n from public.anonymous_report_tbl')).rows[0].n===2,'Legacy reports were deleted');
  await check((await db.query('select count(*)::int n from public.anonymous_message_tbl')).rows[0].n===1,'Legacy messages were deleted');
  await check((await db.query('select count(*)::int n from public.investigation_tbl')).rows[0].n===3,'Legacy investigation history was deleted or collapsed');
  const legacy = (await db.query('select * from public.anonymous_report_tbl where report_id=$1',[legacyId])).rows[0];
  await check(legacy.reference_hash===createHash('sha256').update(legacyReference,'utf8').digest('hex'),'Reference bytes were changed before hashing');
  await check(legacy.case_status==='Under Investigation' && legacy.report_description==='Historical report','Legacy report content/status changed');
  await check(legacy.subject==='Legacy report (title not supplied)','Missing legacy subject was not labeled');
  await check((await db.query('select * from public.anonymous_message_tbl')).rows[0].message_text==='Historical reply','Legacy message content changed');
  await asUser(employee);
  await check((await rpc('aepm_track_anonymous_report',[legacyReference])).report_id===legacyId,'Legacy spaced reference no longer tracks');
  await check((await rpc('aepm_track_anonymous_report',[unicodeReference])).report_id===unicodeId,'Legacy Unicode reference no longer tracks');
  await denied('select aepm_anonymous_private.reference_id($1)',[legacyReference],'42501');
  await asUser(admin);
  const legacyHistory = await rpc('aepm_anonymous_investigation_history',[legacyId]);
  await check(legacyHistory.length===2 && legacyHistory[0].investigation_note==='Historical private note','Existing investigation history not accessible');
  await check((await rpc('aepm_get_anonymous_investigation',[legacyId])).new_status==='Under Investigation','Latest investigation not selected');
  await check((await rpc('aepm_get_anonymous_investigation',[unicodeId])).new_status===null,'Missing legacy audit status was invented');
  await db.exec('reset role');
  await asUser(employee);
  await denied("select public.aepm_submit_anonymous_report($1,'Safety',' ','Details',null)",[reference],'22023');
  const report = await rpc('aepm_submit_anonymous_report',[reference,'Safety','Concern','Details',null]);
  const id = report.report_id;
  await check(report.case_status === 'Submitted', 'Wrong default status');
  await check(!JSON.stringify(report).includes(reference), 'Submission report exposes token');
  await denied('select * from public.anonymous_report_tbl',[],'42501');
  await denied('select reference_hash from public.anonymous_report_tbl',[],'42501');
  await denied("insert into public.anonymous_report_tbl(report_category,subject,report_description) values('x','y','z')",[],'42501');
  await denied('select public.aepm_list_anonymous_reports()',[],'42501');
  await check((await rpc('aepm_track_anonymous_report',[reference])).report_id === id,'Reference tracking failed');
  await check(await rpc('aepm_track_anonymous_report',[otherReference]) === null,'Other reference disclosed report');
  await check(await rpc('aepm_track_anonymous_report',['invalid']) === null,'Malformed reference disclosed report');
  await denied('select public.aepm_send_employee_report_message($1,$2)',[otherReference,'Hello'],'P0002');
  await denied('select public.aepm_send_employee_report_message($1,$2)',[reference,'  '],'22023');
  const sent = await rpc('aepm_send_employee_report_message',[reference,'Follow up']);
  await check(sent.sender_type === 'Anonymous Reporter','Spoofed employee sender');
  await asUser(secondEmployee);
  await check((await rpc('aepm_track_anonymous_report',[reference])).report_id === id,'Tracking improperly binds identity');
  await asUser(manager);
  await denied('select public.aepm_track_anonymous_report($1)',[reference],'42501');
  await denied('select public.aepm_list_anonymous_reports()',[],'42501');
  await denied('select * from public.anonymous_report_tbl',[],'42501');
  await asUser(admin);
  const page = await rpc('aepm_list_anonymous_reports',['Submitted','Safety',null,null,10,0]);
  await check(page.total_count === 1 && page.rows.length === 1,'Review filters failed');
  await check((await rpc('aepm_list_anonymous_reports',[null,'Other',null,null,10,0])).rows.length === 0,'Category filter failed');
  await check((await rpc('aepm_list_anonymous_reports',[null,null,'2000-01-01','2000-01-02',10,0])).rows.length === 0,'Date filter failed');
  await check((await rpc('aepm_list_anonymous_reports',['Submitted','Safety',null,null,10,1])).rows.length === 0,'Offset ignored');
  await denied('select public.aepm_list_anonymous_reports(null,null,null,null,101,0)',[],'22023');
  await denied("select public.aepm_list_anonymous_reports(null,null,'2026-02-01','2026-01-01',10,0)",[],'22023');
  await denied('select reference_hash from public.anonymous_report_tbl',[],'42501');
  await denied("update public.anonymous_report_tbl set case_status='Closed' where report_id=$1",[id],'42501');
  const detail = await rpc('aepm_get_anonymous_report',[id]);
  for (const name of ['user_id','employee_id','username','email','reference','reference_hash'])
    await check(!(name in detail),'Administrator received identifying/secret field: ' + name);
  const reply = await rpc('aepm_send_admin_report_message',[id,'Reply']);
  await check(reply.sender_type === 'Administrator','Wrong administrator sender');
  await check(!('sender_admin_id' in reply),'Conversation exposes sender identity metadata');
  await check((await rpc('aepm_admin_report_messages',[id])).length === 2,'Admin conversation incomplete');
  await denied('select public.aepm_track_anonymous_report($1)',[reference],'42501');
  await denied("select public.aepm_save_anonymous_investigation($1,'Submitted','Resolved','Notes',null,'Outcome')",[id],'22023');
  await rpc('aepm_save_anonymous_investigation',[id,'Submitted','Under Review','Private notes',admin,'Draft outcome']);
  await denied("select public.aepm_save_anonymous_investigation($1,'Submitted','Under Review','Notes',null,null)",[id],'40001');
  await denied("select public.aepm_save_anonymous_investigation($1,'Under Review','Under Investigation','Notes',$2,null)",[id,employee],'22023');
  await asUser(employee);
  const tracked = await rpc('aepm_track_anonymous_report',[reference]);
  await check(tracked.case_status === 'Under Review' && tracked.resolution === null,'Draft outcome was exposed');
  await check(!JSON.stringify(tracked).includes('Private notes'),'Private notes exposed');
  await denied('select * from public.investigation_tbl',[],'42501');
  await denied('select public.aepm_get_anonymous_investigation($1)',[id],'42501');
  await denied("select public.aepm_save_anonymous_investigation($1,'Under Review','Under Investigation','Notes',null,null)",[id],'42501');
  const history = await rpc('aepm_employee_report_messages',[reference]);
  await check(history.length === 2 && Date.parse(history[0].sent_at) <= Date.parse(history[1].sent_at),'Employee history not ordered');
  await asUser(admin);
  await rpc('aepm_save_anonymous_investigation',[id,'Under Review','Under Investigation','Private notes',admin,null]);
  await denied("select public.aepm_save_anonymous_investigation($1,'Under Investigation','Resolved','Notes',null,' ')",[id],'22023');
  await rpc('aepm_save_anonymous_investigation',[id,'Under Investigation','Resolved','Private notes',admin,'Public outcome']);
  await rpc('aepm_save_anonymous_investigation',[id,'Resolved','Closed','Private notes',admin,'Public outcome']);
  await denied("select public.aepm_save_anonymous_investigation($1,'Closed','Closed','Notes',null,'Outcome')",[id],'22023');
  await denied('select public.aepm_send_admin_report_message($1,$2)',[id,'Too late'],'22023');
  await asUser(employee);
  await check((await rpc('aepm_track_anonymous_report',[reference])).resolution === 'Public outcome','Published outcome missing');
  await denied('select public.aepm_send_employee_report_message($1,$2)',[reference,'Too late'],'22023');
  await asUser(null,'anon');
  await denied('select public.aepm_track_anonymous_report($1)',[reference],'42501');
  await db.exec('reset role');
  const keys = (await db.query('select report_id,reference_hash from public.anonymous_report_tbl')).rows;
  await check(keys.length === 3 && keys.every(k=>k.reference_hash.length===64) && !JSON.stringify(keys).includes(reference),'Plaintext reference stored');
  await check((await db.query("select count(*)::int as n from information_schema.columns where table_schema='public' and table_name like 'anonymous_%' and column_name in ('employee_id','user_id','username','email')")).rows[0].n === 0,'Submitter identity stored');
  // Return to administrator authorization for history; normal clients never get direct table rights.
  await asUser(admin);
  const historySnapshots = await rpc('aepm_anonymous_investigation_history',[id]);
  await check(historySnapshots.length===4,'New investigation updates overwrote snapshots');
  await check(historySnapshots[0].previous_status==='Submitted' && historySnapshots[3].new_status==='Closed','History workflow was not retained');
  await check((await rpc('aepm_get_anonymous_report',[id])).case_status==='Closed','Final live status mismatch');
  await db.exec('reset role');
  await db.query("update public.anonymous_report_tbl set updated_at='2000-01-01Z' where report_id=$1",[id]);
  await check(Date.parse((await db.query('select updated_at from public.anonymous_report_tbl where report_id=$1',[id])).rows[0].updated_at)>Date.parse('2000-01-01Z'),'Report timestamp trigger did not override stale timestamp');
  await db.query("update public.investigation_tbl set updated_at='2000-01-01Z' where report_id=$1",[id]);
  await check((await db.query('select updated_at from public.investigation_tbl where report_id=$1',[id])).rows.every(row=>Date.parse(row.updated_at)>Date.parse('2000-01-01Z')),'Investigation timestamp trigger failed');
  await db.exec(await readFile(new URL('../verification/202611040002_anonymous_reporting_verification.sql',import.meta.url),'utf8'));
  checks++;
  console.log('PASS: ' + checks + ' live-schema alignment, preservation, RLS and workflow checks; no live Supabase access.');
} finally { await db.close(); }
