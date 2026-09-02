-- Georgian text needs nvarchar, not varchar.
--
-- The database collation is SQL_Latin1_General_CP1_CI_AS, which has no code
-- page for Georgian. A varchar column silently stores ?????????? in place of
-- მათემატიკა - the write succeeds, nothing errors, and the text is simply gone.
-- Every string this system holds is Georgian, so String now maps to nvarchar
-- (hibernate.use_nationalized_character_data).
--
-- Only needed where the schema was created before that setting existed. A
-- database built from 001_schema.sql today already has nvarchar throughout.
--
-- Indexed columns cannot be altered in place, so the constraints that cover
-- them are dropped and rebuilt around the change.

SET
XACT_ABORT ON;
BEGIN
TRANSACTION;

-- ---- drop what sits on top of the affected columns -----------------------

DROP INDEX ix_student_last_name ON sgs.student;
DROP INDEX ix_tv_template_status ON sgs.template_version;
DROP INDEX ix_grade_grid_covering ON sgs.grade_entry;

ALTER TABLE sgs.template_assignment DROP CONSTRAINT uq_assignment_class_subject_scope;
ALTER TABLE sgs.class_group DROP CONSTRAINT uq_class_year_school_name;
ALTER TABLE sgs.component DROP CONSTRAINT uq_component_version_code;
ALTER TABLE sgs.class_period_setting DROP CONSTRAINT uq_cps_class_period_key;
ALTER TABLE sgs.period DROP CONSTRAINT uq_period_scheme_code;
ALTER TABLE sgs.school DROP CONSTRAINT uq_school_code;
ALTER TABLE sgs.student DROP CONSTRAINT uq_student_username;
ALTER TABLE sgs.subject DROP CONSTRAINT uq_subject_name;
ALTER TABLE sgs.academic_year DROP CONSTRAINT uq_year_code;
GO

-- ---- the columns ---------------------------------------------------------

ALTER TABLE sgs.academic_year ALTER COLUMN code nvarchar(16) NOT NULL;

ALTER TABLE sgs.class_group ALTER COLUMN name nvarchar(64) NOT NULL;

ALTER TABLE sgs.class_period_setting ALTER COLUMN setting_key nvarchar(64) NOT NULL;

ALTER TABLE sgs.component ALTER COLUMN code nvarchar(64) NOT NULL;
ALTER TABLE sgs.component ALTER COLUMN group_label nvarchar(256) NULL;
ALTER TABLE sgs.component ALTER COLUMN kind nvarchar(16) NOT NULL;
ALTER TABLE sgs.component ALTER COLUMN label nvarchar(256) NOT NULL;
ALTER TABLE sgs.component ALTER COLUMN period_kind nvarchar(16) NOT NULL;

ALTER TABLE sgs.derivation_rule ALTER COLUMN null_policy nvarchar(16) NOT NULL;
ALTER TABLE sgs.derivation_rule ALTER COLUMN rounding_mode nvarchar(16) NOT NULL;
ALTER TABLE sgs.derivation_rule ALTER COLUMN type nvarchar(24) NOT NULL;

ALTER TABLE sgs.derivation_term ALTER COLUMN label nvarchar(256) NULL;
ALTER TABLE sgs.derivation_term ALTER COLUMN period_ref nvarchar(16) NOT NULL;
ALTER TABLE sgs.derivation_term ALTER COLUMN reduce nvarchar(16) NOT NULL;
ALTER TABLE sgs.derivation_term ALTER COLUMN source_kind nvarchar(16) NOT NULL;

ALTER TABLE sgs.grade_entry ALTER COLUMN published_special_value nvarchar(16) NULL;
ALTER TABLE sgs.grade_entry ALTER COLUMN source nvarchar(16) NOT NULL;
ALTER TABLE sgs.grade_entry ALTER COLUMN special_value nvarchar(16) NULL;

ALTER TABLE sgs.grading_template ALTER COLUMN description nvarchar(512) NULL;
ALTER TABLE sgs.grading_template ALTER COLUMN name nvarchar(128) NOT NULL;
ALTER TABLE sgs.grading_template ALTER COLUMN scope nvarchar(16) NOT NULL;

ALTER TABLE sgs.period ALTER COLUMN code nvarchar(32) NOT NULL;
ALTER TABLE sgs.period ALTER COLUMN kind nvarchar(16) NOT NULL;
ALTER TABLE sgs.period ALTER COLUMN label nvarchar(128) NOT NULL;

ALTER TABLE sgs.period_scheme ALTER COLUMN name nvarchar(128) NOT NULL;

ALTER TABLE sgs.school ALTER COLUMN code nvarchar(32) NOT NULL;
ALTER TABLE sgs.school ALTER COLUMN name nvarchar(128) NOT NULL;

ALTER TABLE sgs.student ALTER COLUMN first_name nvarchar(128) NOT NULL;
ALTER TABLE sgs.student ALTER COLUMN guardian_email nvarchar(256) NULL;
ALTER TABLE sgs.student ALTER COLUMN last_name nvarchar(128) NOT NULL;
ALTER TABLE sgs.student ALTER COLUMN password_hash nvarchar(256) NOT NULL;
ALTER TABLE sgs.student ALTER COLUMN personal_number nvarchar(32) NULL;
ALTER TABLE sgs.student ALTER COLUMN username nvarchar(64) NOT NULL;

ALTER TABLE sgs.subject ALTER COLUMN name nvarchar(256) NOT NULL;
ALTER TABLE sgs.subject ALTER COLUMN short_name nvarchar(64) NULL;

ALTER TABLE sgs.template_assignment ALTER COLUMN scope nvarchar(16) NOT NULL;

ALTER TABLE sgs.template_version ALTER COLUMN status nvarchar(16) NOT NULL;
GO

-- ---- put them back -------------------------------------------------------

ALTER TABLE sgs.academic_year
    ADD CONSTRAINT uq_year_code UNIQUE (code);
ALTER TABLE sgs.subject
    ADD CONSTRAINT uq_subject_name UNIQUE (name);
ALTER TABLE sgs.student
    ADD CONSTRAINT uq_student_username UNIQUE (username);
ALTER TABLE sgs.school
    ADD CONSTRAINT uq_school_code UNIQUE (code);
ALTER TABLE sgs.period
    ADD CONSTRAINT uq_period_scheme_code UNIQUE (scheme_id, code);
ALTER TABLE sgs.class_period_setting
    ADD CONSTRAINT uq_cps_class_period_key
        UNIQUE (class_group_id, period_id, setting_key);
ALTER TABLE sgs.component
    ADD CONSTRAINT uq_component_version_code
        UNIQUE (template_version_id, code);
ALTER TABLE sgs.class_group
    ADD CONSTRAINT uq_class_year_school_name
        UNIQUE (academic_year_id, school_id, name);
ALTER TABLE sgs.template_assignment
    ADD CONSTRAINT uq_assignment_class_subject_scope
        UNIQUE (class_group_id, subject_id, scope);

CREATE
NONCLUSTERED INDEX ix_student_last_name ON sgs.student (last_name);
CREATE
NONCLUSTERED INDEX ix_tv_template_status ON sgs.template_version (template_id, status);

-- The working-set index, from 002_indexes.sql. It is the reason a whole grid
-- loads in one seek.
CREATE
NONCLUSTERED INDEX ix_grade_grid_covering
    ON sgs.grade_entry (period_id, subject_id, enrollment_id)
    INCLUDE (component_id, value, special_value, row_version, is_override, source,
             template_version_id);
GO

COMMIT TRANSACTION;
GO
