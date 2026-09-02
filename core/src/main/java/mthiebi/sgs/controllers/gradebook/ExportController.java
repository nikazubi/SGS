package mthiebi.sgs.controllers.gradebook;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.service.export.GradeExportService;
import mthiebi.sgs.utils.AuthConstants;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

/**
 * Excel exports.
 * <p>
 * Two endpoints where there were four, because the four differed only in which
 * component and which periods they printed. The base path is also no longer
 * literally {@code /test}.
 */
@RestController
@RequestMapping("/api/gradebook/export")
public class ExportController {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private GradeExportService exportService;

    @Autowired
    private ClassScopeGuard classScope;

    @Autowired
    private mthiebi.sgs.gradebook.service.export.BulkExportService bulkExportService;

    /**
     * Students down, subjects across, one component per cell.
     * <p>
     * {@code splitByChildPeriod} turns the year into a column per trimester,
     * which is the annual export; left off it is a single period, which is the
     * monthly one.
     */
    @GetMapping("/matrix")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void matrix(@RequestParam Long classGroupId,
                       @RequestParam Long periodId,
                       @RequestParam String componentCode,
                       @RequestParam(defaultValue = "false") boolean splitByChildPeriod,
                       @RequestParam(required = false) String className,
                       @RequestParam(required = false) String journalUuid,
                       @RequestParam(defaultValue = "false") boolean converted,
                       @RequestHeader("authorization") String authHeader,
                       HttpServletResponse response) throws SGSException, IOException {
        classScope.check(authHeader, classGroupId);
        write(exportService.matrix(classGroupId, periodId, componentCode, splitByChildPeriod,
                journalUuid, converted), response, className);
    }

    /**
     * Students down, the template's components across, for one subject.
     */
    @GetMapping("/detail")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void detail(@RequestParam Long classGroupId,
                       @RequestParam Long subjectId,
                       @RequestParam Long periodId,
                       @RequestParam(required = false) String className,
                       @RequestParam(required = false) String journalUuid,
                       @RequestParam(defaultValue = "false") boolean converted,
                       @RequestHeader("authorization") String authHeader,
                       HttpServletResponse response) throws SGSException, IOException {
        classScope.check(authHeader, classGroupId);
        write(exportService.detail(classGroupId, subjectId, periodId, journalUuid, converted),
                response, className);
    }

    // ---- bulk ------------------------------------------------------------

    /**
     * Every class the caller may see, in one zip.
     * <p>
     * Scope comes from {@code ClassScopeGuard} rather than from a parameter, so
     * a coordinator gets their class and a director gets the school from the
     * same button - and no request can widen its own scope by naming ids.
     * <p>
     * The period is a trimester (or the year), chosen once. There is no semester
     * anywhere in this: the school corrected that wording, and the period tree
     * has only ever held trimesters.
     */
    @GetMapping("/bulk/matrix")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void bulkMatrix(@RequestParam Long periodId,
                           @RequestParam String componentCode,
                           @RequestParam(defaultValue = "false") boolean splitByChildPeriod,
                           @RequestParam(required = false) String journalUuid,
                           @RequestParam(required = false) String label,
                           @RequestParam(defaultValue = "false") boolean converted,
                           @RequestHeader("authorization") String authHeader,
                           HttpServletResponse response) throws SGSException, IOException {
        writeZip(response, label);
        bulkExportService.writeMatrix(response.getOutputStream(), periodId, componentCode,
                splitByChildPeriod, journalUuid, converted, scopeOf(authHeader));
    }

    @GetMapping("/bulk/detail")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void bulkDetail(@RequestParam Long periodId,
                           @RequestParam(required = false) String journalUuid,
                           @RequestParam(required = false) String label,
                           @RequestParam(defaultValue = "false") boolean converted,
                           @RequestHeader("authorization") String authHeader,
                           HttpServletResponse response) throws SGSException, IOException {
        writeZip(response, label);
        bulkExportService.writeDetail(response.getOutputStream(), periodId, journalUuid,
                converted, scopeOf(authHeader));
    }

    /**
     * Moved onto ClassScopeGuard as visibleClassGroupIds.
     * <p>
     * It was right here and nowhere else: three other listings read
     * allowedClassGroupIds directly and treated an empty set as unrestricted,
     * which hands the whole school to a user whose grant has gone stale. One
     * accessor means one answer.
     */
    private Set<Long> scopeOf(String authHeader) throws SGSException {
        return classScope.visibleClassGroupIds(authHeader);
    }

    private void writeZip(HttpServletResponse response, String label) throws IOException {
        String name = "IB_Mthiebi_" + (label == null || label.isEmpty() ? "export" : label) + ".zip";
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
                .replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"export.zip\"; filename*=UTF-8''" + encoded);
    }

    private void write(Workbook workbook, HttpServletResponse response, String className)
            throws IOException {
        // The filename carries Georgian, so it needs RFC 5987 encoding rather
        // than being dropped raw into the header.
        String name = "IB_Mthiebi_" + (className == null || className.isEmpty()
                ? "export" : className) + ".xlsx";
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
                .replace("+", "%20");

        response.setContentType(XLSX);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"export.xlsx\"; filename*=UTF-8''" + encoded);
        try {
            workbook.write(response.getOutputStream());
        } finally {
            workbook.close();
        }
    }
}
