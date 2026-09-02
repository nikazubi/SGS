import axios from "../../../utils/axios";

/**
 * The four content modules beyond homework: schedule, menu, characterization
 * and news.
 *
 * Staff side only — the parent side of every module lands together in phase 11.
 */

// ---- standing documents: schedule and menu ------------------------------
//
// One per class, entered once for the year and adjusted occasionally, so these
// fetch a single document rather than a list. `kind` is "schedule" or "menu" and
// picks the endpoint, not a parameter — the two have separate permissions, and a
// shared endpoint would let either one edit both.

export const fetchStanding = async (kind, classGroupId) => {
    if (!classGroupId) return null;
    const {data} = await axios.get(`/api/gradebook/${kind}`, {params: {classGroupId}});
    return data || null;
};

export const saveStanding = async (kind, draft) => {
    const {data} = await axios.post(`/api/gradebook/${kind}`, draft);
    return data;
};

export const publishStanding = async (kind, uuid) => {
    const {data} = await axios.post(`/api/gradebook/${kind}/${uuid}/publish`);
    return data;
};

// ---- characterization ----------------------------------------------------

export const fetchCharacterizations = async ({classGroupId, subjectId, from, to, limit}) => {
    const {data} = await axios.get("/api/gradebook/characterization", {
        params: {classGroupId, subjectId, from, to, limit}
    });
    return data;
};

export const fetchCharacterization = async (uuid) => {
    const {data} = await axios.get(`/api/gradebook/characterization/${uuid}`);
    return data;
};

export const saveCharacterization = async (draft) => {
    const {data} = await axios.post("/api/gradebook/characterization", draft);
    return data;
};

export const publishCharacterization = async (uuid) => {
    const {data} = await axios.post(`/api/gradebook/characterization/${uuid}/publish`);
    return data;
};

export const archiveCharacterization = async ({uuid, archived = true}) =>
    axios.post(`/api/gradebook/characterization/${uuid}/archive`, null, {params: {archived}});

// ---- news ----------------------------------------------------------------

export const fetchNews = async ({categoryId, from, to, limit} = {}) => {
    const {data} = await axios.get("/api/gradebook/news", {
        params: {categoryId, from, to, limit}
    });
    return data;
};

export const fetchNewsItem = async (uuid) => {
    const {data} = await axios.get(`/api/gradebook/news/${uuid}`);
    return data;
};

export const saveNews = async (draft) => {
    const {data} = await axios.post("/api/gradebook/news", draft);
    return data;
};

export const publishNews = async (uuid) => {
    const {data} = await axios.post(`/api/gradebook/news/${uuid}/publish`);
    return data;
};

export const archiveNews = async ({uuid, archived = true}) =>
    axios.post(`/api/gradebook/news/${uuid}/archive`, null, {params: {archived}});

export const fetchCategories = async () => {
    const {data} = await axios.get("/api/gradebook/news/categories");
    return data;
};

/** Find by name or create, so the autocomplete accepts something new. */
export const addCategory = async (name) => {
    const {data} = await axios.post("/api/gradebook/news/categories", null, {params: {name}});
    return data;
};

/**
 * Upload a picture. The server downscales and re-encodes it, so what comes back
 * describes what was actually stored rather than what was sent.
 */
export const uploadImage = async (file) => {
    const form = new FormData();
    form.append("file", file);
    const {data} = await axios.post("/api/gradebook/news/images", form,
        {headers: {"Content-Type": "multipart/form-data"}});
    return data;
};

/**
 * The picture, as an object URL an <img> can use.
 *
 * Fetched through axios rather than pointed at directly, because the endpoint is
 * behind MANAGE_NEWS and auth here is a bearer token added by an interceptor —
 * a plain `<img src>` would go out with no header and come back 403. The caller
 * must revoke the URL when it is done with it.
 */
export const fetchImageObjectUrl = async (uuid) => {
    if (!uuid) return null;
    const response = await axios.get(`/api/gradebook/news/images/${uuid}`,
        {responseType: "blob"});
    return window.URL.createObjectURL(response.data);
};
