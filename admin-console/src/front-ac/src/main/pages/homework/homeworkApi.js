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
