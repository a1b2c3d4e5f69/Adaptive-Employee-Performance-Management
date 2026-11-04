-- LIVE fixes for the applied migration 002. Do NOT modify or rerun 002.
-- No table/column changes, data updates/deletions, policy weakening or token rotation.
-- 42883 reproduced live: digest(text,unknown) is not visible under search_path=public.
-- Built-in SHA-256 produces exactly the same lowercase hex hashes over exact UTF-8 references.
-- Run once in Supabase SQL Editor as the database owner, then rerun the Java live suite.
begin;

create or replace function public.aepm_anonymous_reference_hash(p_reference text)
returns text language plpgsql immutable security definer set search_path='' as $function$
begin
    if p_reference is null or length(p_reference)=0 or length(p_reference)>50 then return null; end if;
    return pg_catalog.encode(pg_catalog.sha256(pg_catalog.convert_to(p_reference,'UTF8')),'hex');
end;
$function$;

-- A missing/inactive profile returns NULL. IS DISTINCT FROM denies that state instead of letting NULL skip IF.
create or replace function public.aepm_anonymous_current_role_id()
returns integer language sql stable security definer set search_path='' as $function$
    select u.role_id::integer from public.user_tbl u
    where u.user_id=auth.uid() and lower(u.account_status)='active' limit 1;
$function$;
create or replace function public.aepm_anonymous_require_employee()
returns void language plpgsql security definer set search_path='' as $function$
begin
    if auth.uid() is null or public.aepm_anonymous_current_role_id() is distinct from 3 then
        raise exception 'Employee access required.' using errcode='42501';
    end if;
end;
$function$;
create or replace function public.aepm_anonymous_require_admin()
returns void language plpgsql security definer set search_path='' as $function$
begin
    if auth.uid() is null or public.aepm_anonymous_current_role_id() is distinct from 1 then
        raise exception 'Administrator access required.' using errcode='42501';
    end if;
end;
$function$;

-- Qualify columns in UPDATE: RETURNS TABLE output variables also include report_id/resolution.
-- Keep the existing four parameter names and returned columns so deployed clients remain compatible.
create or replace function public.aepm_save_anonymous_investigation(
    p_report_id uuid,
    p_investigation_note text default null,
    p_new_status text default null,
    p_resolution text default null
)
returns table (
    investigation_id bigint,
    report_id uuid,
    previous_status text,
    new_status text,
    resolution text,
    created_at timestamptz,
    updated_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_old_status text;
    v_new_status text;
    v_resolution text;
    v_investigation_id bigint;
begin

    perform public.aepm_anonymous_require_admin();


    select r.case_status
    into v_old_status
    from public.anonymous_report_tbl r
    where r.report_id = p_report_id
    for update;


    if v_old_status is null then

        raise exception
            'Anonymous report was not found.';

    end if;


    v_new_status :=
        coalesce(
            nullif(
                btrim(p_new_status),
                ''
            ),
            v_old_status
        );


    if v_new_status not in (
        'Submitted',
        'Under Review',
        'Under Investigation',
        'Resolved',
        'Closed'
    ) then

        raise exception
            'Invalid anonymous report status.';

    end if;


    if not public.aepm_anonymous_valid_transition(
        v_old_status,
        v_new_status
    ) then

        raise exception
            'Invalid case status transition: % -> %.',
            v_old_status,
            v_new_status;

    end if;


    v_resolution :=
        nullif(
            btrim(
                coalesce(
                    p_resolution,
                    ''
                )
            ),
            ''
        );


    if v_new_status = 'Resolved'
       and v_resolution is null then

        select r.resolution
        into v_resolution
        from public.anonymous_report_tbl r
        where r.report_id = p_report_id;


        if v_resolution is null
           or btrim(v_resolution) = '' then

            raise exception
                'A resolution is required before marking report as Resolved.';

        end if;

    end if;


    if v_new_status = 'Closed'
       and v_resolution is null then

        select r.resolution
        into v_resolution
        from public.anonymous_report_tbl r
        where r.report_id = p_report_id;

    end if;


    insert into public.investigation_tbl (
        report_id,
        administrator_id,
        investigation_note,
        previous_status,
        new_status,
        resolution
    )
    values (
        p_report_id,
        auth.uid(),

        nullif(
            btrim(
                coalesce(
                    p_investigation_note,
                    ''
                )
            ),
            ''
        ),

        v_old_status,
        v_new_status,
        v_resolution
    )
    returning investigation_tbl.investigation_id
    into v_investigation_id;


    update public.anonymous_report_tbl as target_report
    set
        case_status = v_new_status,

        resolution =
            case

                when v_new_status in (
                    'Resolved',
                    'Closed'
                )
                then coalesce(
                    v_resolution,
                    target_report.resolution
                )

                else target_report.resolution

            end

    where target_report.report_id = p_report_id;


    return query

    select
        i.investigation_id,
        i.report_id,
        i.previous_status::text,
        i.new_status::text,
        i.resolution,
        i.created_at,
        i.updated_at

    from public.investigation_tbl i

    where i.investigation_id =
        v_investigation_id;

end;
$function$;

-- Preserve read receipts while disambiguating sender_type/is_read output variables.
create or replace function public.aepm_employee_report_messages(
    p_reference_number text
)
returns table (
    message_id bigint,
    sender_type text,
    message_text text,
    sent_at timestamptz,
    is_read boolean
)
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_hash text;
    v_report_id uuid;
begin

    perform public.aepm_anonymous_require_employee();


    v_hash :=
        public.aepm_anonymous_reference_hash(
            p_reference_number
        );


    if v_hash is null then
        return;
    end if;


    select r.report_id
    into v_report_id
    from public.anonymous_report_tbl r
    where r.reference_hash = v_hash
    limit 1;


    if v_report_id is null then
        return;
    end if;


    update public.anonymous_message_tbl as target_message
    set is_read = true
    where target_message.report_id = v_report_id
      and target_message.sender_type = 'Administrator'
      and target_message.is_read = false;


    return query

    select
        m.message_id,
        m.sender_type::text,
        m.message_text,
        m.sent_at,
        m.is_read

    from public.anonymous_message_tbl m

    where m.report_id = v_report_id

    order by
        m.sent_at asc,
        m.message_id asc;

end;
$function$;

create or replace function public.aepm_admin_report_messages(
    p_report_id uuid
)
returns table (
    message_id bigint,
    sender_type text,
    message_text text,
    sent_at timestamptz,
    is_read boolean
)
language plpgsql
security definer
set search_path = ''
as $function$
begin

    perform public.aepm_anonymous_require_admin();


    update public.anonymous_message_tbl as target_message
    set is_read = true
    where target_message.report_id = p_report_id
      and target_message.sender_type = 'Anonymous Reporter'
      and target_message.is_read = false;


    return query

    select
        m.message_id,
        m.sender_type::text,
        m.message_text,
        m.sent_at,
        m.is_read

    from public.anonymous_message_tbl m

    where m.report_id = p_report_id

    order by
        m.sent_at asc,
        m.message_id asc;

end;
$function$;

-- Internal helpers stay unavailable as direct client RPCs; owner-executed business RPCs can call them.
revoke execute on function public.aepm_anonymous_reference_hash(text),
    public.aepm_anonymous_current_role_id(),public.aepm_anonymous_require_employee(),
    public.aepm_anonymous_require_admin() from public,anon,authenticated;
revoke execute on function public.aepm_save_anonymous_investigation(uuid,text,text,text) from public,anon;
grant execute on function public.aepm_save_anonymous_investigation(uuid,text,text,text) to authenticated;
revoke execute on function public.aepm_employee_report_messages(text),
    public.aepm_admin_report_messages(uuid) from public,anon;
grant execute on function public.aepm_employee_report_messages(text),
    public.aepm_admin_report_messages(uuid) to authenticated;
notify pgrst,'reload schema';
commit;
