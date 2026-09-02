create sequence sgs.absence_notice_seq start with 1 increment by 50;
create sequence sgs.academic_year_seq start with 1 increment by 50;
create sequence sgs.class_group_seq start with 1 increment by 50;
create sequence sgs.class_period_setting_seq start with 1 increment by 50;
create sequence sgs.class_subject_seq start with 1 increment by 50;
create sequence sgs.component_seq start with 1 increment by 50;
create sequence sgs.conversion_formula_seq start with 1 increment by 50;
create sequence sgs.daily_absence_seq start with 1 increment by 50;
create sequence sgs.derivation_rule_seq start with 1 increment by 50;
create sequence sgs.derivation_source_seq start with 1 increment by 50;
create sequence sgs.derivation_term_seq start with 1 increment by 50;
create sequence sgs.enrollment_placement_seq start with 1 increment by 50;
create sequence sgs.enrollment_seq start with 1 increment by 50;
create sequence sgs.grade_change_request_seq start with 1 increment by 50;
create sequence sgs.grade_entry_seq start with 1 increment by 50;
create sequence sgs.grading_template_seq start with 1 increment by 50;
create sequence sgs.homework_seen_seq start with 1 increment by 50;
create sequence sgs.period_scheme_seq start with 1 increment by 50;
create sequence sgs.period_seq start with 1 increment by 50;
create sequence sgs.post_category_seq start with 1 increment by 50;
create sequence sgs.post_image_seq start with 1 increment by 50;
create sequence sgs.post_line_seq start with 1 increment by 50;
create sequence sgs.post_link_seq start with 1 increment by 50;
create sequence sgs.post_seq start with 1 increment by 50;
create sequence sgs.post_target_seq start with 1 increment by 50;
create sequence sgs.publication_seq start with 1 increment by 50;
create sequence sgs.school_seq start with 1 increment by 50;
create sequence sgs.student_seq start with 1 increment by 50;
create sequence sgs.subject_seq start with 1 increment by 50;
create sequence sgs.teaching_assignment_seq start with 1 increment by 50;
create sequence sgs.template_assignment_seq start with 1 increment by 50;
create sequence sgs.template_version_seq start with 1 increment by 50;

create table sgs.absence_notice
(
    id            bigint    not null,
    absence_date  date      not null,
    is_cancelled  bit       not null,
    queued_at     datetime2 not null,
    sent_at       datetime2,
    enrollment_id bigint    not null,
    primary key (id)
);

create table sgs.academic_year
(
    id         bigint not null,
    code       nvarchar(16) not null,
    is_current bit    not null,
    ends_on    date   not null,
    starts_on  date   not null,
    primary key (id)
);

create table sgs.class_group
(
    id               bigint   not null,
    level            smallint not null,
    name             nvarchar(64) not null,
    academic_year_id bigint   not null,
    period_scheme_id bigint   not null,
    school_id        bigint   not null,
    primary key (id)
);

create table sgs.class_period_setting
(
    id             bigint    not null,
    created_at     datetime2 not null,
    created_by     bigint,
    updated_at     datetime2 not null,
    updated_by     bigint,
    setting_key    nvarchar(64) not null,
    setting_value  numeric(12, 2),
    class_group_id bigint    not null,
    period_id      bigint    not null,
    primary key (id)
);

create table sgs.class_subject
(
    id                  bigint not null,
    sort_index          int    not null,
    teacher_name        nvarchar(256),
    class_group_id      bigint not null,
    subject_id          bigint not null,
    template_version_id bigint,
    primary key (id)
);

create table sgs.component
(
    id                   bigint      not null,
    allow_override       bit         not null,
    allow_special_values bit         not null,
    code                 nvarchar(64) not null,
    decimals             int         not null,
    group_label          nvarchar(256),
    kind                 varchar(16) not null,
    label                nvarchar(256) not null,
    ordinal              int         not null,
    parent_visible       bit         not null,
    period_kind          varchar(16) not null,
    scale_max            numeric(6, 2),
    scale_min            numeric(6, 2),
    subject_scoped       bit         not null,
    summary_column       bit         not null,
    template_version_id  bigint      not null,
    primary key (id)
);

create table sgs.conversion_formula
(
    id           bigint    not null,
    created_at   datetime2 not null,
    created_by   bigint,
    updated_at   datetime2 not null,
    updated_by   bigint,
    multiplier   numeric(9, 4),
    name         nvarchar(128) not null,
    offset_value numeric(9, 4),
    primary key (id)
);

create table sgs.daily_absence
(
    id            bigint    not null,
    absence_date  date      not null,
    marked_at     datetime2 not null,
    marked_by     bigint,
    enrollment_id bigint    not null,
    primary key (id)
);

create table sgs.derivation_rule
(
    id                  bigint      not null,
    chain_order         int         not null,
    decimals            int         not null,
    null_policy         varchar(16) not null,
    renormalize_weights bit         not null,
    rounding_mode       varchar(16) not null,
    type                varchar(24) not null,
    component_id        bigint      not null,
    primary key (id)
);

create table sgs.derivation_source
(
    id           bigint not null,
    component_id bigint not null,
    term_id      bigint not null,
    primary key (id)
);

create table sgs.derivation_term
(
    id          bigint        not null,
    label       nvarchar(256),
    ordinal     int           not null,
    period_ref  varchar(16)   not null,
    reduce      varchar(16)   not null,
    source_kind varchar(16)   not null,
    weight      numeric(6, 4) not null,
    period_id   bigint,
    rule_id     bigint        not null,
    primary key (id)
);

create table sgs.enrollment
(
    id               bigint not null,
    joined_on        date,
    left_on          date,
    academic_year_id bigint not null,
    class_group_id   bigint not null,
    student_id       bigint not null,
    primary key (id)
);

create table sgs.enrollment_placement
(
    id             bigint    not null,
    created_at     datetime2 not null,
    created_by     bigint,
    updated_at     datetime2 not null,
    updated_by     bigint,
    from_date      date      not null,
    to_date        date,
    class_group_id bigint    not null,
    enrollment_id  bigint    not null,
    primary key (id)
);

create table sgs.grade_change_request
(
    id                      bigint      not null,
    decided_at              datetime2,
    decided_by              bigint,
    decision_comment        nvarchar(1024),
    previous_special_value  nvarchar(16),
    previous_value          numeric(6, 2),
    reason                  nvarchar(1024) not null,
    requested_at            datetime2   not null,
    requested_by            bigint,
    requested_special_value nvarchar(16),
    requested_value         numeric(6, 2),
    row_version             int         not null,
    status                  varchar(16) not null,
    grade_entry_id          bigint      not null,
    primary key (id)
);

create table sgs.grade_entry
(
    id                      bigint      not null,
    created_at              datetime2   not null,
    created_by              bigint,
    updated_at              datetime2   not null,
    updated_by              bigint,
    is_override             bit         not null,
    published_at            datetime2,
    published_special_value nvarchar(16),
    published_value         numeric(6, 2),
    row_version             int         not null,
    source                  varchar(16) not null,
    special_value           nvarchar(16),
    value                   numeric(6, 2),
    component_id            bigint      not null,
    enrollment_id           bigint      not null,
    period_id               bigint      not null,
    subject_id              bigint,
    template_version_id     bigint      not null,
    primary key (id)
);

create table sgs.grading_template
(
    id                bigint      not null,
    created_at        datetime2   not null,
    created_by        bigint,
    updated_at        datetime2   not null,
    updated_by        bigint,
    is_archived       bit         not null,
    chart_key         nvarchar(32),
    description       nvarchar(512),
    frequency         varchar(16) not null,
    grid_mode         varchar(16) not null,
    locks_on_publish  bit         not null,
    name              nvarchar(128) not null,
    is_parent_visible bit         not null,
    sort_index        int         not null,
    subject_scoped    bit         not null,
    uuid              nvarchar(36) not null,
    school_id         bigint,
    primary key (id)
);

create table sgs.homework_seen
(
    id            bigint    not null,
    seen_at       datetime2 not null,
    enrollment_id bigint    not null,
    post_id       bigint    not null,
    primary key (id)
);

create table sgs.period
(
    id        bigint      not null,
    code      nvarchar(32) not null,
    depth     int         not null,
    ends_on   date,
    kind      varchar(16) not null,
    label     nvarchar(128) not null,
    ordinal   int         not null,
    starts_on date,
    parent_id bigint,
    scheme_id bigint      not null,
    primary key (id)
);

create table sgs.period_scheme
(
    id               bigint not null,
    name             nvarchar(128) not null,
    academic_year_id bigint not null,
    primary key (id)
);

create table sgs.post
(
    id                      bigint      not null,
    created_at              datetime2   not null,
    created_by              bigint,
    updated_at              datetime2   not null,
    updated_by              bigint,
    is_archived             bit         not null,
    body_html               nvarchar(MAX),
    event_date              date,
    has_unpublished_changes bit         not null,
    kind                    varchar(24) not null,
    published_at            datetime2,
    published_payload       nvarchar(MAX),
    status                  varchar(16) not null,
    title                   nvarchar(512),
    uuid                    nvarchar(36) not null,
    category_id             bigint,
    class_group_id          bigint,
    image_id                bigint,
    subject_id              bigint,
    primary key (id)
);

create table sgs.post_category
(
    id          bigint    not null,
    created_at  datetime2 not null,
    created_by  bigint,
    updated_at  datetime2 not null,
    updated_by  bigint,
    is_archived bit       not null,
    name        nvarchar(128) not null,
    uuid        nvarchar(36) not null,
    primary key (id)
);

create table sgs.post_image
(
    id           bigint    not null,
    created_at   datetime2 not null,
    created_by   bigint,
    updated_at   datetime2 not null,
    updated_by   bigint,
    byte_size    int       not null,
    bytes        varbinary(MAX) not null,
    content_type nvarchar(64) not null,
    height       int       not null,
    uuid         nvarchar(36) not null,
    width        int       not null,
    primary key (id)
);

create table sgs.post_line
(
    id        bigint not null,
    ordinal   int    not null,
    text      nvarchar(1024),
    time_text nvarchar(64),
    weekday   int    not null,
    post_id   bigint not null,
    primary key (id)
);

create table sgs.post_link
(
    id      bigint not null,
    label   nvarchar(256),
    ordinal int    not null,
    url     nvarchar(2048) not null,
    post_id bigint not null,
    primary key (id)
);

create table sgs.post_target
(
    id            bigint not null,
    enrollment_id bigint not null,
    post_id       bigint not null,
    primary key (id)
);

create table sgs.publication
(
    id                  bigint    not null,
    cell_count          int       not null,
    from_change_request bit       not null,
    published_at        datetime2 not null,
    published_by        bigint,
    class_group_id      bigint    not null,
    period_id           bigint    not null,
    subject_id          bigint,
    primary key (id)
);

create table sgs.school
(
    id      bigint not null,
    code    nvarchar(32) not null,
    name    nvarchar(128) not null,
    ordinal int    not null,
    primary key (id)
);

create table sgs.student
(
    id              bigint    not null,
    created_at      datetime2 not null,
    created_by      bigint,
    updated_at      datetime2 not null,
    updated_by      bigint,
    is_active       bit       not null,
    first_name      nvarchar(128) not null,
    guardian_email  nvarchar(256),
    last_name       nvarchar(128) not null,
    password_hash   nvarchar(256) not null,
    personal_number nvarchar(32),
    username        nvarchar(64) not null,
    primary key (id)
);

create table sgs.subject
(
    id         bigint    not null,
    created_at datetime2 not null,
    created_by bigint,
    updated_at datetime2 not null,
    updated_by bigint,
    is_active  bit       not null,
    name       nvarchar(256) not null,
    short_name nvarchar(64),
    primary key (id)
);

create table sgs.teaching_assignment
(
    id               bigint not null,
    is_primary       bit    not null,
    system_user_id   bigint not null,
    class_subject_id bigint not null,
    primary key (id)
);

create table sgs.template_assignment
(
    id                  bigint    not null,
    created_at          datetime2 not null,
    created_by          bigint,
    updated_at          datetime2 not null,
    updated_by          bigint,
    class_group_id      bigint    not null,
    subject_id          bigint,
    template_id         bigint    not null,
    template_version_id bigint    not null,
    primary key (id)
);

create table sgs.template_version
(
    id                       bigint      not null,
    created_at               datetime2   not null,
    created_by               bigint,
    updated_at               datetime2   not null,
    updated_by               bigint,
    activated_at             datetime2,
    status                   varchar(16) not null,
    version_no               int         not null,
    effective_from_period_id bigint,
    period_scheme_id         bigint      not null,
    template_id              bigint      not null,
    primary key (id)
);
create index ix_absence_notice_pending on sgs.absence_notice (sent_at, queued_at);
create index ix_absence_notice_student on sgs.absence_notice (enrollment_id, absence_date);

alter table sgs.academic_year
    add constraint uq_year_code unique (code);
create index ix_class_year on sgs.class_group (academic_year_id);

alter table sgs.class_group
    add constraint uq_class_year_school_name unique (academic_year_id, school_id, name);

alter table sgs.class_period_setting
    add constraint uq_cps_class_period_key unique (class_group_id, period_id, setting_key);
create index ix_class_subject_class on sgs.class_subject (class_group_id, sort_index);

alter table sgs.class_subject
    add constraint uq_class_subject unique (class_group_id, subject_id);
create index ix_component_version on sgs.component (template_version_id, ordinal);

alter table sgs.component
    add constraint uq_component_version_code unique (template_version_id, code);
create index ix_daily_absence_date on sgs.daily_absence (absence_date);

alter table sgs.daily_absence
    add constraint uq_daily_absence unique (enrollment_id, absence_date);

alter table sgs.derivation_rule
    add constraint uq_rule_component_chain unique (component_id, chain_order);
create index ix_source_component on sgs.derivation_source (component_id);

alter table sgs.derivation_source
    add constraint uq_source_term_component unique (term_id, component_id);
create index ix_term_rule on sgs.derivation_term (rule_id, ordinal);
create index ix_enrollment_class on sgs.enrollment (class_group_id);
create index ix_enrollment_student on sgs.enrollment (student_id);

alter table sgs.enrollment
    add constraint uq_enrollment_student_year unique (student_id, academic_year_id);
create index ix_change_request_status on sgs.grade_change_request (status, requested_at);
create index ix_change_request_entry on sgs.grade_change_request (grade_entry_id);
create index ix_grade_component_period on sgs.grade_entry (component_id, period_id);
create index ix_grade_template_version on sgs.grade_entry (template_version_id);

alter table sgs.grade_entry
    add constraint uq_grade_cell unique (enrollment_id, subject_id, period_id, component_id);

alter table sgs.grading_template
    add constraint uq_template_uuid unique (uuid);

alter table sgs.homework_seen
    add constraint uq_homework_seen unique (enrollment_id, post_id);
create index ix_period_scheme_parent on sgs.period (scheme_id, parent_id);

alter table sgs.period
    add constraint uq_period_scheme_code unique (scheme_id, code);
create index ix_post_class_kind on sgs.post (kind, class_group_id, subject_id, event_date);
create index ix_post_published on sgs.post (kind, status, event_date);

alter table sgs.post
    add constraint uq_post_uuid unique (uuid);

alter table sgs.post_category
    add constraint uq_post_category_name unique (name);

alter table sgs.post_image
    add constraint uq_post_image_uuid unique (uuid);
create index ix_post_line_post on sgs.post_line (post_id, weekday, ordinal);
create index ix_post_link_post on sgs.post_link (post_id, ordinal);
create index ix_post_target_enrollment on sgs.post_target (enrollment_id);

alter table sgs.post_target
    add constraint uq_post_target unique (post_id, enrollment_id);
create index ix_publication_class_period on sgs.publication (class_group_id, period_id, published_at);
create index ix_publication_at on sgs.publication (published_at);

alter table sgs.school
    add constraint uq_school_code unique (code);
create index ix_student_last_name on sgs.student (last_name);

alter table sgs.subject
    add constraint uq_subject_name unique (name);
create index ix_ta_user on sgs.teaching_assignment (system_user_id);

alter table sgs.teaching_assignment
    add constraint uq_teaching_assignment unique (class_subject_id, system_user_id);
create index ix_assignment_class on sgs.template_assignment (class_group_id);

alter table sgs.template_assignment
    add constraint uq_assignment_class_subject_journal unique (class_group_id, subject_id, template_id);
create index ix_tv_template_status on sgs.template_version (template_id, status);

alter table sgs.template_version
    add constraint uq_template_version_no unique (template_id, version_no);

alter table sgs.absence_notice
    add constraint fk_absence_notice_enrollment
        foreign key (enrollment_id)
            references sgs.enrollment;

alter table sgs.class_group
    add constraint fk_class_year
        foreign key (academic_year_id)
            references sgs.academic_year;

alter table sgs.class_group
    add constraint fk_class_scheme
        foreign key (period_scheme_id)
            references sgs.period_scheme;

alter table sgs.class_group
    add constraint fk_class_school
        foreign key (school_id)
            references sgs.school;

alter table sgs.class_period_setting
    add constraint fk_cps_class
        foreign key (class_group_id)
            references sgs.class_group;

alter table sgs.class_period_setting
    add constraint fk_cps_period
        foreign key (period_id)
            references sgs.period;

alter table sgs.class_subject
    add constraint fk_cs_class
        foreign key (class_group_id)
            references sgs.class_group;

alter table sgs.class_subject
    add constraint fk_cs_subject
        foreign key (subject_id)
            references sgs.subject;

alter table sgs.class_subject
    add constraint fk_cs_template_version
        foreign key (template_version_id)
            references sgs.template_version;

alter table sgs.component
    add constraint fk_component_version
        foreign key (template_version_id)
            references sgs.template_version;

alter table sgs.daily_absence
    add constraint fk_daily_absence_enrollment
        foreign key (enrollment_id)
            references sgs.enrollment;

alter table sgs.derivation_rule
    add constraint fk_rule_component
        foreign key (component_id)
            references sgs.component;

alter table sgs.derivation_source
    add constraint fk_source_component
        foreign key (component_id)
            references sgs.component;

alter table sgs.derivation_source
    add constraint fk_source_term
        foreign key (term_id)
            references sgs.derivation_term;

alter table sgs.derivation_term
    add constraint fk_term_period
        foreign key (period_id)
            references sgs.period;

alter table sgs.derivation_term
    add constraint fk_term_rule
        foreign key (rule_id)
            references sgs.derivation_rule;

alter table sgs.enrollment
    add constraint fk_enrollment_year
        foreign key (academic_year_id)
            references sgs.academic_year;

alter table sgs.enrollment
    add constraint fk_enrollment_class
        foreign key (class_group_id)
            references sgs.class_group;

alter table sgs.enrollment
    add constraint fk_enrollment_student
        foreign key (student_id)
            references sgs.student;

alter table sgs.enrollment_placement
    add constraint fk_placement_class
        foreign key (class_group_id)
            references sgs.class_group;

alter table sgs.enrollment_placement
    add constraint fk_placement_enrollment
        foreign key (enrollment_id)
            references sgs.enrollment;

alter table sgs.grade_change_request
    add constraint fk_change_request_entry
        foreign key (grade_entry_id)
            references sgs.grade_entry;

alter table sgs.grade_entry
    add constraint fk_grade_component
        foreign key (component_id)
            references sgs.component;

alter table sgs.grade_entry
    add constraint fk_grade_enrollment
        foreign key (enrollment_id)
            references sgs.enrollment;

alter table sgs.grade_entry
    add constraint fk_grade_period
        foreign key (period_id)
            references sgs.period;

alter table sgs.grade_entry
    add constraint fk_grade_subject
        foreign key (subject_id)
            references sgs.subject;

alter table sgs.grade_entry
    add constraint fk_grade_version
        foreign key (template_version_id)
            references sgs.template_version;

alter table sgs.grading_template
    add constraint fk_template_school
        foreign key (school_id)
            references sgs.school;

alter table sgs.homework_seen
    add constraint fk_homework_seen_enrollment
        foreign key (enrollment_id)
            references sgs.enrollment;

alter table sgs.homework_seen
    add constraint fk_homework_seen_post
        foreign key (post_id)
            references sgs.post;

alter table sgs.period
    add constraint fk_period_parent
        foreign key (parent_id)
            references sgs.period;

alter table sgs.period
    add constraint fk_period_scheme
        foreign key (scheme_id)
            references sgs.period_scheme;

alter table sgs.period_scheme
    add constraint fk_scheme_year
        foreign key (academic_year_id)
            references sgs.academic_year;

alter table sgs.post
    add constraint fk_post_category
        foreign key (category_id)
            references sgs.post_category;

alter table sgs.post
    add constraint fk_post_class
        foreign key (class_group_id)
            references sgs.class_group;

alter table sgs.post
    add constraint fk_post_image
        foreign key (image_id)
            references sgs.post_image;

alter table sgs.post
    add constraint fk_post_subject
        foreign key (subject_id)
            references sgs.subject;

alter table sgs.post_line
    add constraint fk_post_line_post
        foreign key (post_id)
            references sgs.post;

alter table sgs.post_link
    add constraint fk_post_link_post
        foreign key (post_id)
            references sgs.post;

alter table sgs.post_target
    add constraint fk_post_target_enrollment
        foreign key (enrollment_id)
            references sgs.enrollment;

alter table sgs.post_target
    add constraint fk_post_target_post
        foreign key (post_id)
            references sgs.post;

alter table sgs.publication
    add constraint fk_publication_class
        foreign key (class_group_id)
            references sgs.class_group;

alter table sgs.publication
    add constraint fk_publication_period
        foreign key (period_id)
            references sgs.period;

alter table sgs.publication
    add constraint fk_publication_subject
        foreign key (subject_id)
            references sgs.subject;

alter table sgs.teaching_assignment
    add constraint fk_ta_class_subject
        foreign key (class_subject_id)
            references sgs.class_subject;

alter table sgs.template_assignment
    add constraint fk_assignment_class
        foreign key (class_group_id)
            references sgs.class_group;

alter table sgs.template_assignment
    add constraint fk_assignment_subject
        foreign key (subject_id)
            references sgs.subject;

alter table sgs.template_assignment
    add constraint fk_assignment_template
        foreign key (template_id)
            references sgs.grading_template;

alter table sgs.template_assignment
    add constraint fk_assignment_version
        foreign key (template_version_id)
            references sgs.template_version;

alter table sgs.template_version
    add constraint fk_tv_effective_period
        foreign key (effective_from_period_id)
            references sgs.period;

alter table sgs.template_version
    add constraint fk_tv_scheme
        foreign key (period_scheme_id)
            references sgs.period_scheme;

alter table sgs.template_version
    add constraint fk_tv_template
        foreign key (template_id)
            references sgs.grading_template;
