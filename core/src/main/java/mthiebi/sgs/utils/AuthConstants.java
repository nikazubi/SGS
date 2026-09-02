package mthiebi.sgs.utils;

public class AuthConstants {
    public static final String MANAGE_STUDENT = "MANAGE_STUDENT";
    public static final String VIEW_STUDENT = "VIEW_STUDENT";
    public static final String MANAGE_ACADEMY_CLASS = "MANAGE_ACADEMY_CLASS";
    public static final String VIEW_ACADEMY_CLASS = "VIEW_ACADEMY_CLASS";
    public static final String MANAGE_SUBJECT = "MANAGE_SUBJECT";
    public static final String VIEW_SUBJECT = "VIEW_SUBJECT";
    public static final String ADD_GRADES = "ADD_GRADES";
    public static final String MANAGE_GRADES = "MANAGE_GRADES";
    public static final String MANAGE_SYSTEM_USER = "MANAGE_SYSTEM_USER";
    public static final String MANAGE_CHANGE_REQUESTS = "MANAGE_CHANGE_REQUESTS";
    public static final String VIEW_CHANGE_REQUESTS = "VIEW_CHANGE_REQUESTS";
    public static final String MANAGE_CLOSED_PERIOD = "MANAGE_CLOSED_PERIOD";
    /**
     * Creating and editing journals: which columns exist and how they are calculated.
     * Separate from MANAGE_GRADES so that entering marks does not imply changing
     * the rules they are worked out by.
     */
    public static final String MANAGE_TEMPLATES = "MANAGE_TEMPLATES";
    public static final String VIEW_SYSTEM_USER_GROUP = "VIEW_SYSTEM_USER_GROUP";
    public static final String MANAGE_TOTAL_ABSENCE = "MANAGE_TOTAL_ABSENCE";
    /**
     * Setting and publishing homework. Per module rather than one MANAGE_CONTENT,
     * so a subject teacher can be allowed to set homework without also being
     * allowed to publish school news. The other four arrive with their modules.
     */
    public static final String MANAGE_HOMEWORK = "MANAGE_HOMEWORK";
    /**
     * The class's standing weekly routine.
     */
    public static final String MANAGE_SCHEDULE = "MANAGE_SCHEDULE";
    /**
     * The class's standing weekly menu.
     */
    public static final String MANAGE_MENU = "MANAGE_MENU";
    /**
     * Written accounts of a student, per subject.
     */
    public static final String MANAGE_CHARACTERIZATION = "MANAGE_CHARACTERIZATION";
    /**
     * School-wide news. The one content module that is not class-scoped.
     */
    public static final String MANAGE_NEWS = "MANAGE_NEWS";
}