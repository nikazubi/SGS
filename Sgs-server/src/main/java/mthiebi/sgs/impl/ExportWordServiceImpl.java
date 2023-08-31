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
    private static final String[] FIRST_SEMESTER_MONTH_NAMES = { "სექტემბერი-ოქტომბერი", "ნოემბერი", "დეკემბერი"};
    private static final String[] FIRST_SEMESTER_MONTH_INT = {"9","11", "12"};
    private static final String[] SECOND_SEMESTER_MONTH_NAMES = { "იანვარი-თებერვალი", "მარტი", "აპრილი", "მაისი", "ივნისი"};
    private static final String[] SECOND_SEMESTER_MONTH_INT = {"1", "3", "4", "5", "6"};

    @Override
    public void exportSemesterGrades(Map<Student, Map<Subject, Map<Integer, BigDecimal>>> grades, boolean semester) {
        try (XWPFDocument document = new XWPFDocument()) {

            CTBody body = document.getDocument().getBody();
            CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
            CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();

            pageSize.setOrient(STPageOrientation.LANDSCAPE);
            pageSize.setW(BigInteger.valueOf(842 * 20));
            pageSize.setH(BigInteger.valueOf(595 * 20));

            List<Student> students = new ArrayList<>(grades.keySet());
            List<Subject> subjects = new ArrayList<>(grades.get(students.get(0)).keySet());
            int numberOfMonths = semester ? 3 : 5;
            int numberOfColumns = 1 + (subjects.size() *  numberOfMonths);
            BigDecimal numOfPages = BigDecimal.valueOf(numberOfColumns).divide(BigDecimal.valueOf(NUMBER_OF_SUBJECTS_PER_PAGE * numberOfMonths), RoundingMode.CEILING);
            Map<String, Subject> subjectWithNames = subjects.stream().collect(Collectors.toMap(Subject::getName, Function.identity()));
            List<String> subjectNames =  new ArrayList<>(subjectWithNames.keySet());
            subjectNames.add(0, "მოსწავლის სახელი და გვარი");
            String[] arraySubjectNames = subjectNames.toArray(new String[0]);
            String[] months = semester ? FIRST_SEMESTER_MONTH_NAMES : SECOND_SEMESTER_MONTH_NAMES;
            String[] monthsNum = semester ? FIRST_SEMESTER_MONTH_INT : SECOND_SEMESTER_MONTH_INT;

            for (int i = 0; i < numOfPages.longValue(); i++){
                 String[] headers = getColumnsForPage(arraySubjectNames, i);
                 String[][] studentData = new String[students.size()+1][(headers.length-1) * monthsNum.length + 1];
                 for (int l = 0; l < (headers.length-1) * monthsNum.length + 1 ; l++) {
                     studentData[0][l] = l == 0 ? "/" : (semester ? FIRST_SEMESTER_MONTH_NAMES[(l-1) % 3] : SECOND_SEMESTER_MONTH_NAMES[(l-1) % 5]);
                 }
                 for (int j = 1; j <= students.size(); j ++) {
                     String studentName = students.get(j - 1 ).getFirstName() + " " + students.get(j - 1).getLastName();
                     String[] gradeArr = new String[(headers.length-1) * monthsNum.length + 1]; // todo washale monthsNum -ის პპირველი წევრი
                     gradeArr[0] = studentName;
                     for (int k = 1; k < headers.length; k ++) {
                         Subject subject = subjectWithNames.get(headers[k]);
                         if (subject == null) {
                             continue;
                         }
                         for (int m = 1; m <= monthsNum.length; m ++) {
                             gradeArr[(monthsNum.length * (k - 1)) + m] =  grades.get(students.get(j - 1)).get(subject).get(Integer.valueOf(monthsNum[m-1])).toString();
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
        XWPFTable table = document.createTable(studentData.length + 1 , studentData[0].length);

        // Set column widths for the headers and cells
        table.setWidthType(TableWidthType.PCT);

        int k = 1;
        for (int i = 0; i < headers.length; i++) {
            if (i == 0) {
                table.getRow(0).getCell(i).setWidth("20%");
                table.getRow(0).getCell(i).setText(headers[i]);
            } else {
                table.getRow(0).getCell(k).setWidth("15%");
                table.getRow(0).getCell(k).setText(headers[i]);
                table.getRow(0).getCell(k).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
                int a = 0;
                for(int j = k + 1; j < k + months.length; j ++){
                    table.getRow(0).getCell(j).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                    a = j;
                }
                k = a + 1;
            }
        }

//        XWPFTableRow row = table.createRow();
//        for (int i = 0; i < months.length; i++) {
//            monthRow.getCell(i).setText(months[i]);
//        }
//
//        for (String[] student : studentData) {
//            XWPFTableRow studentRow = table.createRow();
//            for (int i = 0; i < student.length; i++) {
//                if(i == 1)
//                    continue;
//                studentRow.getCell(i).setText(student[i]);
//            }
//        }
//
        for (int i = 1; i <= studentData.length; i++) {
            for (int j = 0; j < studentData[i-1].length; j++) {
                table.getRow(i).getCell(j).setText(studentData[i - 1][j]);
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
