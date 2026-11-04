-- POST-ALIGNMENT VERIFICATION. Read-only; run as the database owner in Supabase SQL Editor.
-- Raises an exception on a failed assertion. Catalog queries below show the actual deployed definitions.
-- It prints no plaintext references, hashes, report contents or investigation notes.
begin transaction read only;
do $verify$
declare e record; t text; role_name text; sig text; function_oid oid; v_sequence text;
begin
    for e in select * from (values
      ('anonymous_report_tbl','report_id','uuid',true),
      ('anonymous_report_tbl','reference_hash','text',true),
      ('anonymous_report_tbl','report_category','varchar',true),
      ('anonymous_report_tbl','subject','varchar',true),
      ('anonymous_report_tbl','report_description','text',true),
      ('anonymous_report_tbl','evidence_file','text',false),
      ('anonymous_report_tbl','case_status','varchar',true),
      ('anonymous_report_tbl','resolution','text',false),
      ('anonymous_report_tbl','submitted_at','timestamptz',true),
      ('anonymous_report_tbl','updated_at','timestamptz',true),
      ('anonymous_message_tbl','message_id','int8',true),
      ('anonymous_message_tbl','report_id','uuid',false),
      ('anonymous_message_tbl','sender_type','varchar',true),
      ('anonymous_message_tbl','sender_admin_id','uuid',false),
      ('anonymous_message_tbl','message_text','text',true),
      ('anonymous_message_tbl','sent_at','timestamptz',true),
      ('anonymous_message_tbl','is_read','bool',true),
      ('investigation_tbl','investigation_id','int8',true),
      ('investigation_tbl','report_id','uuid',false),
      ('investigation_tbl','administrator_id','uuid',false),
      ('investigation_tbl','investigation_note','text',false),
      ('investigation_tbl','previous_status','varchar',false),
      ('investigation_tbl','new_status','varchar',false),
      ('investigation_tbl','created_at','timestamptz',true),
      ('investigation_tbl','updated_at','timestamptz',true),
      ('investigation_tbl','resolution','text',false)
    ) expected(table_name,column_name,type_name,required) loop
        if not exists(select 1 from information_schema.columns c where c.table_schema='public'
            and c.table_name=e.table_name and c.column_name=e.column_name and c.udt_name=e.type_name
            and (not e.required or c.is_nullable='NO')) then
            raise exception 'Missing/incompatible expected column: %.%',e.table_name,e.column_name;
        end if;
    end loop;
    if exists(select 1 from information_schema.columns where table_schema='public' and table_name='anonymous_report_tbl'
        and column_name='reference_number') then raise exception 'Plaintext reference_number still exists.'; end if;
    if exists(select 1 from public.anonymous_report_tbl where reference_hash is null or reference_hash !~ '^[0-9a-f]{64}$') then
        raise exception 'Reference hash backfill is incomplete or invalid.';
    end if;
    if exists(select 1 from information_schema.columns where table_schema='public'
        and table_name in ('anonymous_report_tbl','anonymous_message_tbl','investigation_tbl')
        and column_name in ('user_id','employee_id','email','username','full_name')) then
        raise exception 'Unexpected identifying column present; review schema privacy.';
    end if;
    foreach t in array array['anonymous_report_tbl','anonymous_message_tbl','investigation_tbl'] loop
        if not exists(select 1 from pg_class where oid=to_regclass('public.'||t) and relrowsecurity) then
            raise exception 'RLS is not enabled on %.',t;
        end if;
        if not exists(select 1 from pg_policy where polrelid=to_regclass('public.'||t)
            and polname='aepm_anonymous_rpc_only' and not polpermissive and polcmd='*'
            and polroles='{0}'::oid[] and pg_get_expr(polqual,polrelid)='false'
            and pg_get_expr(polwithcheck,polrelid)='false') then
            raise exception 'Restrictive RPC-only policy missing on %.',t;
        end if;
        foreach role_name in array array['anon','authenticated'] loop
            if has_table_privilege(role_name,'public.'||t,'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
              or has_any_column_privilege(role_name,'public.'||t,'SELECT,INSERT,UPDATE,REFERENCES') then
                raise exception 'Unexpected direct table/column privileges for % on %.',role_name,t;
            end if;
        end loop;
        if not exists(select 1 from pg_constraint where conrelid=to_regclass('public.'||t) and contype='p') then
            raise exception 'Primary key missing on %.',t;
        end if;
    end loop;
    for e in select * from (values
       ('anonymous_message_tbl','report_id','anonymous_report_tbl'),
       ('anonymous_message_tbl','sender_admin_id','user_tbl'),
       ('investigation_tbl','report_id','anonymous_report_tbl'),
       ('investigation_tbl','administrator_id','user_tbl')) expected(table_name,column_name,target_table)
    loop
        if not exists(select 1 from pg_constraint c join pg_attribute a
            on a.attrelid=c.conrelid and a.attnum=any(c.conkey)
            where c.conrelid=to_regclass('public.'||e.table_name) and c.contype='f'
              and c.confrelid=to_regclass('public.'||e.target_table) and a.attname=e.column_name) then
            raise exception 'Expected FK missing: %.% -> %.',e.table_name,e.column_name,e.target_table;
        end if;
    end loop;
    for e in select * from (values
      ('anonymous_report_tbl','aepm_reference_hash_format'),('anonymous_report_tbl','aepm_subject_nonblank'),
      ('anonymous_report_tbl','aepm_case_status_allowed'),('anonymous_message_tbl','aepm_sender_type_allowed'),
      ('investigation_tbl','aepm_previous_status_allowed'),('investigation_tbl','aepm_new_status_allowed')) expected(table_name,constraint_name)
    loop
        if not exists(select 1 from pg_constraint where conrelid=to_regclass('public.'||e.table_name)
            and conname=e.constraint_name and contype='c' and convalidated) then
            raise exception 'Validated constraint missing: %.',e.constraint_name;
        end if;
    end loop;
    foreach t in array array['aepm_reference_hash_uidx','aepm_report_review_idx','aepm_report_filter_idx',
        'aepm_message_history_idx','aepm_investigation_history_idx'] loop
        if not exists(select 1 from pg_index where indexrelid=to_regclass('public.'||t) and indisvalid) then
            raise exception 'Required index missing or invalid: %.',t;
        end if;
    end loop;
    if not exists(select 1 from pg_index where indexrelid='public.aepm_reference_hash_uidx'::regclass and indisunique) then
        raise exception 'Reference hash index is not unique.';
    end if;
    foreach t in array array['anonymous_message_tbl','investigation_tbl'] loop
        v_sequence:=pg_get_serial_sequence('public.'||t,case when t='anonymous_message_tbl' then 'message_id' else 'investigation_id' end);
        if v_sequence is null then raise exception 'Expected identity sequence missing on %.',t; end if;
        if not exists(select 1 from information_schema.columns where table_schema='public' and table_name=t
            and column_name=case when t='anonymous_message_tbl' then 'message_id' else 'investigation_id' end and is_identity='YES') then
            raise exception 'Identity ID is missing on %.',t;
        end if;
        foreach role_name in array array['anon','authenticated'] loop
            if has_sequence_privilege(role_name,v_sequence,'USAGE,SELECT,UPDATE') then
                raise exception 'Unexpected sequence privilege for % on %.',role_name,v_sequence;
            end if;
        end loop;
    end loop;
    for e in select * from (values ('anonymous_report_tbl','aepm_report_touch_updated_at'),
        ('investigation_tbl','aepm_investigation_touch_updated_at')) expected(table_name,trigger_name) loop
        if not exists(select 1 from pg_trigger where tgrelid=to_regclass('public.'||e.table_name)
          and tgname=e.trigger_name and not tgisinternal and tgenabled in ('O','A')
          and (tgtype::integer & 19)=19
          and tgfoid='aepm_anonymous_private.touch_updated_at()'::regprocedure) then
            raise exception 'Expected BEFORE ROW UPDATE timestamp trigger missing on %.',e.table_name;
        end if;
    end loop;
    foreach sig in array array['public.aepm_submit_anonymous_report(text,text,text,text,text)',
        'public.aepm_track_anonymous_report(text)',
        'public.aepm_list_anonymous_reports(text,text,date,date,integer,integer)',
        'public.aepm_get_anonymous_report(uuid)',
        'public.aepm_employee_report_messages(text)',
        'public.aepm_admin_report_messages(uuid)',
        'public.aepm_send_employee_report_message(text,text)',
        'public.aepm_send_admin_report_message(uuid,text)',
        'public.aepm_get_anonymous_investigation(uuid)',
        'public.aepm_save_anonymous_investigation(uuid,text,text,text,uuid,text)',
        'public.aepm_anonymous_investigation_history(uuid)'] loop
        function_oid:=to_regprocedure(sig);
        if function_oid is null then raise exception 'Required RPC is missing: %.',sig; end if;
        if not exists(select 1 from pg_proc where oid=function_oid and prosecdef and prorettype='jsonb'::regtype
            and proconfig @> array['search_path=""']) then raise exception 'Unsafe RPC configuration: %.',sig; end if;
        if has_function_privilege('anon',function_oid,'EXECUTE') or not has_function_privilege('authenticated',function_oid,'EXECUTE') then
            raise exception 'Incorrect RPC execute grants: %.',sig;
        end if;
    end loop;
    foreach role_name in array array['anon','authenticated'] loop
        if has_schema_privilege(role_name,'aepm_anonymous_private','USAGE') then raise exception 'Private schema is accessible to %.',role_name; end if;
        if exists(select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='aepm_anonymous_private'
            and has_function_privilege(role_name,p.oid,'EXECUTE')) then raise exception 'Private helper EXECUTE granted to %.',role_name; end if;
    end loop;
    if exists(select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace where p.prokind='f'
        and p.prorettype not in ('trigger'::regtype,'event_trigger'::regtype)
        and n.nspname not in ('pg_catalog','information_schema','aepm_anonymous_private')
        and p.prosrc ~* '(anonymous_report_tbl|anonymous_message_tbl|investigation_tbl|reference_number|reference_hash)'
        and not exists(select 1 from unnest(array['public.aepm_submit_anonymous_report(text,text,text,text,text)',
        'public.aepm_track_anonymous_report(text)',
        'public.aepm_list_anonymous_reports(text,text,date,date,integer,integer)',
        'public.aepm_get_anonymous_report(uuid)',
        'public.aepm_employee_report_messages(text)',
        'public.aepm_admin_report_messages(uuid)',
        'public.aepm_send_employee_report_message(text,text)',
        'public.aepm_send_admin_report_message(uuid,text)',
        'public.aepm_get_anonymous_investigation(uuid)',
        'public.aepm_save_anonymous_investigation(uuid,text,text,text,uuid,text)',
        'public.aepm_anonymous_investigation_history(uuid)']) s(signature) where p.oid=to_regprocedure(s.signature))
        and (has_function_privilege('anon',p.oid,'EXECUTE') or has_function_privilege('authenticated',p.oid,'EXECUTE'))) then
        raise exception 'Unexpected callable legacy module function exists; review before enabling clients.';
    end if;
    for e in
      with recursive dependent_relations(oid) as (
        select unnest(array['public.anonymous_report_tbl'::regclass::oid,
          'public.anonymous_message_tbl'::regclass::oid,'public.investigation_tbl'::regclass::oid])
        union
        select rw.ev_class from dependent_relations base
          join pg_depend d on d.refobjid=base.oid and d.refclassid='pg_class'::regclass
          join pg_rewrite rw on rw.oid=d.objid and d.classid='pg_rewrite'::regclass
      )
      select distinct n.nspname,c.relname from dependent_relations dep
        join pg_class c on c.oid=dep.oid join pg_namespace n on n.oid=c.relnamespace
      where c.relkind in ('v','m')
        and (has_table_privilege('anon',c.oid,'SELECT') or has_any_column_privilege('anon',c.oid,'SELECT')
          or has_table_privilege('authenticated',c.oid,'SELECT') or has_any_column_privilege('authenticated',c.oid,'SELECT'))
    loop
        raise exception 'Exposed dependent view %.% can bypass RPC-only privacy.',e.nspname,e.relname;
    end loop;
    raise notice 'PASS: schema, keys, constraints, indexes, RLS, policies, timestamp triggers, RPCs and effective grants.';
end;
$verify$;

select table_name,column_name,data_type,is_nullable,column_default,is_identity,character_maximum_length
from information_schema.columns where table_schema='public'
  and table_name in ('anonymous_report_tbl','anonymous_message_tbl','investigation_tbl') order by table_name,ordinal_position;
select conrelid::regclass as table_name,conname,contype,convalidated,pg_get_constraintdef(oid) as definition
from pg_constraint where conrelid in ('public.anonymous_report_tbl'::regclass,'public.anonymous_message_tbl'::regclass,'public.investigation_tbl'::regclass)
order by table_name,conname;
select tablename,indexname,indexdef from pg_indexes where schemaname='public'
  and tablename in ('anonymous_report_tbl','anonymous_message_tbl','investigation_tbl') order by tablename,indexname;
select schemaname,tablename,policyname,permissive,roles,cmd,qual,with_check from pg_policies
  where schemaname='public' and tablename in ('anonymous_report_tbl','anonymous_message_tbl','investigation_tbl') order by tablename,policyname;
select p.oid::regprocedure as routine,p.prosecdef,p.proconfig,
  has_function_privilege('anon',p.oid,'EXECUTE') as anon_execute,
  has_function_privilege('authenticated',p.oid,'EXECUTE') as authenticated_execute
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where (n.nspname='public' and p.proname like 'aepm_%anonymous%') or n.nspname='aepm_anonymous_private' order by routine;
-- Inventory remaining dependencies/legacy entry points before deployment. Review dynamic SQL/wrappers manually.
select n.nspname,p.oid::regprocedure as routine,pg_get_functiondef(p.oid) as definition
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where p.prokind='f' and n.nspname not in ('pg_catalog','information_schema')
  and p.prosrc ~* '(anonymous_report_tbl|anonymous_message_tbl|investigation_tbl|reference_number|reference_hash)' order by routine;
rollback;
