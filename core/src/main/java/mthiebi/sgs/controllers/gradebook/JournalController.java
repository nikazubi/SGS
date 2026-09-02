package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.service.journal.ColumnRef;
import mthiebi.sgs.gradebook.service.journal.ComponentDraft;
import mthiebi.sgs.gradebook.service.journal.JournalDraft;
import mthiebi.sgs.gradebook.service.journal.JournalService;
import mthiebi.sgs.gradebook.service.journal.JournalView;
import mthiebi.sgs.gradebook.service.journal.MigrationPlan;
import mthiebi.sgs.gradebook.service.journal.MigrationService;
import mthiebi.sgs.gradebook.service.journal.SaveResult;
import mthiebi.sgs.gradebook.service.journal.VersionStructure;
import mthiebi.sgs.utils.AuthConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Journals: the grids the school creates, names and sees in the menu.
 * <p>
 * Listing is open to anyone who may enter marks, because the menu is built from
 * it. Everything that changes a journal is behind MANAGE_TEMPLATES - entering
 * marks should not imply the ability to change how they are calculated.
 */
@RestController
@RequestMapping("/api/gradebook/journals")
public class JournalController {

    @Autowired
    private JournalService journalService;

    @Autowired
    private MigrationService migrationService;

    @Autowired
    private ActorResolver actorResolver;

    // ---- the menu --------------------------------------------------------

    @GetMapping
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_TEMPLATES})
    public List<JournalView> list(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return journalService.list(includeArchived);
    }

    @GetMapping("/{uuid}")
    @Secured({AuthConstants.ADD_GRADES, AuthConstants.MANAGE_GRADES,
            AuthConstants.MANAGE_TEMPLATES})
    public JournalView get(@PathVariable String uuid) throws SGSException {
        return journalService.get(uuid);
    }

    // ---- the wizard ------------------------------------------------------

    @PostMapping
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public JournalView create(@RequestBody JournalDraft draft,
                              @RequestParam Long periodSchemeId) throws SGSException {
        return journalService.create(draft, periodSchemeId);
    }

    @PutMapping("/{uuid}")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public JournalView update(@PathVariable String uuid,
                              @RequestBody JournalDraft draft) throws SGSException {
        return journalService.update(uuid, draft);
    }

    /**
     * Removed from the menu, never deleted - grades point at it.
     */
    @PostMapping("/{uuid}/archive")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public void archive(@PathVariable String uuid,
                        @RequestParam(defaultValue = "true") boolean archived)
            throws SGSException {
        journalService.archive(uuid, archived);
    }

    @PostMapping("/reorder")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public void reorder(@RequestBody List<String> uuidsInOrder) throws SGSException {
        journalService.reorder(uuidsInOrder);
    }

    // ---- the editor ------------------------------------------------------

    @GetMapping("/{uuid}/structure")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public VersionStructure structure(@PathVariable String uuid) throws SGSException {
        return journalService.currentStructure(uuid);
    }

    /**
     * The whole version at once.
     * <p>
     * Posted entire rather than as per-column CRUD: it is what a wizard and a
     * spreadsheet-shaped editor both produce, and validation gets to see the
     * complete structure instead of judging one column at a time. If the
     * version already holds marks the save forks a draft and says so.
     */
    @PutMapping("/{uuid}/structure")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public SaveResult save(@PathVariable String uuid,
                           @RequestParam Long versionId,
                           @RequestBody List<ComponentDraft> components) throws SGSException {
        return journalService.save(uuid, versionId, components);
    }

    @PostMapping("/{uuid}/activate")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public SaveResult activate(@PathVariable String uuid,
                               @RequestParam Long versionId) throws SGSException {
        return journalService.activate(uuid, versionId);
    }

    /**
     * Every journal and every column, for the formula picker.
     */
    @GetMapping("/columns")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public List<ColumnRef> columns(@RequestParam(required = false) String callerUuid)
            throws SGSException {
        return journalService.pickableColumns(callerUuid);
    }

    // ---- migration -------------------------------------------------------

    /**
     * What moving a period onto the current version would do.
     * <p>
     * The same walk as the migration with the writes suppressed, so the numbers
     * in the prompt cannot disagree with what happens.
     */
    @GetMapping("/{uuid}/migrate/preview")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public MigrationPlan previewMigration(@PathVariable String uuid,
                                          @RequestParam(required = false) Long classGroupId,
                                          @RequestParam(required = false) Long periodId)
            throws SGSException {
        return classGroupId == null || periodId == null
                ? migrationService.previewAll(uuid)
                : migrationService.preview(uuid, classGroupId, periodId);
    }

    /**
     * Apply it. Always recalculates - a period moved without recomputing would
     * hold values the new rules never produced.
     * <p>
     * Omitting the class and period migrates every period still on an older
     * version, which is the useful and dangerous one; the preview above is what
     * the confirmation shows first.
     */
    @PostMapping("/{uuid}/migrate")
    @Secured({AuthConstants.MANAGE_TEMPLATES})
    public MigrationPlan migrate(@RequestHeader("authorization") String authHeader,
                                 @PathVariable String uuid,
                                 @RequestParam(required = false) Long classGroupId,
                                 @RequestParam(required = false) Long periodId)
            throws SGSException {
        Long actor = actorResolver.idOf(authHeader);
        return classGroupId == null || periodId == null
                ? migrationService.migrateAll(uuid, actor)
                : migrationService.migrate(uuid, classGroupId, periodId, actor);
    }
}
