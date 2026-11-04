// Exercises the exact deployed 002 function bodies and the 003 patch in isolated PostgreSQL.
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {resolve} from 'node:path';
import {pathToFileURL} from 'node:url';
import {randomUUID} from 'node:crypto';
const {PGlite}=await import(pathToFileURL(resolve(process.argv[2],'node_modules/@electric-sql/pglite/dist/index.js')).href);
const db=new PGlite();let checks=0;
const employee=randomUUID(),admin=randomUUID(),missingProfile=randomUUID();
async function call(name,args=[]) {return (await db.query('select * from public.'+name+'('+args.map((_,i)=>'$'+(i+1)).join(',')+')',args)).rows;}
async function login(id) {await db.query("select set_config('request.jwt.claim.sub',$1,false)",[id]);}
async function error(code,work) {try {await work();assert.fail('Expected SQLSTATE '+code);}catch(e){assert.equal(e.code,code);checks++;}}
try {
 await db.exec(`create role anon;create role authenticated;create schema auth;
 create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
 create table public.user_tbl(user_id uuid primary key,role_id integer,account_status text);
 create table public.anonymous_report_tbl(report_id uuid primary key default gen_random_uuid(),reference_hash varchar(64) unique not null,
 report_category varchar(100) not null,subject varchar(200) not null,report_description text not null,evidence_file text,
 case_status varchar(30) not null default 'Submitted',resolution text,submitted_at timestamptz not null default now(),updated_at timestamptz not null default now());
 create table public.anonymous_message_tbl(message_id bigint generated always as identity primary key,report_id uuid references public.anonymous_report_tbl,
 sender_type varchar(30) not null,sender_admin_id uuid references public.user_tbl,message_text text not null,sent_at timestamptz not null default now(),is_read boolean not null default false);
 create table public.investigation_tbl(investigation_id bigint generated always as identity primary key,report_id uuid references public.anonymous_report_tbl,
 administrator_id uuid references public.user_tbl,investigation_note text,previous_status varchar(30),new_status varchar(30),resolution text,
 created_at timestamptz not null default now(),updated_at timestamptz not null default now());`);
 await db.query("insert into public.user_tbl values($1,3,'Active'),($2,1,'Active')",[employee,admin]);
 const deployed=await readFile(new URL('../migrations/202611040002_anonymous_reporting_schema_alignment.sql',import.meta.url),'utf8');
 const functions=deployed.slice(deployed.indexOf('create or replace function public.aepm_anonymous_current_role_id()'),deployed.indexOf('-- 28. ENABLE RLS'));
 await db.exec(functions);
 await login(employee);
 await error('42883',()=>call('aepm_track_anonymous_report',['test-reference']));
 // Use a disposable public digest shim only to reach the second original bug. Production 003 needs no shim/extension.
 await db.exec("create function public.digest(text,text) returns bytea language sql immutable as $$select sha256(convert_to($1,'UTF8'))$$");
 const submitted=(await call('aepm_submit_anonymous_report',['Safety','Live test','Details',null]))[0];
 await login(admin);
 await error('42702',()=>call('aepm_save_anonymous_investigation',[submitted.report_id,'Private note','Under Review',null]));
 assert.equal((await db.query('select count(*)::int n from public.investigation_tbl')).rows[0].n,0);checks++;
 await error('42702',()=>call('aepm_admin_report_messages',[submitted.report_id]));
 await login(employee);
 await error('42702',()=>call('aepm_employee_report_messages',[submitted.reference_number]));
 await db.exec('drop function public.digest(text,text)');
 await db.exec(await readFile(new URL('../migrations/202611040003_anonymous_reporting_live_fixes.sql',import.meta.url),'utf8'));
 await login(employee);
 assert.equal((await call('aepm_track_anonymous_report',[submitted.reference_number]))[0].report_id,submitted.report_id);checks++;
 const next=(await call('aepm_submit_anonymous_report',['Safety','Second test','Details',null]))[0];
 assert.match(next.reference_number,/^[a-f0-9]{48}$/);assert.ok(!('reference_hash' in next));checks+=2;
 await call('aepm_send_employee_report_message',[next.reference_number,'Follow up']);
 assert.equal((await call('aepm_employee_report_messages',[next.reference_number])).length,1);checks++;
 await login(admin);
 for(const [state,resolution] of [['Submitted',null],['Under Review',null],['Under Investigation',null],['Resolved','Outcome'],['Closed','Outcome']])
   await call('aepm_save_anonymous_investigation',[next.report_id,'Private note '+state,state,resolution]);
 assert.equal((await call('aepm_anonymous_investigation_history',[next.report_id])).length,5);checks++;
 await login(employee);
 const tracked=(await call('aepm_track_anonymous_report',[next.reference_number]))[0];
 assert.equal(tracked.case_status,'Closed');assert.equal(tracked.resolution,'Outcome');checks+=2;
 assert.ok(!('investigation_note' in tracked));checks++;
 await login(missingProfile);
 await error('42501',()=>call('aepm_anonymous_require_employee'));
 await error('42501',()=>call('aepm_anonymous_require_admin'));
 await db.query("update public.user_tbl set account_status=null where user_id=$1",[employee]);await login(employee);
 await error('42501',()=>call('aepm_anonymous_require_employee'));
 console.log('PASS: '+checks+' deployed-contract SQL regression checks; original 42883/42702 reproduced, 003 fixes verified; no live access.');
} catch(e) {console.error('FAIL: SQLSTATE '+(e.code||'unavailable')+' '+e.message);process.exitCode=1;} finally {await db.close();}
