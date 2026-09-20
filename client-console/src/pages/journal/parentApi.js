import axios from "../utils/axios";

/**
 * The parent portal.
 *
 * Two calls serve every journal, because a journal decides its own shape. There
 * is no per-journal endpoint and no per-journal page: the school inventing one
 * next year has to reach parents without anyone writing code for it.
 *
 * Everything here is the published value. The working column is not exposed to
 * this console at all.
 */

/** The boxes on the landing page — journals the school has released. */
export const fetchParentJournals = async () => {
    const {data} = await axios.get("/api/parent/journals");
    return data;
};

/**
 * The buttons the landing page draws.
 *
 * Not the same as the journals above, and the difference is the whole reason
 * this exists: three of the school's five parent screens are the trimester
 * journal at different settings — per subject, summed across subjects, and the
 * year — so a button is an entry point rather than a journal.
 *
 * A journal nobody has configured stands in for itself, so this is a superset
 * of the journal list and never returns fewer boxes than there are journals.
 */
export const fetchParentMenu = async () => {
    const {data} = await axios.get("/api/parent/views");
    return data;
};

/**
 * One journal for the logged-in student.
 *
 * The student is never a parameter — it comes from the token, so changing a
 * number in the URL cannot read another child.
 */
export const fetchParentView = async ({uuid, periodId, subjectId, periodKind, summary}) => {
    if (!uuid) return null;
    const {data} = await axios.get(`/api/parent/journals/${uuid}`, {
        // periodKind only decides where to open. The server ignores it once
        // periodId is set, so the picker always wins. `summary` decides how many
        // columns: one mark per subject, or a single subject's whole row.
        params: {periodId, subjectId, periodKind, summary}
    });
    return data;
};

// ---- homework --------------------------------------------------------------

/**
 * A month of the calendar.
 *
 * Returns only the days that hold something, each with a total and how many the
 * parent has not opened — the grid itself is drawn from the month, so the
 * server has no reason to send thirty empty days.
 */
export const fetchHomeworkMonth = async (month) => {
    const {data} = await axios.get("/api/parent/homework", {params: {month}});
    return data;
};

/** One day, grouped by subject. Reading it marks nothing as seen. */
export const fetchHomeworkDay = async (date) => {
    if (!date) return null;
    const {data} = await axios.get(`/api/parent/homework/${date}`);
    return data;
};

/**
 * Records that these assignments have been opened.
 *
 * Batched and debounced by the caller rather than sent on every tap. Idempotent
 * at the database, so a retry after a dropped response is free — which is what
 * makes debouncing safe here.
 */
export const markHomeworkSeen = async (postUuids) => {
    if (!postUuids || postUuids.length === 0) return 0;
    const {data} = await axios.post("/api/parent/homework/seen", {postUuids});
    return data;
};

// ---- news ------------------------------------------------------------------

/** Published news, newest first. Institution-wide: every parent sees every item. */
export const fetchNews = async ({categoryId, page = 0, size = 10}) => {
    const {data} = await axios.get("/api/parent/news", {params: {categoryId, page, size}});
    return data;
};

/**
 * One news item, by uuid.
 *
 * Only for an article opened cold - pasted, bookmarked or refreshed - where
 * there is no list in hand to read it from. An article with its own address
 * needs it; the dialog it replaces did not.
 */
export const fetchNewsItem = async (uuid) => {
    if (!uuid) return null;
    const {data} = await axios.get(`/api/parent/news/${uuid}`);
    return data;
};

/** The categories the school files news under, for the filter. */
export const fetchNewsCategories = async () => {
    const {data} = await axios.get("/api/parent/news/categories");
    return data;
};

// ---- schedule, menu, the child's description -------------------------------

/**
 * Which boxes this child's school shows.
 *
 * From the server, not decided here: the primary/basic/secondary rule is about
 * the school, and the school is in the data. The console maps a name to a route.
 */
export const fetchParentModules = async () => {
    const {data} = await axios.get("/api/parent/modules");
    return data;
};

/** The class's weekly timetable, or null when it has not been written. */
export const fetchSchedule = async () => {
    const {data} = await axios.get("/api/parent/schedule");
    return data;
};

/** The class's weekly menu, same shape as the schedule minus the times. */
export const fetchMenu = async () => {
    const {data} = await axios.get("/api/parent/menu");
    return data;
};

/** What the school has written about this child, newest first. */
/** A month of the child's descriptions, in the homework calendar's shape. */
export const fetchDescriptionMonth = async (month) => {
    const {data} = await axios.get("/api/parent/characterizations/month", {params: {month}});
    return data;
};

/** One day's descriptions, grouped by subject. */
export const fetchDescriptionDay = async (date) => {
    if (!date) return null;
    const {data} = await axios.get(`/api/parent/characterizations/day/${date}`);
    return data;
};

export const fetchCharacterizations = async () => {
    const {data} = await axios.get("/api/parent/characterizations");
    return data;
};
