import axios from "../../../utils/axios";

/**
 * The roster, against the model the rest of the console reads.
 *
 * Everything here writes `sgs`. The pages these replace wrote `dbo`, which is
 * why a student created in the old console never appeared in the gradebook —
 * see REWRITE-ROSTER.md. Nothing syncs the two, deliberately.
 */

const BASE = "/api/gradebook/roster";

// ---- years and schools -----------------------------------------------------

export const fetchYears = async () => {
    const {data} = await axios.get(`${BASE}/years`);
    return data;
};

export const fetchSchools = async () => {
    const {data} = await axios.get(`${BASE}/schools`);
    return data;
};

export const startYear = async (draft) => {
    const {data} = await axios.post(`${BASE}/years`, draft);
    return data;
};

export const makeYearCurrent = async (id) => {
    await axios.post(`${BASE}/years/${id}/current`);
};

// ---- subjects --------------------------------------------------------------

export const fetchSubjects = async (includeInactive = false) => {
    const {data} = await axios.get(`${BASE}/subjects`, {params: {includeInactive}});
    return data;
};

export const saveSubject = async (draft) => {
    const {data} = await axios.post(`${BASE}/subjects`, draft);
    return data;
};

export const deleteSubject = async (id) => {
    await axios.delete(`${BASE}/subjects/${id}`);
};

// ---- classes ---------------------------------------------------------------

export const fetchClassGroups = async (academicYearId) => {
    const {data} = await axios.get(`${BASE}/classes`, {params: {academicYearId}});
    return data;
};

export const saveClassGroup = async (draft) => {
    const {data} = await axios.post(`${BASE}/classes`, draft);
    return data;
};

export const deleteClassGroup = async (id) => {
    await axios.delete(`${BASE}/classes/${id}`);
};

// ---- what a class is taught ------------------------------------------------

export const fetchClassSubjects = async (classGroupId) => {
    if (!classGroupId) return [];
    const {data} = await axios.get(`${BASE}/classes/${classGroupId}/subjects`);
    return data;
};

export const addClassSubject = async ({classGroupId, draft}) => {
    const {data} = await axios.post(`${BASE}/classes/${classGroupId}/subjects`, draft);
    return data;
};

export const updateClassSubject = async ({classSubjectId, draft}) => {
    await axios.post(`${BASE}/classes/subjects/${classSubjectId}`, draft);
};

export const removeClassSubject = async (classSubjectId) => {
    await axios.delete(`${BASE}/classes/subjects/${classSubjectId}`);
};

export const reorderClassSubjects = async ({classGroupId, classSubjectIds}) => {
    await axios.post(`${BASE}/classes/${classGroupId}/subjects/reorder`, {classSubjectIds});
};

// ---- students --------------------------------------------------------------

export const fetchStudents = async ({academicYearId, classGroupId, search, includeInactive}) => {
    if (!academicYearId) return [];
    const {data} = await axios.get(`${BASE}/students`, {
        params: {academicYearId, classGroupId, search, includeInactive}
    });
    return data;
};

/**
 * The year goes in the query string because it is not a property of the
 * student — it says which enrollment the form's "class" field refers to.
 */
export const saveStudent = async ({draft, academicYearId}) => {
    const {data} = await axios.post(`${BASE}/students`, draft, {params: {academicYearId}});
    return data;
};

export const deactivateStudent = async (id) => {
    await axios.post(`${BASE}/students/${id}/deactivate`);
};

export const fetchPlacements = async ({studentId, academicYearId}) => {
    const {data} = await axios.get(`${BASE}/students/${studentId}/history`, {
        params: {academicYearId}
    });
    return data;
};

// ---- enrollment, as dated events -------------------------------------------

export const moveEnrollment = async ({enrollmentId, classGroupId, on}) => {
    await axios.post(`${BASE}/enrollments/${enrollmentId}/move`, {classGroupId, on});
};

export const leaveEnrollment = async ({enrollmentId, on}) => {
    await axios.post(`${BASE}/enrollments/${enrollmentId}/leave`, {on});
};
