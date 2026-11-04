-- ============================================================
-- AEPM
-- Anonymous Reporting Schema Alignment
--
-- Migration:
-- 202611040002_anonymous_reporting_schema_alignment.sql
--
-- Existing live tables:
--   public.anonymous_report_tbl
--   public.anonymous_message_tbl
--   public.investigation_tbl
--
-- IMPORTANT:
-- - Existing tables are preserved.
-- - Existing rows are preserved.
-- - No UI/FXML/CSS changes.
-- - Plaintext reference_number is converted to SHA-256 hash.
-- ============================================================

begin;


-- ============================================================
-- 1. PGCRYPTO
-- ============================================================

create extension if not exists pgcrypto;


-- ============================================================
-- 2. DROP OLD ANONYMOUS REPORTING RPC FUNCTIONS
-- ============================================================

do $drop_old_rpc$
declare
    r record;
begin

    for r in
        select
            p.oid::regprocedure as function_signature
        from pg_proc p
        join pg_namespace n
            on n.oid = p.pronamespace
        where n.nspname = 'public'
          and p.proname = any (
              array[
                  'aepm_submit_anonymous_report',
                  'aepm_track_anonymous_report',
                  'aepm_employee_report_messages',
                  'aepm_send_employee_report_message',
                  'aepm_list_anonymous_reports',
                  'aepm_get_anonymous_report',
                  'aepm_admin_report_messages',
                  'aepm_send_admin_report_message',
                  'aepm_get_anonymous_investigation',
                  'aepm_save_anonymous_investigation',
                  'aepm_anonymous_investigation_history'
              ]
          )
    loop

        execute
            'drop function if exists '
            || r.function_signature::text;

    end loop;

end;
$drop_old_rpc$;


-- ============================================================
-- 3. ALIGN anonymous_report_tbl
-- ============================================================

alter table public.anonymous_report_tbl
    add column if not exists subject varchar(200);


update public.anonymous_report_tbl
set subject = 'Legacy Anonymous Report'
where subject is null
   or btrim(subject) = '';


alter table public.anonymous_report_tbl
    alter column subject set not null;


-- ============================================================
-- 4. ADD REFERENCE HASH
-- ============================================================

alter table public.anonymous_report_tbl
    add column if not exists reference_hash varchar(64);


-- ============================================================
-- 5. CONVERT EXISTING REFERENCE NUMBER TO HASH
-- ============================================================

do $reference_conversion$
begin

    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'anonymous_report_tbl'
          and column_name = 'reference_number'
    ) then

        update public.anonymous_report_tbl
        set reference_hash =
            encode(
                digest(
                    reference_number::text,
                    'sha256'
                ),
                'hex'
            )
        where reference_hash is null;

    end if;


    if exists (
        select 1
        from public.anonymous_report_tbl
        where reference_hash is null
           or length(reference_hash) <> 64
    ) then

        raise exception
            'Reference conversion failed. Migration rolled back.';

    end if;

end;
$reference_conversion$;


alter table public.anonymous_report_tbl
    alter column reference_hash set not null;


create unique index if not exists
    uq_anonymous_report_reference_hash
on public.anonymous_report_tbl (
    reference_hash
);


-- ============================================================
-- 6. ADD PUBLIC RESOLUTION
-- ============================================================

alter table public.anonymous_report_tbl
    add column if not exists resolution text;


-- ============================================================
-- 7. STANDARDISE REPORT STATUS
-- ============================================================

update public.anonymous_report_tbl
set case_status = 'Under Investigation'
where case_status = 'Investigating';


alter table public.anonymous_report_tbl
    drop constraint if exists
    anonymous_report_tbl_case_status_check;


alter table public.anonymous_report_tbl
    add constraint anonymous_report_tbl_case_status_check
    check (
        case_status in (
            'Submitted',
            'Under Review',
            'Under Investigation',
            'Resolved',
            'Closed'
        )
    );


alter table public.anonymous_report_tbl
    alter column case_status
    set default 'Submitted';


-- ============================================================
-- 8. REPORT INDEXES
-- ============================================================

create index if not exists
    idx_anonymous_report_status
on public.anonymous_report_tbl (
    case_status
);


create index if not exists
    idx_anonymous_report_category
on public.anonymous_report_tbl (
    report_category
);


create index if not exists
    idx_anonymous_report_submitted_at
on public.anonymous_report_tbl (
    submitted_at desc
);


-- ============================================================
-- 9. ALIGN anonymous_message_tbl
-- ============================================================

alter table public.anonymous_message_tbl
    drop constraint if exists
    anonymous_message_tbl_sender_type_check;


alter table public.anonymous_message_tbl
    add constraint anonymous_message_tbl_sender_type_check
    check (
        sender_type in (
            'Anonymous Reporter',
            'Administrator'
        )
    );


create index if not exists
    idx_anonymous_message_report_id
on public.anonymous_message_tbl (
    report_id
);


create index if not exists
    idx_anonymous_message_report_sent_at
on public.anonymous_message_tbl (
    report_id,
    sent_at
);


-- ============================================================
-- 10. ALIGN investigation_tbl
-- ============================================================

alter table public.investigation_tbl
    add column if not exists resolution text;


alter table public.investigation_tbl
    add column if not exists updated_at timestamptz;


update public.investigation_tbl
set updated_at = created_at
where updated_at is null;


alter table public.investigation_tbl
    alter column updated_at
    set default now();


alter table public.investigation_tbl
    alter column updated_at
    set not null;


update public.investigation_tbl
set previous_status = 'Under Investigation'
where previous_status = 'Investigating';


update public.investigation_tbl
set new_status = 'Under Investigation'
where new_status = 'Investigating';


alter table public.investigation_tbl
    drop constraint if exists
    investigation_tbl_previous_status_check;


alter table public.investigation_tbl
    add constraint investigation_tbl_previous_status_check
    check (
        previous_status is null
        or previous_status in (
            'Submitted',
            'Under Review',
            'Under Investigation',
            'Resolved',
            'Closed'
        )
    );


alter table public.investigation_tbl
    drop constraint if exists
    investigation_tbl_new_status_check;


alter table public.investigation_tbl
    add constraint investigation_tbl_new_status_check
    check (
        new_status is null
        or new_status in (
            'Submitted',
            'Under Review',
            'Under Investigation',
            'Resolved',
            'Closed'
        )
    );


create index if not exists
    idx_investigation_report_id
on public.investigation_tbl (
    report_id
);


create index if not exists
    idx_investigation_report_created_at
on public.investigation_tbl (
    report_id,
    created_at desc
);


-- ============================================================
-- 11. UPDATED_AT TRIGGER FUNCTION
-- ============================================================

create or replace function public.aepm_anonymous_set_updated_at()
returns trigger
language plpgsql
security definer
set search_path = public
as $function$
begin

    new.updated_at := now();

    return new;

end;
$function$;


drop trigger if exists
    trg_anonymous_report_updated_at
on public.anonymous_report_tbl;


create trigger trg_anonymous_report_updated_at
before update
on public.anonymous_report_tbl
for each row
execute function public.aepm_anonymous_set_updated_at();


drop trigger if exists
    trg_investigation_updated_at
on public.investigation_tbl;


create trigger trg_investigation_updated_at
before update
on public.investigation_tbl
for each row
execute function public.aepm_anonymous_set_updated_at();


-- ============================================================
-- 12. CURRENT ROLE HELPER
-- ============================================================

create or replace function public.aepm_anonymous_current_role_id()
returns integer
language sql
stable
security definer
set search_path = public
as $function$

    select u.role_id::integer
    from public.user_tbl u
    where u.user_id = auth.uid()
      and coalesce(
          u.account_status,
          'Active'
      ) = 'Active'
    limit 1;

$function$;


-- ============================================================
-- 13. REQUIRE EMPLOYEE
-- ============================================================

create or replace function public.aepm_anonymous_require_employee()
returns void
language plpgsql
security definer
set search_path = public
as $function$
begin

    if auth.uid() is null then

        raise exception
            'Authentication required.';

    end if;


    if public.aepm_anonymous_current_role_id() <> 3 then

        raise exception
            'Employee access required.';

    end if;

end;
$function$;


-- ============================================================
-- 14. REQUIRE ADMIN
-- ============================================================

create or replace function public.aepm_anonymous_require_admin()
returns void
language plpgsql
security definer
set search_path = public
as $function$
begin

    if auth.uid() is null then

        raise exception
            'Authentication required.';

    end if;


    if public.aepm_anonymous_current_role_id() <> 1 then

        raise exception
            'Administrator access required.';

    end if;

end;
$function$;


-- ============================================================
-- 15. REFERENCE HASH HELPER
-- ============================================================

create or replace function public.aepm_anonymous_reference_hash(
    p_reference text
)
returns text
language plpgsql
immutable
security definer
set search_path = public
as $function$
begin

    if p_reference is null
       or length(p_reference) = 0
       or length(p_reference) > 50 then

        return null;

    end if;


    return encode(
        digest(
            p_reference,
            'sha256'
        ),
        'hex'
    );

end;
$function$;


-- ============================================================
-- 16. EMPLOYEE - SUBMIT REPORT
-- ============================================================

create or replace function public.aepm_submit_anonymous_report(
    p_report_category text,
    p_subject text,
    p_report_description text,
    p_evidence_file text default null
)
returns table (
    report_id uuid,
    reference_number text,
    report_category text,
    subject text,
    report_description text,
    evidence_file text,
    case_status text,
    submitted_at timestamptz,
    updated_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $function$
declare
    v_reference text;
    v_hash text;
    v_report_id uuid;
begin

    perform public.aepm_anonymous_require_employee();


    if p_report_category is null
       or btrim(p_report_category) = '' then

        raise exception
            'Report category is required.';

    end if;


    if p_subject is null
       or btrim(p_subject) = '' then

        raise exception
            'Report subject is required.';

    end if;


    if p_report_description is null
       or btrim(p_report_description) = '' then

        raise exception
            'Report description is required.';

    end if;


    if length(btrim(p_report_category)) > 100 then

        raise exception
            'Report category must not exceed 100 characters.';

    end if;


    if length(btrim(p_subject)) > 200 then

        raise exception
            'Report subject must not exceed 200 characters.';

    end if;


    -- UUID + UUID gives a long non-guessable reference.

    v_reference :=
        replace(
            gen_random_uuid()::text
            || gen_random_uuid()::text,
            '-',
            ''
        );


    -- Keep within varchar(50)-compatible reference length.

    v_reference :=
        substring(
            v_reference
            from 1
            for 48
        );


    v_hash :=
        public.aepm_anonymous_reference_hash(
            v_reference
        );


    insert into public.anonymous_report_tbl (
        reference_hash,
        report_category,
        subject,
        report_description,
        evidence_file,
        case_status
    )
    values (
        v_hash,
        btrim(p_report_category),
        btrim(p_subject),
        btrim(p_report_description),

        nullif(
            btrim(
                coalesce(
                    p_evidence_file,
                    ''
                )
            ),
            ''
        ),

        'Submitted'
    )
    returning anonymous_report_tbl.report_id
    into v_report_id;


    return query

    select
        r.report_id,
        v_reference,
        r.report_category::text,
        r.subject::text,
        r.report_description,
        r.evidence_file,
        r.case_status::text,
        r.submitted_at,
        r.updated_at

    from public.anonymous_report_tbl r

    where r.report_id = v_report_id;

end;
$function$;


-- ============================================================
-- 17. EMPLOYEE - TRACK REPORT
-- ============================================================

create or replace function public.aepm_track_anonymous_report(
    p_reference_number text
)
returns table (
    report_id uuid,
    report_category text,
    subject text,
    report_description text,
    evidence_file text,
    case_status text,
    resolution text,
    submitted_at timestamptz,
    updated_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $function$
declare
    v_hash text;
begin

    perform public.aepm_anonymous_require_employee();


    v_hash :=
        public.aepm_anonymous_reference_hash(
            p_reference_number
        );


    if v_hash is null then
        return;
    end if;


    return query

    select
        r.report_id,
        r.report_category::text,
        r.subject::text,
        r.report_description,
        r.evidence_file,
        r.case_status::text,

        case

            when r.case_status in (
                'Resolved',
                'Closed'
            )
            then r.resolution

            else null

        end,

        r.submitted_at,
        r.updated_at

    from public.anonymous_report_tbl r

    where r.reference_hash = v_hash

    limit 1;

end;
$function$;


-- ============================================================
-- 18. EMPLOYEE - GET MESSAGES
-- ============================================================

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
set search_path = public
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


    update public.anonymous_message_tbl
    set is_read = true
    where report_id = v_report_id
      and sender_type = 'Administrator'
      and is_read = false;


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


-- ============================================================
-- 19. EMPLOYEE - SEND MESSAGE
-- ============================================================

create or replace function public.aepm_send_employee_report_message(
    p_reference_number text,
    p_message_text text
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
set search_path = public
as $function$
declare
    v_hash text;
    v_report_id uuid;
    v_message_id bigint;
begin

    perform public.aepm_anonymous_require_employee();


    if p_message_text is null
       or btrim(p_message_text) = '' then

        raise exception
            'Message cannot be empty.';

    end if;


    v_hash :=
        public.aepm_anonymous_reference_hash(
            p_reference_number
        );


    if v_hash is null then

        raise exception
            'Invalid report reference.';

    end if;


    select r.report_id
    into v_report_id
    from public.anonymous_report_tbl r
    where r.reference_hash = v_hash
    limit 1;


    if v_report_id is null then

        raise exception
            'Anonymous report was not found.';

    end if;


    insert into public.anonymous_message_tbl (
        report_id,
        sender_type,
        sender_admin_id,
        message_text,
        is_read
    )
    values (
        v_report_id,
        'Anonymous Reporter',
        null,
        btrim(p_message_text),
        false
    )
    returning anonymous_message_tbl.message_id
    into v_message_id;


    return query

    select
        m.message_id,
        m.sender_type::text,
        m.message_text,
        m.sent_at,
        m.is_read

    from public.anonymous_message_tbl m

    where m.message_id = v_message_id;

end;
$function$;


-- ============================================================
-- 20. ADMIN - LIST REPORTS
-- ============================================================

create or replace function public.aepm_list_anonymous_reports(
    p_status text default null,
    p_category text default null,
    p_start_date date default null,
    p_end_date date default null,
    p_limit integer default 50,
    p_offset integer default 0
)
returns table (
    report_id uuid,
    report_category text,
    subject text,
    report_description text,
    evidence_file text,
    case_status text,
    resolution text,
    submitted_at timestamptz,
    updated_at timestamptz,
    total_count bigint
)
language plpgsql
security definer
set search_path = public
as $function$
begin

    perform public.aepm_anonymous_require_admin();


    if p_limit is null
       or p_limit < 1
       or p_limit > 200 then

        raise exception
            'Page size must be between 1 and 200.';

    end if;


    if p_offset is null
       or p_offset < 0 then

        raise exception
            'Page offset cannot be negative.';

    end if;


    if p_start_date is not null
       and p_end_date is not null
       and p_end_date < p_start_date then

        raise exception
            'Invalid submitted date range.';

    end if;


    return query

    select
        r.report_id,
        r.report_category::text,
        r.subject::text,
        r.report_description,
        r.evidence_file,
        r.case_status::text,
        r.resolution,
        r.submitted_at,
        r.updated_at,
        count(*) over ()

    from public.anonymous_report_tbl r

    where (
        p_status is null
        or btrim(p_status) = ''
        or r.case_status = p_status
    )

    and (
        p_category is null
        or btrim(p_category) = ''
        or lower(r.report_category)
            = lower(btrim(p_category))
    )

    and (
        p_start_date is null
        or r.submitted_at >= p_start_date::timestamptz
    )

    and (
        p_end_date is null
        or r.submitted_at
            < (p_end_date + 1)::timestamptz
    )

    order by
        r.submitted_at desc,
        r.report_id

    limit p_limit
    offset p_offset;

end;
$function$;


-- ============================================================
-- 21. ADMIN - GET REPORT
-- ============================================================

create or replace function public.aepm_get_anonymous_report(
    p_report_id uuid
)
returns table (
    report_id uuid,
    report_category text,
    subject text,
    report_description text,
    evidence_file text,
    case_status text,
    resolution text,
    submitted_at timestamptz,
    updated_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $function$
begin

    perform public.aepm_anonymous_require_admin();


    return query

    select
        r.report_id,
        r.report_category::text,
        r.subject::text,
        r.report_description,
        r.evidence_file,
        r.case_status::text,
        r.resolution,
        r.submitted_at,
        r.updated_at

    from public.anonymous_report_tbl r

    where r.report_id = p_report_id

    limit 1;

end;
$function$;


-- ============================================================
-- 22. ADMIN - GET MESSAGES
-- ============================================================

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
set search_path = public
as $function$
begin

    perform public.aepm_anonymous_require_admin();


    update public.anonymous_message_tbl
    set is_read = true
    where report_id = p_report_id
      and sender_type = 'Anonymous Reporter'
      and is_read = false;


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


-- ============================================================
-- 23. ADMIN - SEND MESSAGE
-- ============================================================

create or replace function public.aepm_send_admin_report_message(
    p_report_id uuid,
    p_message_text text
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
set search_path = public
as $function$
declare
    v_message_id bigint;
begin

    perform public.aepm_anonymous_require_admin();


    if p_message_text is null
       or btrim(p_message_text) = '' then

        raise exception
            'Message cannot be empty.';

    end if;


    if not exists (
        select 1
        from public.anonymous_report_tbl
        where report_id = p_report_id
    ) then

        raise exception
            'Anonymous report was not found.';

    end if;


    insert into public.anonymous_message_tbl (
        report_id,
        sender_type,
        sender_admin_id,
        message_text,
        is_read
    )
    values (
        p_report_id,
        'Administrator',
        auth.uid(),
        btrim(p_message_text),
        false
    )
    returning anonymous_message_tbl.message_id
    into v_message_id;


    return query

    select
        m.message_id,
        m.sender_type::text,
        m.message_text,
        m.sent_at,
        m.is_read

    from public.anonymous_message_tbl m

    where m.message_id = v_message_id;

end;
$function$;


-- ============================================================
-- 24. STATUS TRANSITION
-- ============================================================

create or replace function public.aepm_anonymous_valid_transition(
    p_old_status text,
    p_new_status text
)
returns boolean
language sql
immutable
security definer
set search_path = public
as $function$

    select
        case

            when p_old_status = p_new_status
                then true

            when p_old_status = 'Submitted'
             and p_new_status = 'Under Review'
                then true

            when p_old_status = 'Under Review'
             and p_new_status = 'Under Investigation'
                then true

            when p_old_status = 'Under Investigation'
             and p_new_status = 'Resolved'
                then true

            when p_old_status = 'Resolved'
             and p_new_status = 'Closed'
                then true

            else false

        end;

$function$;


-- ============================================================
-- 25. ADMIN - GET LATEST INVESTIGATION
-- ============================================================

create or replace function public.aepm_get_anonymous_investigation(
    p_report_id uuid
)
returns table (
    investigation_id bigint,
    report_id uuid,
    administrator_id uuid,
    investigation_note text,
    previous_status text,
    new_status text,
    resolution text,
    created_at timestamptz,
    updated_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $function$
begin

    perform public.aepm_anonymous_require_admin();


    return query

    select
        i.investigation_id,
        i.report_id,
        i.administrator_id,
        i.investigation_note,
        i.previous_status::text,
        i.new_status::text,
        i.resolution,
        i.created_at,
        i.updated_at

    from public.investigation_tbl i

    where i.report_id = p_report_id

    order by
        i.created_at desc,
        i.investigation_id desc

    limit 1;

end;
$function$;


-- ============================================================
-- 26. ADMIN - SAVE INVESTIGATION
-- ============================================================

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
set search_path = public
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


    update public.anonymous_report_tbl
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
                    resolution
                )

                else resolution

            end

    where report_id = p_report_id;


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


-- ============================================================
-- 27. ADMIN - INVESTIGATION HISTORY
-- ============================================================

create or replace function public.aepm_anonymous_investigation_history(
    p_report_id uuid
)
returns table (
    investigation_id bigint,
    report_id uuid,
    administrator_id uuid,
    investigation_note text,
    previous_status text,
    new_status text,
    resolution text,
    created_at timestamptz,
    updated_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $function$
begin

    perform public.aepm_anonymous_require_admin();


    return query

    select
        i.investigation_id,
        i.report_id,
        i.administrator_id,
        i.investigation_note,
        i.previous_status::text,
        i.new_status::text,
        i.resolution,
        i.created_at,
        i.updated_at

    from public.investigation_tbl i

    where i.report_id = p_report_id

    order by
        i.created_at asc,
        i.investigation_id asc;

end;
$function$;


-- ============================================================
-- 28. ENABLE RLS
-- ============================================================

alter table public.anonymous_report_tbl
    enable row level security;


alter table public.anonymous_message_tbl
    enable row level security;


alter table public.investigation_tbl
    enable row level security;


-- ============================================================
-- 29. DROP EXISTING POLICIES
-- ============================================================

do $drop_policies$
declare
    r record;
begin

    for r in

        select
            schemaname,
            tablename,
            policyname

        from pg_policies

        where schemaname = 'public'

          and tablename in (
              'anonymous_report_tbl',
              'anonymous_message_tbl',
              'investigation_tbl'
          )

    loop

        execute format(
            'drop policy if exists %I on %I.%I',
            r.policyname,
            r.schemaname,
            r.tablename
        );

    end loop;

end;
$drop_policies$;


-- ============================================================
-- 30. RPC-ONLY POLICIES
-- ============================================================

create policy anonymous_report_rpc_only
on public.anonymous_report_tbl
as restrictive
for all
to public
using (false)
with check (false);


create policy anonymous_message_rpc_only
on public.anonymous_message_tbl
as restrictive
for all
to public
using (false)
with check (false);


create policy anonymous_investigation_rpc_only
on public.investigation_tbl
as restrictive
for all
to public
using (false)
with check (false);


-- ============================================================
-- 31. REVOKE DIRECT TABLE ACCESS
-- ============================================================

revoke all
on table public.anonymous_report_tbl
from public, anon, authenticated;


revoke all
on table public.anonymous_message_tbl
from public, anon, authenticated;


revoke all
on table public.investigation_tbl
from public, anon, authenticated;


-- ============================================================
-- 32. RPC GRANTS
-- ============================================================

grant execute
on function public.aepm_submit_anonymous_report(
    text,
    text,
    text,
    text
)
to authenticated;


grant execute
on function public.aepm_track_anonymous_report(text)
to authenticated;


grant execute
on function public.aepm_employee_report_messages(text)
to authenticated;


grant execute
on function public.aepm_send_employee_report_message(
    text,
    text
)
to authenticated;


grant execute
on function public.aepm_list_anonymous_reports(
    text,
    text,
    date,
    date,
    integer,
    integer
)
to authenticated;


grant execute
on function public.aepm_get_anonymous_report(uuid)
to authenticated;


grant execute
on function public.aepm_admin_report_messages(uuid)
to authenticated;


grant execute
on function public.aepm_send_admin_report_message(
    uuid,
    text
)
to authenticated;


grant execute
on function public.aepm_get_anonymous_investigation(uuid)
to authenticated;


grant execute
on function public.aepm_save_anonymous_investigation(
    uuid,
    text,
    text,
    text
)
to authenticated;


grant execute
on function public.aepm_anonymous_investigation_history(uuid)
to authenticated;


-- ============================================================
-- 33. REVOKE PUBLIC / ANON RPC ACCESS
-- ============================================================

revoke execute
on function public.aepm_submit_anonymous_report(
    text,
    text,
    text,
    text
)
from public, anon;


revoke execute
on function public.aepm_track_anonymous_report(text)
from public, anon;


revoke execute
on function public.aepm_employee_report_messages(text)
from public, anon;


revoke execute
on function public.aepm_send_employee_report_message(
    text,
    text
)
from public, anon;


revoke execute
on function public.aepm_list_anonymous_reports(
    text,
    text,
    date,
    date,
    integer,
    integer
)
from public, anon;


revoke execute
on function public.aepm_get_anonymous_report(uuid)
from public, anon;


revoke execute
on function public.aepm_admin_report_messages(uuid)
from public, anon;


revoke execute
on function public.aepm_send_admin_report_message(
    uuid,
    text
)
from public, anon;


revoke execute
on function public.aepm_get_anonymous_investigation(uuid)
from public, anon;


revoke execute
on function public.aepm_save_anonymous_investigation(
    uuid,
    text,
    text,
    text
)
from public, anon;


revoke execute
on function public.aepm_anonymous_investigation_history(uuid)
from public, anon;


-- ============================================================
-- 34. DROP PLAINTEXT REFERENCE NUMBER
-- ============================================================

do $drop_reference$
begin

    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'anonymous_report_tbl'
          and column_name = 'reference_number'
    ) then

        alter table public.anonymous_report_tbl
            drop column reference_number;

    end if;

end;
$drop_reference$;


-- ============================================================
-- 35. FINAL SECURITY CHECK
-- ============================================================

do $security_check$
begin

    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'anonymous_report_tbl'
          and column_name = 'reference_number'
    ) then

        raise exception
            'Security check failed: reference_number still exists.';

    end if;


    if not exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'anonymous_report_tbl'
          and column_name = 'reference_hash'
    ) then

        raise exception
            'Security check failed: reference_hash is missing.';

    end if;


    if exists (
        select 1
        from public.anonymous_report_tbl
        where reference_hash is null
           or length(reference_hash) <> 64
    ) then

        raise exception
            'Security check failed: invalid reference hash exists.';

    end if;

end;
$security_check$;


-- ============================================================
-- 36. POSTGREST RELOAD
-- ============================================================

notify pgrst, 'reload schema';


-- ============================================================
-- 37. COMMIT
-- ============================================================

commit;