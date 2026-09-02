package mthiebi.sgs.gradebook.service.export;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.gradebook.model.ClassGroup;
import mthiebi.sgs.gradebook.model.ClassSubject;
import mthiebi.sgs.gradebook.repository.ClassGroupRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Every class in one download.
 * <p>
 * The school archives a trimester by running an export per class and filing the
 * results - 47 downloads done by hand. This is the same exports, batched, with
 * the trimester and year chosen once.
 * <p>
 * The zip is written straight to the response as it is built. Holding 180
 * workbooks in memory to zip them at the end would be the obvious way and the
 * wrong one; streamed, memory is one workbook at a time regardless of how many
 * classes the school grows to.
 */
@Service
public class BulkExportService {

    private static final Logger log = LoggerFactory.getLogger(BulkExportService.class);

    @Autowired
    private GradeExportService exportService;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    /**
     * @param allowed class ids the caller may see, or empty for unrestricted -
     *                the same convention {@code ClassScopeGuard} uses, so a
     *                coordinator gets their class and a director gets the
     *                school from one endpoint.
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void writeMatrix(OutputStream out, Long periodId, String componentCode,
                            boolean splitByChildPeriod, String journalUuid, boolean converted,
                            Set<Long> allowed) throws IOException {

        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (ClassGroup group : classesInScope(allowed)) {
                addEntry(zip, folder(group) + fileName(group.getName()), () ->
                        exportService.matrix(group.getId(), periodId, componentCode,
                                splitByChildPeriod, journalUuid, converted));
            }
        }
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public void writeDetail(OutputStream out, Long periodId, String journalUuid,
                            boolean converted, Set<Long> allowed) throws IOException {

        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (ClassGroup group : classesInScope(allowed)) {
                for (ClassSubject cs : classGroupRepository.findClassSubjectsOf(group.getId())) {
                    Long subjectId = cs.getSubject().getId();
                    addEntry(zip,
                            folder(group) + fileName(group.getName() + "_" + cs.getSubject().getName()),
                            () -> exportService.detail(group.getId(), subjectId, periodId,
                                    journalUuid, converted));
                }
            }
        }
    }

    private List<ClassGroup> classesInScope(Set<Long> allowed) {
        List<ClassGroup> all = classGroupRepository.findForCurrentYear();
        if (allowed == null || allowed.isEmpty()) {
            return all;
        }
        return all.stream().filter(c -> allowed.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * One class that cannot be exported must not lose the other forty-six.
     * <p>
     * A class with no assignment, or a period the journal does not reach, throws
     * from the exporter. The failure is recorded as a readable file inside the
     * zip rather than swallowed, so whoever ran it can see which class is
     * missing and why - a silently short zip is the worse outcome, because it
     * looks complete.
     */
    private void addEntry(ZipOutputStream zip, String name, WorkbookSupplier supplier)
            throws IOException {
        try {
            Workbook workbook = supplier.get();
            try {
                zip.putNextEntry(new ZipEntry(name));
                workbook.write(zip);
                zip.closeEntry();
            } finally {
                workbook.close();
            }
        } catch (SGSException e) {
            log.warn("bulk export skipped {}: {}", name, e.getMessage());
            zip.putNextEntry(new ZipEntry(name + ".error.txt"));
            zip.write(("ვერ დაგენერირდა: " + e.getMessage()).getBytes("UTF-8"));
            zip.closeEntry();
        }
    }

    private String folder(ClassGroup group) {
        return safe(group.getName()) + "/";
    }

    private String fileName(String label) {
        return "IB_Mthiebi_" + safe(label) + ".xlsx";
    }

    /**
     * Zip entry names keep their Georgian but lose anything a filesystem will
     * not take. A subject called "მათემატიკა I/II" would otherwise create a
     * directory when the archive is unpacked.
     */
    private String safe(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "export";
        }
        return s.trim().replaceAll("[\\/:*?\"<>|]", "_");
    }

    @FunctionalInterface
    private interface WorkbookSupplier {
        Workbook get() throws SGSException;
    }
}
