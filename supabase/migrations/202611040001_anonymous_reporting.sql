-- Execute in the Supabase SQL Editor as the database owner, after reviewing docs/anonymous-reporting.md.
-- New module only. Intentionally fails on conflicting existing tables instead of guessing their schema.
begin;

create schema aepm_anonymous_private;
revoke all on schema aepm_anonymous_private from public, anon, authenticated;
grant usage on schema aepm_anonymous_private to authenticated;

create table public.anonymous_report_tbl (
    report_id uuid primary key default gen_random_uuid(),
    category text not null check (length(category) between 1 and 100 and category ~ '[^[:space:]]'),
    subject text not null check (length(subject) between 1 and 200 and subject ~ '[^[:space:]]'),
    description text not null check (length(description) between 1 and 10000 and description ~ '[^[:space:]]'),
    evidence_path text check (length(evidence_path) <= 2048),
    status text not null default 'Submitted' check (status in ('Submitted','Under Review','Investigating','Resolved','Closed')),
    resolution text check (length(resolution) <= 10000),
    submitted_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
-- No submitter identity, auth UID, IP address or raw tracking reference is stored.
create table aepm_anonymous_private.report_reference (
    reference_hash text primary key check (reference_hash ~ '^[0-9a-f]{64}$'),
    report_id uuid not null unique references public.anonymous_report_tbl(report_id) on delete cascade
);
create table public.anonymous_report_message_tbl (
    message_id uuid primary key default gen_random_uuid(),
    report_id uuid not null references public.anonymous_report_tbl(report_id) on delete cascade,
    sender_type text not null check (sender_type in ('ANONYMOUS_EMPLOYEE','ADMINISTRATOR')),
    content text not null check (length(content) between 1 and 5000 and content ~ '[^[:space:]]'),
    sent_at timestamptz not null default now()
);
create table public.anonymous_investigation_tbl (
    report_id uuid primary key references public.anonymous_report_tbl(report_id) on delete cascade,
    notes text not null default '' check (length(notes) <= 20000),
    assigned_administrator uuid references auth.users(id),
    status text not null check (status in ('Submitted','Under Review','Investigating','Resolved','Closed')),
    resolution text check (length(resolution) <= 10000),
    updated_at timestamptz not null default now()
);
create index anonymous_report_review_idx on public.anonymous_report_tbl(submitted_at desc, report_id desc);
create index anonymous_report_filter_idx on public.anonymous_report_tbl(status, category, submitted_at desc);
create index anonymous_report_message_history_idx on public.anonymous_report_message_tbl(report_id, sent_at, message_id);

-- Existing project conventions: user_tbl.user_id matches auth.uid(), role 1 admin, role 3 employee.
create function aepm_anonymous_private.has_role(p_role bigint) returns boolean
language sql stable security definer set search_path = '' as $$
    select exists(select 1 from public.user_tbl u
        where u.user_id::text = auth.uid()::text and u.role_id = p_role
          and lower(u.account_status) = 'active');
$$;
revoke all on function aepm_anonymous_private.has_role(bigint) from public, anon, authenticated;
grant execute on function aepm_anonymous_private.has_role(bigint) to authenticated;

alter table public.anonymous_report_tbl enable row level security;
alter table public.anonymous_report_message_tbl enable row level security;
alter table public.anonymous_investigation_tbl enable row level security;
alter table aepm_anonymous_private.report_reference enable row level security;
revoke all on public.anonymous_report_tbl, public.anonymous_report_message_tbl,
    public.anonymous_investigation_tbl, aepm_anonymous_private.report_reference from public, anon, authenticated;
grant select on public.anonymous_report_tbl, public.anonymous_report_message_tbl,
    public.anonymous_investigation_tbl to authenticated;
create policy anonymous_report_admin_read on public.anonymous_report_tbl for select to authenticated
    using (aepm_anonymous_private.has_role(1));
create policy anonymous_message_admin_read on public.anonymous_report_message_tbl for select to authenticated
    using (aepm_anonymous_private.has_role(1));
create policy anonymous_investigation_admin_read on public.anonymous_investigation_tbl for select to authenticated
    using (aepm_anonymous_private.has_role(1));
-- Employees and managers have no direct table policies. Writes only via narrowly scoped RPCs.
-- The reference table has no client policy or grants, including for application administrators.

create function aepm_anonymous_private.require_role(p_role bigint) returns void
language plpgsql stable security definer set search_path = '' as $$
begin
    if not aepm_anonymous_private.has_role(p_role) then
        raise exception 'Anonymous reporting access denied.' using errcode = '42501';
    end if;
end;
$$;
create function aepm_anonymous_private.clean_text(p_value text, p_max integer, p_required boolean) returns text
language plpgsql immutable set search_path = '' as $$
begin
    if p_value is null or p_value !~ '[^[:space:]]' then
        if p_required then raise exception 'Required field is blank.' using errcode = '22023'; end if;
        return null;
    end if;
    if length(p_value) > p_max then raise exception 'Field is too long.' using errcode = '22023'; end if;
    return btrim(p_value);
end;
$$;
create function aepm_anonymous_private.reference_id(p_reference text) returns uuid
language sql stable security definer set search_path = '' as $$
    select k.report_id from aepm_anonymous_private.report_reference k
    where p_reference ~ '^[A-Za-z0-9_-]{43}$'
      and k.reference_hash = encode(sha256(convert_to(p_reference, 'UTF8')), 'hex');
$$;

create function public.aepm_submit_anonymous_report(p_reference text, p_category text, p_subject text,
    p_description text, p_evidence_path text default null) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_report public.anonymous_report_tbl;
begin
    perform aepm_anonymous_private.require_role(3);
    if p_reference is null or p_reference !~ '^[A-Za-z0-9_-]{43}$' then
        raise exception 'Invalid anonymous reference.' using errcode = '22023';
    end if;
    insert into public.anonymous_report_tbl(category, subject, description, evidence_path)
    values (aepm_anonymous_private.clean_text(p_category,100,true),
        aepm_anonymous_private.clean_text(p_subject,200,true),
        aepm_anonymous_private.clean_text(p_description,10000,true),
        aepm_anonymous_private.clean_text(p_evidence_path,2048,false)) returning * into v_report;
    insert into aepm_anonymous_private.report_reference(reference_hash, report_id)
    values (encode(sha256(convert_to(p_reference,'UTF8')),'hex'),v_report.report_id);
    return to_jsonb(v_report);
end;
$$;
create function public.aepm_track_anonymous_report(p_reference text) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_result jsonb;
begin
    perform aepm_anonymous_private.require_role(3);
    select to_jsonb(r) into v_result from public.anonymous_report_tbl r
      where r.report_id = aepm_anonymous_private.reference_id(p_reference);
    return v_result;
end;
$$;
create function public.aepm_list_anonymous_reports(p_status text default null, p_category text default null,
    p_from date default null, p_through date default null, p_limit integer default 50, p_offset integer default 0)
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare v_result jsonb;
begin
    perform aepm_anonymous_private.require_role(1);
    if p_limit is null or p_limit < 1 or p_limit > 100 or p_offset is null or p_offset < 0
       or (p_from is not null and p_through is not null and p_from > p_through)
       or (p_status is not null and p_status not in ('Submitted','Under Review','Investigating','Resolved','Closed')) then
        raise exception 'Invalid report filters.' using errcode = '22023';
    end if;
    with matching as materialized (
        select r.* from public.anonymous_report_tbl r
        where (p_status is null or r.status = p_status)
          and (p_category is null or r.category = p_category)
          and (p_from is null or r.submitted_at >= (p_from::timestamp at time zone 'Asia/Kuala_Lumpur'))
          and (p_through is null or r.submitted_at < ((p_through + 1)::timestamp at time zone 'Asia/Kuala_Lumpur'))
    ), page as (select * from matching order by submitted_at desc, report_id desc limit p_limit offset p_offset)
    select jsonb_build_object('total_count',(select count(*) from matching),
        'rows',coalesce((select jsonb_agg(to_jsonb(p) order by p.submitted_at desc,p.report_id desc) from page p),'[]'::jsonb))
      into v_result;
    return v_result;
end;
$$;
create function public.aepm_get_anonymous_report(p_report_id uuid) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_result jsonb;
begin
    perform aepm_anonymous_private.require_role(1);
    select to_jsonb(r) into v_result from public.anonymous_report_tbl r where r.report_id = p_report_id;
    return v_result;
end;
$$;

create function public.aepm_employee_report_messages(p_reference text) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_id uuid; v_result jsonb;
begin
    perform aepm_anonymous_private.require_role(3);
    v_id := aepm_anonymous_private.reference_id(p_reference);
    if v_id is null then raise exception 'Report unavailable.' using errcode = 'P0002'; end if;
    select coalesce(jsonb_agg(to_jsonb(m) order by m.sent_at,m.message_id),'[]'::jsonb)
      into v_result from public.anonymous_report_message_tbl m where m.report_id = v_id;
    return v_result;
end;
$$;
create function public.aepm_admin_report_messages(p_report_id uuid) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_result jsonb;
begin
    perform aepm_anonymous_private.require_role(1);
    if not exists(select 1 from public.anonymous_report_tbl where report_id = p_report_id) then
        raise exception 'Report unavailable.' using errcode = 'P0002';
    end if;
    select coalesce(jsonb_agg(to_jsonb(m) order by m.sent_at,m.message_id),'[]'::jsonb)
      into v_result from public.anonymous_report_message_tbl m where m.report_id = p_report_id;
    return v_result;
end;
$$;
-- Internal helper; caller chooses the sender type after authorization, never from employee input.
create function aepm_anonymous_private.send_message(p_report_id uuid, p_sender text, p_content text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_status text; v_message public.anonymous_report_message_tbl;
begin
    select status into v_status from public.anonymous_report_tbl where report_id = p_report_id for update;
    if not found then raise exception 'Report unavailable.' using errcode = 'P0002'; end if;
    if v_status = 'Closed' then raise exception 'Closed report cannot receive messages.' using errcode = '22023'; end if;
    insert into public.anonymous_report_message_tbl(report_id,sender_type,content)
      values (p_report_id,p_sender,aepm_anonymous_private.clean_text(p_content,5000,true)) returning * into v_message;
    update public.anonymous_report_tbl set updated_at = clock_timestamp() where report_id = p_report_id;
    return to_jsonb(v_message);
end;
$$;
create function public.aepm_send_employee_report_message(p_reference text,p_content text) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
    perform aepm_anonymous_private.require_role(3);
    return aepm_anonymous_private.send_message(aepm_anonymous_private.reference_id(p_reference),'ANONYMOUS_EMPLOYEE',p_content);
end;
$$;
create function public.aepm_send_admin_report_message(p_report_id uuid,p_content text) returns jsonb
language plpgsql security definer set search_path = '' as $$
begin
    perform aepm_anonymous_private.require_role(1);
    return aepm_anonymous_private.send_message(p_report_id,'ADMINISTRATOR',p_content);
end;
$$;
create function public.aepm_get_anonymous_investigation(p_report_id uuid) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare v_result jsonb;
begin
    perform aepm_anonymous_private.require_role(1);
    select to_jsonb(i) into v_result from public.anonymous_investigation_tbl i where i.report_id = p_report_id;
    return v_result;
end;
$$;
create function public.aepm_save_anonymous_investigation(p_report_id uuid,p_expected_status text,
    p_status text,p_notes text,p_assigned_administrator uuid default null,p_resolution text default null) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare v_current text; v_resolution text; v_notes text; v_investigation public.anonymous_investigation_tbl;
    v_workflow text[] := array['Submitted','Under Review','Investigating','Resolved','Closed'];
begin
    perform aepm_anonymous_private.require_role(1);
    select status into v_current from public.anonymous_report_tbl where report_id = p_report_id for update;
    if not found then raise exception 'Report unavailable.' using errcode = 'P0002'; end if;
    if p_expected_status is null or v_current <> p_expected_status then
        raise exception 'Report status changed. Reload before updating.' using errcode = '40001';
    end if;
    if p_status is null or not (p_status = any(v_workflow)) or v_current = 'Closed'
       or (p_status <> v_current and array_position(v_workflow,p_status) <> array_position(v_workflow,v_current)+1) then
        raise exception 'Invalid investigation status transition.' using errcode = '22023';
    end if;
    if p_assigned_administrator is not null and not exists(select 1 from public.user_tbl u
        where u.user_id::text = p_assigned_administrator::text and u.role_id = 1 and lower(u.account_status) = 'active') then
        raise exception 'Assignee must be an active administrator.' using errcode = '22023';
    end if;
    v_resolution := aepm_anonymous_private.clean_text(p_resolution,10000,false);
    v_notes := coalesce(aepm_anonymous_private.clean_text(p_notes,20000,false),'');
    if p_status in ('Resolved','Closed') and v_resolution is null then
        raise exception 'Resolution is required.' using errcode = '22023';
    end if;
    insert into public.anonymous_investigation_tbl(report_id,notes,assigned_administrator,status,resolution,updated_at)
      values(p_report_id,v_notes,p_assigned_administrator,p_status,v_resolution,clock_timestamp())
      on conflict(report_id) do update set notes=excluded.notes,assigned_administrator=excluded.assigned_administrator,
        status=excluded.status,resolution=excluded.resolution,updated_at=excluded.updated_at returning * into v_investigation;
    update public.anonymous_report_tbl set status=p_status,
      resolution=case when p_status in ('Resolved','Closed') then v_resolution else null end,
      updated_at=v_investigation.updated_at where report_id=p_report_id;
    return to_jsonb(v_investigation);
end;
$$;

-- Revoke the PostgreSQL default PUBLIC execute privilege, including all internal helpers.
revoke all on function aepm_anonymous_private.require_role(bigint),
    aepm_anonymous_private.clean_text(text,integer,boolean), aepm_anonymous_private.reference_id(text),
    aepm_anonymous_private.send_message(uuid,text,text) from public, anon, authenticated;
-- Scope grants to this module only; never alter existing tables, policies or global default privileges.
revoke all on function public.aepm_submit_anonymous_report(text,text,text,text,text),
    public.aepm_track_anonymous_report(text),
    public.aepm_list_anonymous_reports(text,text,date,date,integer,integer),
    public.aepm_get_anonymous_report(uuid), public.aepm_employee_report_messages(text),
    public.aepm_admin_report_messages(uuid), public.aepm_send_employee_report_message(text,text),
    public.aepm_send_admin_report_message(uuid,text), public.aepm_get_anonymous_investigation(uuid),
    public.aepm_save_anonymous_investigation(uuid,text,text,text,uuid,text) from public, anon, authenticated;
grant execute on function public.aepm_submit_anonymous_report(text,text,text,text,text),
    public.aepm_track_anonymous_report(text),
    public.aepm_list_anonymous_reports(text,text,date,date,integer,integer),
    public.aepm_get_anonymous_report(uuid), public.aepm_employee_report_messages(text),
    public.aepm_admin_report_messages(uuid), public.aepm_send_employee_report_message(text,text),
    public.aepm_send_admin_report_message(uuid,text), public.aepm_get_anonymous_investigation(uuid),
    public.aepm_save_anonymous_investigation(uuid,text,text,text,uuid,text) to authenticated;
notify pgrst, 'reload schema';
commit;
