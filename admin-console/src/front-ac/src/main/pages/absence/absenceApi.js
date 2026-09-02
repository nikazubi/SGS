import axios from "../../../utils/axios";

/**
 * The two absence registers, which are two different endpoints because they are
 * two different things.
 *
 * Daily is its own table: a mark is a row keyed by student and date, and
 * clearing one deletes it. Monthly is a journal like any other - typed academic
 * hours, published to parents.
 */

// ---- daily -----------------------------------------------------------------

/**
 * A month of the daily register.
 *
 * The month is named by a period id, but nothing under it is: the columns come
 * back as dates, being the weekdays between the month's own two.
 */
export const fetchDailyGrid = async ({classGroupId, monthPeriodId}) => {
    if (!classGroupId || !monthPeriodId) return null;
    const {data} = await axios.get("/api/gradebook/absence/daily/grid", {
        params: {classGroupId, monthPeriodId}
    });
    return data;
};

/**
 * One day, for as many children as you like.
 *
 * No conflict can come back. Marking is insert-or-delete on a row keyed by child
 * and date, so two people saving the same column converge rather than one of
 * them losing a version check on a boolean.
 *
 * @returns the enrollment ids newly marked absent - the ones a parent will be
 *          told about, a quarter of an hour from now.
 */
export const markDailyAbsence = async ({classGroupId, date, marks}) => {
    const {data} = await axios.post("/api/gradebook/absence/daily/mark",
        {classGroupId, date, marks});
    return data;
};

// ---- monthly ---------------------------------------------------------------

/**
 * @param parentPeriodId the year; its months become the columns
 */
export const fetchAbsenceGrid = async ({classGroupId, parentPeriodId, journalUuid}) => {
    if (!classGroupId || !parentPeriodId || !journalUuid) return null;
    const {data} = await axios.get("/api/gradebook/absence/grid", {
        params: {classGroupId, parentPeriodId, journalUuid}
    });
    return data;
};

/** One month of hours - the ordinary grade batch underneath. */
export const markAbsence = async ({journalUuid, classGroupId, periodId, entries}) => {
    const {data} = await axios.post("/api/gradebook/absence/mark",
        {journalUuid, classGroupId, periodId, entries});
    return data;
};

/** The month's academic hours, and how many a student may miss. */
export const saveAbsenceSettings = async ({
                                              classGroupId, periodId,
                                              totalAcademicHours, permittedMissedHours
                                          }) =>
    axios.post("/api/gradebook/absence/settings", null, {
        params: {classGroupId, periodId, totalAcademicHours, permittedMissedHours}
    });
