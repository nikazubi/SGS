package mthiebi.sgs.gradebook.model;

public enum TemplateVersionStatus {
    DRAFT,
    ACTIVE,
    /**
     * Referenced by a publication, so its shape can no longer change.
     */
    LOCKED,
    ARCHIVED
}
