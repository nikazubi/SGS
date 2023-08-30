package mthiebi.sgs.impl;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.service.ExportWordService;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExportWordServiceImpl implements ExportWordService {

    private static final int NUMBER_OF_SUBJECTS_PER_PAGE = 4;
    private static final String[] FIRST_SEMESTER_MONTH_NAMES = {"სექტემბერი-ოქტომბერი", "ნოემბერი", "დეკემბერი"};
    private static final String[] FIRST_SEMESTER_MONTH_INT = {"","9","11", "12"};
    private static final String[] SECOND_SEMESTER_MONTH_NAMES = {"იანვარი-თებერვალი", "მარტი", "აპრილი", "მაისი", "ივნისი"};
    private static final String[] SECOND_SEMESTER_MONTH_INT = {"","1", "3", "4", "5", "6"};

    @Override
    public void exportSemesterGrades(Map<Student, Map<Subject, Map<Integer, BigDecimal>>> grades, String semester) {
        try (XWPFDocument document = new XWPFDocument()) {

            CTBody body = document.getDocument().getBody();
            CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();

            pageSize.setOrient(STPageOrientation.LANDSCAPE);
            pageSize.setW(BigInteger.valueOf(842 * 20));
            pageSize.setH(BigInteger.valueOf(595 * 20));

            List<Student> students = new ArrayList<>(grades.keySet());
            List<Subject> subjects = new ArrayList<>(grades.get(students.get(0)).keySet());
            int numberOfMonths = "firstSemester".equals(semester)? 3 : 5;
            int numberOfColumns = 1 + (subjects.size() *  numberOfMonths);
            BigDecimal numOfPages = BigDecimal.valueOf(numberOfColumns).divide(BigDecimal.valueOf(NUMBER_OF_SUBJECTS_PER_PAGE * numberOfMonths), RoundingMode.CEILING);
            Map<String, Subject> subjectWithNames = subjects.stream().collect(Collectors.toMap(Subject::getName, Function.identity()));
            List<String> subjectNames =  new ArrayList<>(subjectWithNames.keySet());
            subjectNames.add(0, "მოსწავლის სახელი და გვარი");
            String[] arraySubjectNames = subjectNames.toArray(new String[0]);
            String[] months = "firstSemester".equals(semester)? FIRST_SEMESTER_MONTH_NAMES : SECOND_SEMESTER_MONTH_NAMES;
            String[] monthsNum = "firstSemester".equals(semester)? FIRST_SEMESTER_MONTH_INT : SECOND_SEMESTER_MONTH_INT;

            for (int i = 0; i < numOfPages.longValue(); i++){
                 String[] headers = getColumnsForPage(arraySubjectNames, i);
                 String[][] studentData = new String[students.size()][headers.length + monthsNum.length + 1];
                 for (int j = 0; j < students.size(); j ++) {
                     String studentName = students.get(j).getFirstName() + " " + students.get(j).getLastName();
                     String[] gradeArr = new String[headers.length + monthsNum.length + 1];
                     gradeArr[0] = studentName;
                     for (int k = 1; k < headers.length; k ++) {
                         Subject subject = subjectWithNames.get(headers[k]);
                         for (int m = 1; m < monthsNum.length; m ++) {
                             gradeArr[k + m] =  grades.get(students.get(j)).get(subject).get(Integer.valueOf(monthsNum[m])).toString();
                         }
                     }
                     studentData[j] = gradeArr;
                 }
                 createTable(document, headers, months, studentData);
                 if (i + 1 != numOfPages.longValue()) {
                     XWPFParagraph pageBreakParagraph = document.createParagraph();
                     XWPFRun pageBreakRun = pageBreakParagraph.createRun();
                     pageBreakRun.addBreak(BreakType.PAGE);
                 }
            }
            // Save the document
            try (FileOutputStream out = new FileOutputStream("WTF.docx")) {
                document.write(out);
                System.out.println("Word document generated successfully.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    XWPFTable createTable(XWPFDocument document, String[] headers, String[] months, String[][] studentData){
        XWPFTable table = document.createTable(1, 12);

        // Set column widths for the headers and cells
        table.setWidthType(TableWidthType.PCT);

        for (int i = 0; i < headers.length; i++) {
            table.getRow(0).getCell(i).setWidth(i == 0? "20%" : "15%");
            table.getRow(0).getCell(i).setText(headers[i]);
            table.getRow(0).getCell(1).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            for(int j = 0; j < months.length; j ++){
                table.getRow(0).getCell(j + 2).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                table.getRow(0).getCell(j + 2).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            }
        }

        XWPFTableRow monthRow = table.createRow();
        for (int i = 0; i < months.length; i++) {
            monthRow.getCell(i).setText(months[i]);
        }

        for (String[] student : studentData) {
            XWPFTableRow studentRow = table.createRow();
            for (int i = 0; i < student.length; i++) {
                studentRow.getCell(i).setText(student[i]);
            }
        }

        return table;
    }

    private String[] getColumnsForPage(String[] cols, int page) {
        int startIdx = (page) * NUMBER_OF_SUBJECTS_PER_PAGE;
        int endIdx = Math.min(startIdx + NUMBER_OF_SUBJECTS_PER_PAGE, cols.length);
        return Arrays.copyOfRange(cols, startIdx, endIdx);
    }
}
