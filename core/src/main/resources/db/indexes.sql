-- Performance indexes for SGS (SQL Server).
--
-- The same indexes are declared on the JPA entities (@Index on Grade / AbsenceGrade), but Hibernate
-- ddl-auto=update does NOT reliably add indexes to tables that already exist. Run this script ONCE against
-- the production database to create them on the existing tables. It is idempotent (safe to re-run).
--
-- Column names are the snake_case physical names produced by Spring Boot's default naming strategy.

------------------------------------------------------------------------------------------------------------
-- GRADES
------------------------------------------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_grades_class_student_subject' AND object_id = OBJECT_ID('GRADES'))
    CREATE INDEX idx_grades_class_student_subject ON GRADES (class_id, student_id, subject_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_grades_grade_type' AND object_id = OBJECT_ID('GRADES'))
    CREATE INDEX idx_grades_grade_type ON GRADES (grade_type);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_grades_exact_month' AND object_id = OBJECT_ID('GRADES'))
    CREATE INDEX idx_grades_exact_month ON GRADES (exact_month);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_grades_identifier' AND object_id = OBJECT_ID('GRADES'))
    CREATE INDEX idx_grades_identifier ON GRADES (identifier);
GO

------------------------------------------------------------------------------------------------------------
-- ABSENCE_GRADES
------------------------------------------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_absence_grades_student_class' AND object_id = OBJECT_ID('ABSENCE_GRADES'))
    CREATE INDEX idx_absence_grades_student_class ON ABSENCE_GRADES (student_id, class_id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_absence_grades_exact_month' AND object_id = OBJECT_ID('ABSENCE_GRADES'))
    CREATE INDEX idx_absence_grades_exact_month ON ABSENCE_GRADES (exact_month);
GO
