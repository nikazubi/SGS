// Frontend mirror of the backend grade "view contract" (mthiebi.sgs.utils.GradeViewConstants).
//
// The backend fabricates synthetic "subject" rows for the behaviour / absence / rating columns and tags them
// with these sentinel ids. The frontend keys off them, so the values MUST stay in sync with the backend.
export const BEHAVIOUR_SUBJECT_ID = 9999;
export const ABSENCE_SUBJECT_ID = 8888;
export const RATING_SUBJECT_ID = 7777;

// Synthetic subject name keys used across the grade dashboards (per-semester variants carry a 1/2 suffix).
export const SYNTHETIC_SUBJECT_NAMES = {
    behaviour: ['behaviour', 'behaviour1', 'behaviour2'],
    absence: ['absence', 'absence1', 'absence2'],
    rating: ['rating'],
};
