import axios from "../../../utils/axios";

/**
 * Journals: the grids the school creates, names and sees in the menu.
 *
 * A journal is addressed by its uuid rather than its name — the name is the
 * menu label and is meant to be changed, so nothing may depend on it.
 */

export const fetchJournals = async (includeArchived = false) => {
    const {data} = await axios.get("/api/gradebook/journals", {
        params: {includeArchived}
    });
    return data;
};

/** One journal, for callers that need its shape rather than the whole list. */
export const fetchJournal = async (uuid) => {
    if (!uuid) return null;
    const {data} = await axios.get(`/api/gradebook/journals/${uuid}`);
    return data;
};

export const createJournal = async ({draft, periodSchemeId}) => {
    const {data} = await axios.post("/api/gradebook/journals", draft, {
        params: {periodSchemeId}
    });
    return data;
};

export const updateJournal = async ({uuid, draft}) => {
    const {data} = await axios.put(`/api/gradebook/journals/${uuid}`, draft);
    return data;
};

export const archiveJournal = async ({uuid, archived}) => {
    await axios.post(`/api/gradebook/journals/${uuid}/archive`, null, {params: {archived}});
};

export const fetchStructure = async (uuid) => {
    if (!uuid) return null;
    const {data} = await axios.get(`/api/gradebook/journals/${uuid}/structure`);
    return data;
};

/** The whole version at once — the shape a wizard and a column table both produce. */
export const saveStructure = async ({uuid, versionId, components}) => {
    const {data} = await axios.put(`/api/gradebook/journals/${uuid}/structure`,
        components, {params: {versionId}});
    return data;
};

export const activateVersion = async ({uuid, versionId}) => {
    const {data} = await axios.post(`/api/gradebook/journals/${uuid}/activate`,
        null, {params: {versionId}});
    return data;
};

/** Every journal and every column, for the formula picker. */
export const fetchPickableColumns = async (callerUuid) => {
    const {data} = await axios.get("/api/gradebook/journals/columns", {
        params: {callerUuid}
    });
    return data;
};

// ---- migration -----------------------------------------------------------

/**
 * Omitting class and period asks about every period still on an older version.
 * The preview is the migration with the writes suppressed, so its numbers
 * cannot disagree with what actually happens.
 */
export const previewMigration = async ({uuid, classGroupId, periodId}) => {
    const {data} = await axios.get(`/api/gradebook/journals/${uuid}/migrate/preview`, {
        params: {classGroupId, periodId}
    });
    return data;
};

export const migrate = async ({uuid, classGroupId, periodId}) => {
    const {data} = await axios.post(`/api/gradebook/journals/${uuid}/migrate`, null, {
        params: {classGroupId, periodId}
    });
    return data;
};
