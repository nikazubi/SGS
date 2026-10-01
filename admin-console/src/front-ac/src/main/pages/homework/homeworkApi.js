import axios from "../../../utils/axios";

/**
 * Homework.
 *
 * Staff side only — the parent side of every content module lands together in
 * phase 11, because it carries UI decisions the school has not made yet.
 */

/**
 * One subject's assignments, newest first.
 *
 * `limit` is the few the accordion shows; omitting it is the "see more" dialog
 * asking for the lot.
 */
export const fetchHomework = async ({classGroupId, subjectId, from, to, limit}) => {
    const {data} = await axios.get("/api/gradebook/homework", {
        params: {classGroupId, subjectId, from, to, limit}
    });
    return data;
};

/** So the list knows whether "see more" has anything behind it. */
export const countHomework = async ({classGroupId, subjectId, from, to}) => {
    const {data} = await axios.get("/api/gradebook/homework/count", {
        params: {classGroupId, subjectId, from, to}
    });
    return data;
};

export const fetchHomeworkItem = async (uuid) => {
    const {data} = await axios.get(`/api/gradebook/homework/${uuid}`);
    return data;
};

/** Create when the draft carries no uuid, update when it does. */
export const saveHomework = async (draft) => {
    const {data} = await axios.post("/api/gradebook/homework", draft);
    return data;
};

/** Release it to parents. No approval — this is not the grade publish flow. */
export const publishHomework = async (uuid) => {
    const {data} = await axios.post(`/api/gradebook/homework/${uuid}/publish`);
    return data;
};

/** Soft delete, so something a parent has already read leaves a trace. */
export const archiveHomework = async ({uuid, archived = true}) =>
    axios.post(`/api/gradebook/homework/${uuid}/archive`, null, {params: {archived}});

/**
 * Recovers the server's own message from a failed download.
 *
 * Asking for a blob means the error body arrives as one too, so the usual
 * `data[0].message` reads nothing and every failure looks identical. That is
 * how "no homework in this range" — a perfectly clear message the API already
 * sends — reached the user as a generic download failure.
 *
 * Falls through silently when the body is not the API's JSON: a proxy timing
 * out mid-download produces HTML, and a parse error there should not replace
 * the real problem.
 */
const withServerMessage = async (e) => {
    const body = e?.response?.data;
    if (!body || typeof body.text !== "function") {
        return e;
    }
    try {
        const parsed = JSON.parse(await body.text());
        const message = Array.isArray(parsed) ? parsed[0]?.message : parsed?.message;
        if (message) {
            e.serverMessage = message;
        }
    } catch (ignored) {
        // Not the API's JSON. Leave the caller its own wording.
    }
    return e;
};

/**
 * The list as a PDF, saved to the device.
 *
 * The same narrowing the list endpoint takes, so the document matches whatever
 * is on screen: no `subjectId` and no `uuid` is the whole class, `subjectId` is
 * one accordion, `uuid` is one row.
 *
 * Fetched rather than linked, because the API wants the bearer token and a
 * token in a query string ends up in logs and browser history.
 */
export const downloadHomeworkPdf = async ({classGroupId, subjectId, from, to, uuid, name}) => {
    let data;
    let headers;
    try {
        ({data, headers} = await axios.get("/api/gradebook/homework/pdf", {
            params: {classGroupId, subjectId, from, to, uuid},
            responseType: "blob",
        }));
    } catch (e) {
        throw await withServerMessage(e);
    }

    const blob = new Blob([data], {
        type: headers?.["content-type"] || "application/pdf",
    });
    const href = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = href;
    a.download = name || "davaleba.pdf";
    document.body.appendChild(a);
    a.click();
    a.remove();
    // Next tick: revoking synchronously can beat the download in some browsers
    // and hand the user an empty file.
    setTimeout(() => window.URL.revokeObjectURL(href), 0);
};
