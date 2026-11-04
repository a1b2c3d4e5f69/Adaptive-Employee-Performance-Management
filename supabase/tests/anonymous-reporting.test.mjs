// Run against an isolated in-memory PostgreSQL engine, never the live project.
// Pass an external test-tools directory containing @electric-sql/pglite as argv[2].
import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
const { PGlite } = await import(pathToFileURL(resolve(process.argv[2], 'node_modules/@electric-sql/pglite/dist/index.js')).href);
const db = new PGlite();
let checks = 0;
const admin = randomUUID(), employee = randomUUID(), secondEmployee = randomUUID(), manager = randomUUID();
const reference = randomBytes(32).toString('base64url');
const otherReference = randomBytes(32).toString('base64url');
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
  await db.exec(await readFile(new URL('../migrations/202611040001_anonymous_reporting.sql', import.meta.url),'utf8'));
  await asUser(employee);
  await denied("select public.aepm_submit_anonymous_report($1,'Safety',' ','Details',null)",[reference],'22023');
  const report = await rpc('aepm_submit_anonymous_report',[reference,'Safety','Concern','Details',null]);
  const id = report.report_id;
  await check(report.status === 'Submitted', 'Wrong default status');
  await check(!JSON.stringify(report).includes(reference), 'Submission report exposes token');
  await check((await db.query('select * from public.anonymous_report_tbl')).rows.length === 0,'Employee bypasses RLS');
  await denied('select * from aepm_anonymous_private.report_reference',[],'42501');
  await denied("insert into public.anonymous_report_tbl(category,subject,description) values('x','y','z')",[],'42501');
  await denied('select public.aepm_list_anonymous_reports()',[],'42501');
  await check((await rpc('aepm_track_anonymous_report',[reference])).report_id === id,'Reference tracking failed');
  await check(await rpc('aepm_track_anonymous_report',[otherReference]) === null,'Other reference disclosed report');
  await check(await rpc('aepm_track_anonymous_report',['invalid']) === null,'Malformed reference disclosed report');
  await denied('select public.aepm_send_employee_report_message($1,$2)',[otherReference,'Hello'],'P0002');
  await denied('select public.aepm_send_employee_report_message($1,$2)',[reference,'  '],'22023');
  const sent = await rpc('aepm_send_employee_report_message',[reference,'Follow up']);
  await check(sent.sender_type === 'ANONYMOUS_EMPLOYEE','Spoofed employee sender');
  await asUser(secondEmployee);
  await check((await rpc('aepm_track_anonymous_report',[reference])).report_id === id,'Tracking improperly binds identity');
  await asUser(manager);
  await denied('select public.aepm_track_anonymous_report($1)',[reference],'42501');
  await denied('select public.aepm_list_anonymous_reports()',[],'42501');
  await check((await db.query('select * from public.anonymous_report_tbl')).rows.length === 0,'Manager bypasses RLS');
  await asUser(admin);
  const page = await rpc('aepm_list_anonymous_reports',['Submitted','Safety',null,null,10,0]);
  await check(page.total_count === 1 && page.rows.length === 1,'Review filters failed');
  await check((await rpc('aepm_list_anonymous_reports',[null,'Other',null,null,10,0])).rows.length === 0,'Category filter failed');
  await check((await rpc('aepm_list_anonymous_reports',[null,null,'2000-01-01','2000-01-02',10,0])).rows.length === 0,'Date filter failed');
  await check((await rpc('aepm_list_anonymous_reports',[null,null,null,null,10,1])).rows.length === 0,'Offset ignored');
  await denied('select public.aepm_list_anonymous_reports(null,null,null,null,101,0)',[],'22023');
  await denied("select public.aepm_list_anonymous_reports(null,null,'2026-02-01','2026-01-01',10,0)",[],'22023');
  await denied('select * from aepm_anonymous_private.report_reference',[],'42501');
  await denied("update public.anonymous_report_tbl set status='Closed' where report_id=$1",[id],'42501');
  const detail = await rpc('aepm_get_anonymous_report',[id]);
  for (const name of ['user_id','employee_id','username','email','reference','reference_hash'])
    await check(!(name in detail),'Administrator received identifying/secret field: ' + name);
  const reply = await rpc('aepm_send_admin_report_message',[id,'Reply']);
  await check(reply.sender_type === 'ADMINISTRATOR','Wrong administrator sender');
  await check((await rpc('aepm_admin_report_messages',[id])).length === 2,'Admin conversation incomplete');
  await denied('select public.aepm_track_anonymous_report($1)',[reference],'42501');
  await denied("select public.aepm_save_anonymous_investigation($1,'Submitted','Resolved','Notes',null,'Outcome')",[id],'22023');
  await rpc('aepm_save_anonymous_investigation',[id,'Submitted','Under Review','Private notes',admin,'Draft outcome']);
  await denied("select public.aepm_save_anonymous_investigation($1,'Submitted','Under Review','Notes',null,null)",[id],'40001');
  await denied("select public.aepm_save_anonymous_investigation($1,'Under Review','Investigating','Notes',$2,null)",[id,employee],'22023');
  await asUser(employee);
  const tracked = await rpc('aepm_track_anonymous_report',[reference]);
  await check(tracked.status === 'Under Review' && tracked.resolution === null,'Draft outcome was exposed');
  await check(!JSON.stringify(tracked).includes('Private notes'),'Private notes exposed');
  await check((await db.query('select * from public.anonymous_investigation_tbl')).rows.length === 0,'Employee can read notes');
  await denied('select public.aepm_get_anonymous_investigation($1)',[id],'42501');
  await denied("select public.aepm_save_anonymous_investigation($1,'Under Review','Investigating','Notes',null,null)",[id],'42501');
  const history = await rpc('aepm_employee_report_messages',[reference]);
  await check(history.length === 2 && Date.parse(history[0].sent_at) <= Date.parse(history[1].sent_at),'Employee history not ordered');
  await asUser(admin);
  await rpc('aepm_save_anonymous_investigation',[id,'Under Review','Investigating','Private notes',admin,null]);
  await denied("select public.aepm_save_anonymous_investigation($1,'Investigating','Resolved','Notes',null,' ')",[id],'22023');
  await rpc('aepm_save_anonymous_investigation',[id,'Investigating','Resolved','Private notes',admin,'Public outcome']);
  await rpc('aepm_save_anonymous_investigation',[id,'Resolved','Closed','Private notes',admin,'Public outcome']);
  await denied("select public.aepm_save_anonymous_investigation($1,'Closed','Closed','Notes',null,'Outcome')",[id],'22023');
  await denied('select public.aepm_send_admin_report_message($1,$2)',[id,'Too late'],'22023');
  await asUser(employee);
  await check((await rpc('aepm_track_anonymous_report',[reference])).resolution === 'Public outcome','Published outcome missing');
  await denied('select public.aepm_send_employee_report_message($1,$2)',[reference,'Too late'],'22023');
  await asUser(null,'anon');
  await denied('select public.aepm_track_anonymous_report($1)',[reference],'42501');
  await db.exec('reset role');
  const keys = (await db.query('select * from aepm_anonymous_private.report_reference')).rows;
  await check(keys.length === 1 && keys[0].reference_hash.length === 64 && !JSON.stringify(keys).includes(reference),'Plaintext reference stored');
  await check((await db.query("select count(*)::int as n from information_schema.columns where table_schema='public' and table_name like 'anonymous_%' and column_name in ('employee_id','user_id','username','email')")).rows[0].n === 0,'Submitter identity stored');
  console.log('PASS: ' + checks + ' isolated PostgreSQL migration/RLS/workflow checks; no live Supabase access.');
} finally { await db.close(); }
