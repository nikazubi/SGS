package mthiebi.sgs.models;

public enum AbsenceGradeType {
    SEPTEMBER_OCTOBER(9),
    NOVEMBER(11),
    DECEMBER(12),
    JANUARY_FEBRUARY(1),
    MARCH(2),
    APRIL(3),
    MAY(4);

    private final int monthNumber;

    AbsenceGradeType(int monthNumber) {
        this.monthNumber = monthNumber;
    }

    public static AbsenceGradeType getMonthByNumber(int number) {
        for (AbsenceGradeType month : AbsenceGradeType.values()) {
            if (month.getMonthNumber() == number) {
                return month;
            }
        }
        return null;
    }

    public int getMonthNumber() {
        return monthNumber;
    }
}
