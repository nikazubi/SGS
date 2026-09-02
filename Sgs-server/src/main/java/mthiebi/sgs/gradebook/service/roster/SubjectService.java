package mthiebi.sgs.gradebook.service.roster;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.SGSExceptionCode;
import mthiebi.sgs.gradebook.model.Subject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The subjects the school teaches.
 * <p>
 * A flat global list, which is what the model says: {@code uq_subject_name} is
 * on the name alone. Who teaches a subject, and in which class, lives on
 * class_subject - see {@link ClassService} - because the same subject is taught
 * to different classes by different people.
 * <p>
 * Named {@code SubjectService} despite a legacy interface of that name existing
 * in {@code mthiebi.sgs.service}. That one is an interface, so its bean is
 * {@code subjectServiceImpl} and this one is {@code subjectService}; the same
 * arrangement {@code ChangeRequestService} has lived under since phase 3.
 */
@Service
public class SubjectService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<RosterView.SubjectRow> list(boolean includeInactive) {
        List<Subject> subjects = em.createQuery(
                        "select s from Subject s "
                                + "where (:all = true or s.active = true) order by s.name", Subject.class)
                .setParameter("all", includeInactive)
                .getResultList();

        return subjects.stream()
                .map(s -> new RosterView.SubjectRow(s.getId(), s.getName(), s.getShortName(),
                        s.isActive(), classCount(s.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Subject save(RosterDraft.Subject draft) throws SGSException {
        String name = trimmed(draft.getName());
        if (name.isEmpty()) {
            throw new SGSException(SGSExceptionCode.BAD_REQUEST, "საგნის სახელი სავალდებულოა");
        }
        requireNameFree(name, draft.getId());

        Subject subject = draft.getId() == null ? new Subject() : find(draft.getId());
        subject.setName(name);
        subject.setShortName(blankToNull(draft.getShortName()));
        if (draft.getActive() != null) {
            subject.setActive(draft.getActive());
        }
        em.persist(subject);
        em.flush();
        return subject;
    }

    /**
     * Removed only while nothing takes it.
     * <p>
     * The legacy page deleted the subject and left the class_subject rows
     * pointing at nothing, which is how a class ends up with a blank column
     * nobody can explain. A subject that has been taught is deactivated
     * instead - it has grades hanging off it through class_subject and those
     * are not ours to destroy.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) throws SGSException {
        Subject subject = find(id);
        long used = classCount(id);
        if (used > 0) {
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "საგანს " + used + " კლასი სწავლობს - წაშლა შეუძლებელია. "
                            + "გამორთეთ ნაცვლად წაშლისა.");
        }
        em.remove(subject);
    }

    // ---- helpers ------------------------------------------------------------

    private long classCount(Long subjectId) {
        return em.createQuery(
                        "select count(cs) from ClassSubject cs where cs.subject.id = :id", Long.class)
                .setParameter("id", subjectId)
                .getSingleResult();
    }

    private void requireNameFree(String name, Long selfId) throws SGSException {
        List<Long> clash = em.createQuery(
                        "select s.id from Subject s where s.name = :n", Long.class)
                .setParameter("n", name)
                .getResultList();
        clash.remove(selfId);
        if (!clash.isEmpty()) {
            throw new SGSException(SGSExceptionCode.CONFLICT,
                    "ასეთი სახელის საგანი უკვე არსებობს");
        }
    }

    private Subject find(Long id) throws SGSException {
        Subject subject = em.find(Subject.class, id);
        if (subject == null) {
            throw new SGSException(SGSExceptionCode.NOT_FOUND, "საგანი ვერ მოიძებნა");
        }
        return subject;
    }

    static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    static String blankToNull(String value) {
        String trimmed = trimmed(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
