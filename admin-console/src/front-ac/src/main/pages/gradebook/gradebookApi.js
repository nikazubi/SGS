import axios from "../../../utils/axios";

/**
 * The gradebook endpoints.
 *
 * One call draws the screen and one call saves a batch of cells. The page this
 * replaces fetched the grid, the class and the subject separately, and then
 * refetched the whole grid after every single cell edit.
 */

export const fetchClasses = async () => {
    const {data} = await axios.get("/api/gradebook/classes");
    return data;
};

export const fetchSubjects = async (classGroupId) => {
    if (!classGroupId) return [];
    const {data} = await axios.get(`/api/gradebook/classes/${classGroupId}/subjects`);
    return data;
};

/**
 * Only the periods this journal is filled in on, plus the year when it has
 * columns that roll up there. A monthly journal offering trimesters would
 * just be a way to open an empty grid.
 */
/** The class list, for pickers that target particular children. */
/**
 * Periods at one level of the tree.
 *
 * The absence register picks the level *above* its columns — a month for the
 * daily register, the year for the monthly one — which fetchPeriods cannot
 * give it, because that narrows to the journal's own level.
 */
export const fetchPeriodsAtDepth = async (classGroupId, depth) => {
    if (!classGroupId) return [];
    const {data} = await axios.get(`/api/gradebook/classes/${classGroupId}/periods-at`,
        {params: {depth}});
    return data;
};

export const fetchStudents = async (classGroupId) => {
    if (!classGroupId) return [];
    const {data} = await axios.get(`/api/gradebook/classes/${classGroupId}/students`);
    return data;
};

export const fetchPeriods = async (classGroupId, journalUuid) => {
    if (!classGroupId) return [];
    const {data} = await axios.get(`/api/gradebook/classes/${classGroupId}/periods`,
        {params: {journalUuid}});
    return data;
};

export const fetchGrid = async ({classGroupId, subjectId, periodId, journalUuid}) => {
    if (!classGroupId || !periodId) return null;
    const {data} = await axios.get("/api/gradebook/grid", {
        params: {classGroupId, subjectId, periodId, journalUuid}
    });
    return data;
};

/**
 * One flush. Every dirty cell goes in a single request and the recomputed
 * values come back on the response, so the client patches its own state rather
 * than invalidating and refetching.
 */
export const saveGradeBatch = async ({
                                         journalUuid, classGroupId, subjectId, periodId,
                                         entries
                                     }) => {
    const {data} = await axios.post("/api/gradebook/grades/batch", {
        journalUuid, classGroupId, subjectId, periodId, entries
    });
    return data;
};

/**
 * @param journalUuid required in practice. The endpoint accepts it as optional
 *        and falls back to the first active journal, so omitting it made
 *        "how was this calculated?" return 400 "unknown column" for a derived
 *        cell in every journal but one.
 */
export const explainCell = async ({
                                      enrollmentId, subjectId, periodId, componentCode,
                                      journalUuid
                                  }) => {
    const {data} = await axios.get("/api/gradebook/grades/explain", {
        params: {enrollmentId, subjectId, periodId, componentCode, journalUuid}
    });
    return data;
};

// ---- publication ---------------------------------------------------------

/**
 * Release a (class, period) to parents, optionally narrowed to one subject.
 * Republishing is normal: it picks up marks entered since and values whose
 * inputs have moved.
 */
/**
 * Release a period to parents.
 *
 * `journalUuid` matters because publication reaches the levels beneath the
 * period: omit it and publishing the absence register's year releases every
 * journal's cells for the whole year.
 */
export const publish = async ({classGroupId, periodId, subjectId, journalUuid}) => {
    const {data} = await axios.post("/api/gradebook/publish", {
        classGroupId, periodId, subjectId, journalUuid
    });
    return data;
};

export const fetchPublications = async (classGroupId) => {
    const {data} = await axios.get("/api/gradebook/publications", {
        params: {classGroupId}
    });
    return data;
};

// ---- change requests -----------------------------------------------------

export const raiseChangeRequest = async ({
                                             gradeEntryId, requestedValue,
                                             requestedSpecialValue, reason
                                         }) => {
    const {data} = await axios.post("/api/gradebook/change-requests", {
        gradeEntryId, requestedValue, requestedSpecialValue, reason
    });
    return data;
};

export const fetchChangeRequests = async ({status, classGroupId}) => {
    const {data} = await axios.get("/api/gradebook/change-requests", {
        params: {status, classGroupId}
    });
    return data;
};

export const decideChangeRequest = async ({changeRequestId, approve, comment}) => {
    const {data} = await axios.post("/api/gradebook/change-requests/decide", {
        changeRequestId, approve, comment
    });
    return data;
};

// ---- exports -------------------------------------------------------------

/**
 * Downloads a workbook.
 *
 * The filename comes from the server's Content-Disposition, which carries the
 * Georgian class name RFC 5987 encoded - so it is read back rather than
 * rebuilt here, as the old export hooks did.
 */
const downloadWorkbook = async (url, params, fallbackName) => {
    const response = await axios.get(url, {responseType: "blob", params});

    const disposition = response.headers["content-disposition"] || "";
    const encoded = /filename\*=UTF-8''([^;]+)/.exec(disposition);
    const name = encoded ? decodeURIComponent(encoded[1]) : fallbackName;

    const href = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = href;
    link.setAttribute("download", name);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(href);
};

/** Students down, the template's columns across, for one subject. */
export const exportDetail = ({
                                 classGroupId, subjectId, periodId, className,
                                 journalUuid, converted
                             }) =>
    downloadWorkbook("/api/gradebook/export/detail",
        {classGroupId, subjectId, periodId, className, journalUuid, converted},
        "export.xlsx");

/** Students down, subjects across, one column per cell. */
export const exportMatrix = ({
                                 classGroupId, periodId, componentCode, splitByChildPeriod,
                                 className, journalUuid, converted
                             }) =>
    downloadWorkbook("/api/gradebook/export/matrix",
        {
            classGroupId, periodId, componentCode, splitByChildPeriod, className,
            journalUuid, converted
        },
        "export.xlsx");

/**
 * The same exports, for every class the user may see, as one zip.
 *
 * There is no class parameter on purpose: scope comes from the caller's own
 * grants on the server, so a coordinator gets their class and a director gets
 * the school from the same button, and no request can widen its own scope.
 */
export const exportBulkDetail = ({periodId, journalUuid, label, converted}) =>
    downloadWorkbook("/api/gradebook/export/bulk/detail",
        {periodId, journalUuid, label, converted}, "export.zip");

export const exportBulkMatrix = ({
                                     periodId, componentCode, splitByChildPeriod,
                                     journalUuid, label, converted
                                 }) =>
    downloadWorkbook("/api/gradebook/export/bulk/matrix",
        {periodId, componentCode, splitByChildPeriod, journalUuid, label, converted},
        "export.zip");
