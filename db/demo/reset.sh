#!/usr/bin/env bash
#
# Build the demo database from nothing, every time.
#
#   bash db/demo/reset.sh
#
# Drops SGS_DEMO if it is there and rebuilds it: schema, migrations, roster,
# journals, administrator. Takes about half a minute. The real SGS database,
# with the school's migrated data in it, is never touched.
#
# Run it whenever a test run has made a mess. That is the point of it - nothing
# in here is precious, so there is never a reason to repair the demo database by
# hand.
#
# ---------------------------------------------------------------------------
# THE ORDER BELOW IS NOT ARBITRARY and is not the numeric order of the files.
# Each script is here because it does something a fresh database needs, and the
# ones that only exist to upgrade an older database are left out - db/001 has
# their changes already. Where two scripts fight over the same rows, the later
# one wins on purpose:
#
#   013 creates numbered weeks -> 021 replaces them with dated days
#                              -> 028 deletes the days again
#
# That is churn, and it is deliberate: 022 refuses to run without day periods,
# so the fresh path walks the same road the school's own database walked rather
# than a shortcut that has never been tested anywhere.
# ---------------------------------------------------------------------------

set -euo pipefail

DB=SGS_DEMO
CONTAINER=sps-mssql-db
SA_PASSWORD='mfcAtl!7'
SQLCMD=/opt/mssql-tools/bin/sqlcmd

# Git Bash rewrites /opt/... into a Windows path on its way into docker exec.
export MSYS_NO_PATHCONV=1

cd "$(dirname "$0")/../.."          # repo root

run_sql() {                          # run_sql <file>
    local file="$1"
    printf '  %-34s' "$(basename "$file")"
    docker cp "$file" "$CONTAINER:/tmp/script.sql" >/dev/null
    # -b   a SQL error is an exit code, not a line of output we might miss
    # -f   the scripts are UTF-8 and full of Georgian
    if ! out=$(docker exec "$CONTAINER" "$SQLCMD" -S localhost -U sa -P "$SA_PASSWORD" \
                    -d "$DB" -b -f 65001 -i /tmp/script.sql 2>&1); then
        echo "FAILED"
        echo "$out" | sed 's/^/    /'
        exit 1
    fi
    echo "ok"
}

run_query() {                        # run_query <sql>
    docker exec "$CONTAINER" "$SQLCMD" -S localhost -U sa -P "$SA_PASSWORD" \
        -d master -b -h -1 -W -Q "SET NOCOUNT ON; $1"
}

echo "==> container"
docker start "$CONTAINER" >/dev/null
# The server accepts connections a few seconds after the container is up.
for _ in $(seq 1 30); do
    if docker exec "$CONTAINER" "$SQLCMD" -S localhost -U sa -P "$SA_PASSWORD" \
            -Q "SELECT 1" >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

echo "==> dropping and creating $DB"
# SINGLE_USER WITH ROLLBACK IMMEDIATE: an application left running holds a
# connection, and DROP DATABASE waits for it forever otherwise.
run_query "
IF DB_ID('$DB') IS NOT NULL
BEGIN
    ALTER DATABASE [$DB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [$DB];
END
CREATE DATABASE [$DB];
" >/dev/null
run_query "USE [$DB]; EXEC('CREATE SCHEMA sgs');" >/dev/null

echo "==> schema"
run_sql db/001_schema.sql            # generated from the entities
run_sql db/002_indexes.sql           # the two indexes JPA cannot express

echo "==> roster"
run_sql db/demo/100_roster.sql       # stands in for 006_migrate_from_dbo
run_sql db/015_student_identity.sql  # here only for its two filtered indexes
run_sql db/016_change_request_version.sql
run_sql db/017_conversion_formula.sql
run_sql db/019_posts.sql
run_sql db/020_content_modules.sql   # here for the news categories it seeds

echo "==> periods"
run_sql db/013_period_levels.sql     # months, and weeks that 021 will replace
run_sql db/021_absence.sql           # days replace weeks; grid_mode; notices

echo "==> journals"
run_sql db/007_seed_template.sql     # the trimester journal
run_sql db/018_annual_columns.sql    # exam, overall, project
run_sql db/022_absence_journals.sql  # the monthly absence register
run_sql db/023_absence_rollup_fix.sql
run_sql db/024_absence_notice_constraint.sql
run_sql db/025_absence_notice_pending_unique.sql
run_sql db/026_absence_scales.sql
run_sql db/033_trimester_columns.sql # the brief's trimester column
run_sql db/034_summary_columns.sql   # which columns make the report card
run_sql db/028_daily_absence.sql     # daily absence leaves grade_entry
run_sql db/029_publication_lock.sql
run_sql db/030_parent_content.sql    # homework_seen; the register goes visible
run_sql db/031_enrollment_placement.sql  # placement history beside the enrollment
# Last of the period work, and it has to be: the guard refuses while anything
# sits below the months, and db/013's weeks only clear once 028 has run.
run_sql db/032_reporting_periods.sql  # ten months become the brief's seven

echo "==> login and settings"
run_sql db/demo/110_admin_user.sql
run_sql db/demo/120_absence_settings.sql
run_sql db/demo/130_ethics_journal.sql

echo
echo "==> what is in it"
docker exec "$CONTAINER" "$SQLCMD" -S localhost -U sa -P "$SA_PASSWORD" -d "$DB" \
    -b -f 65001 -W -Q "
SET NOCOUNT ON;
SELECT s.name AS school, c.name AS class, COUNT(e.id) AS students
FROM sgs.class_group c
JOIN sgs.school s ON s.id = c.school_id
LEFT JOIN sgs.enrollment e ON e.class_group_id = c.id
GROUP BY s.name, c.name ORDER BY s.name;

SELECT t.name AS journal, t.frequency, t.grid_mode, t.is_parent_visible AS parents,
       t.locks_on_publish AS locks, COUNT(k.id) AS columns
FROM sgs.grading_template t
JOIN sgs.template_version v ON v.template_id = t.id AND v.status = 'ACTIVE'
LEFT JOIN sgs.component k ON k.template_version_id = v.id
GROUP BY t.name, t.frequency, t.grid_mode, t.is_parent_visible, t.locks_on_publish
ORDER BY t.name;

SELECT depth, COUNT(*) AS periods FROM sgs.period GROUP BY depth ORDER BY depth;
"

echo
echo "Done. Point the application at it:"
echo "  jdbc:sqlserver://localhost:1433;databaseName=$DB;"
echo "Log in to the staff console as  admin / admin"
