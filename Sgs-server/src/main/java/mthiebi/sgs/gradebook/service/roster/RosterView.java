package mthiebi.sgs.gradebook.service.roster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * What the roster screens read.
 * <p>
 * Flat rows rather than entities. The console needs a class name next to a
 * student and a school name next to a class, and serialising the entities to
 * get them would either drag half the graph across the wire or fail on a lazy
 * proxy outside its session - the same lesson the grid views already learned.
 */
public final class RosterView {

    private RosterView() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentRow {
        private Long id;
        private String firstName;
        private String lastName;
        private String personalNumber;
        private String username;
        private String guardianEmail;
        private boolean active;

        /**
         * The enrollment for the year being looked at, and the class it points
         * to. Null when this child is not enrolled in that year at all - which
         * is a real state, not an error: a student record outlives any one year.
         */
        private Long enrollmentId;
        private Long classGroupId;
        private String className;
        private LocalDate leftOn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassRow {
        private Long id;
        private String name;
        private short level;
        private Long schoolId;
        private String schoolName;
        private Long academicYearId;
        private long studentCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectRow {
        private Long id;
        private String name;
        private String shortName;
        private boolean active;
        /**
         * How many classes take it. Zero is what makes deletion safe.
         */
        private long classCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSubjectRow {
        private Long id;
        private Long subjectId;
        private String subjectName;
        private int sortIndex;
        /**
         * Two ways of saying who teaches it, and the school has both. A name is
         * what 98 of them have; an account is what 3 of them have.
         */
        private String teacherName;
        private Long teacherUserId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlacementRow {
        private Long classGroupId;
        private String className;
        private LocalDate fromDate;
        /**
         * Null is the current placement.
         */
        private LocalDate toDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearRow {
        private Long id;
        private String code;
        private LocalDate startsOn;
        private LocalDate endsOn;
        private boolean current;
        private long classCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchoolRow {
        private Long id;
        private String code;
        private String name;
        private int ordinal;
    }
}
