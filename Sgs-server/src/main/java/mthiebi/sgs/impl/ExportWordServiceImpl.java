package mthiebi.sgs.impl;

import mthiebi.sgs.models.Student;
import mthiebi.sgs.models.Subject;
import mthiebi.sgs.service.ExportWordService;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextVerticalType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
    private static final String[] FIRST_SEMESTER_MONTH_NAMES = { "სექტემბერი-ოქტომბერი", "ნოემბერი", "დეკემბერი", "სემესტრული", "შემოქმედობითობა"};
    private static final String[] FIRST_SEMESTER_MONTH_INT = {"9","11", "12", "-1", "-2"};
    private static final String[] SECOND_SEMESTER_MONTH_NAMES = { "იანვარი-თებერვალი", "მარტი", "აპრილი", "მაისი", "ივნისი", "სემესტრული", "შემოქმედობითობა"};
    private static final String[] SECOND_SEMESTER_MONTH_INT = {"1", "3", "4", "5", "6", "-1", "-2"};

    @Override
    public byte[] exportSemesterGrades(Map<Student, Map<Subject, Map<Integer, BigDecimal>>> grades, boolean semester, boolean isDecimal, String semesterYears) {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("სკოლა პანსიონ იბ მთიები - " + semesterYears + " - " + (semester ? "პირველი " : "მეორე ") + "სემესტრი");
            titleRun.setColor("000000");
            titleRun.setBold(true);
            titleRun.setFontFamily("Courier");
            titleRun.setFontSize(14);

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
            BigDecimal numOfPages = BigDecimal.ZERO.equals(BigDecimal.valueOf(numberOfColumns))? BigDecimal.ZERO : BigDecimal.valueOf(numberOfColumns).divide(BigDecimal.valueOf(NUMBER_OF_SUBJECTS_PER_PAGE * numberOfMonths), RoundingMode.CEILING);
            Map<String, Subject> subjectWithNames = subjects.stream().collect(Collectors.toMap(Subject::getName, Function.identity()));
            List<String> subjectNames =  new ArrayList<>(subjectWithNames.keySet());
            subjectNames.add(0, "მოსწავლის სახელი და გვარი");
            String[] arraySubjectNames = subjectNames.toArray(new String[0]);
            String[] months = semester ? FIRST_SEMESTER_MONTH_NAMES : SECOND_SEMESTER_MONTH_NAMES;
            String[] monthsNum = semester ? FIRST_SEMESTER_MONTH_INT : SECOND_SEMESTER_MONTH_INT;

            for (int i = 0; i < numOfPages.longValue(); i++){
                 String[] headers = getColumnsForPage(arraySubjectNames, i);
                 if (i != 0) {
                     String[] newarr = new String[headers.length + 1];
                     newarr[0] = "მოსწავლის სახელი და გვარი";
                     for (int q = 1; q <= headers.length; q++) {
                         newarr[q] = headers[q-1];
                     }
                     headers = newarr;
                 }
                 String[][] studentData = new String[students.size()+1][(headers.length-1) * monthsNum.length + 1];
                 for (int l = 0; l < (headers.length-1) * monthsNum.length + 1 ; l++) {
                     studentData[0][l] = l == 0 ? " " : (semester ? FIRST_SEMESTER_MONTH_NAMES[(l-1) % 5] : SECOND_SEMESTER_MONTH_NAMES[(l-1) % 7]);
                 }
                 for (int j = 1; j <= students.size(); j ++) {
                     String studentName = students.get(j - 1 ).getFirstName() + " " + students.get(j - 1).getLastName();
                     String[] gradeArr = new String[(headers.length-1) * monthsNum.length + 1];
                     gradeArr[0] = studentName;
                     for (int k = 1; k < headers.length; k ++) {
                         Subject subject = subjectWithNames.get(headers[k]);
                         if (subject == null) {
                             continue;
                         }
                         for (int m = 1; m <= monthsNum.length; m ++) {
                             gradeArr[(monthsNum.length * (k - 1)) + m] = isDecimal ?
                                     String.valueOf(grades.get(students.get(j - 1)).get(subject).get(Integer.valueOf(monthsNum[m-1])).add(new BigDecimal(3)).longValue()) :
                                     String.valueOf(grades.get(students.get(j - 1)).get(subject).get(Integer.valueOf(monthsNum[m-1])).longValue());
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
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            out.close();
            document.close();

            return out.toByteArray();

            // Save the document
//            try (FileOutputStream out = new FileOutputStream("WTF.docx")) {
//                document.write(out);
//                System.out.println("Word document generated successfully.");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    XWPFTable createTable(XWPFDocument document, String[] headers, String[] months, String[][] studentData){
        XWPFTable table = document.createTable(studentData.length + 1 , studentData[0].length);

        // Set column widths for the headers and cells
        table.setWidthType(TableWidthType.AUTO);

        int k = 1;
        for (int i = 0; i < headers.length; i++) {
            if (i == 0) {
//                table.getRow(0).getCell(i).setWidth("20%");
                XWPFRun run = table.getRow(0).getCell(i).addParagraph().createRun();
                run.setText(headers[i]);
                run.setFontSize(10);
                table.getRow(0).getCell(i).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            } else {
//                table.getRow(0).getCell(k).setWidth("15%");
                XWPFRun run = table.getRow(0).getCell(k).addParagraph().createRun();
                run.setText(headers[i]);
                run.setFontSize(10);
                table.getRow(0).getCell(k).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
                table.getRow(0).getCell(k).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                int a = 0;
                for(int j = k + 1; j < k + months.length; j ++){
                    table.getRow(0).getCell(j).getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
                    table.getRow(0).getCell(j).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                    a = j;
                }
                k = a + 1;
            }
        }

        for (int i = 1; i <= studentData.length; i++) {
            for (int j = 0; j < studentData[i-1].length; j++) {
                if (i == 1) {
                    XWPFTableCell newCell = table.getRow(i).getCell(j);
                    XWPFParagraph paragraph = newCell.addParagraph();
                    XWPFRun run = paragraph.createRun();
                    paragraph.setAlignment(ParagraphAlignment.CENTER); // Set alignment
                    run.setText(studentData[i - 1][j].equals("0") ? " " : studentData[i - 1][j]);
                    run.setFontSize(10);

//                    CTPPr ppr = paragraph.getCTP().getPPr();
//                    if (ppr == null) ppr = paragraph.getCTP().addNewPPr();
//                    CTTextDirection textDirection = ppr.isSetTextDirection() ? ppr.getTextDirection() : ppr.addNewTextDirection();
//                    textDirection.setVal(STTextDirection.Enum.forInt(4));
//                    table.getRow(i).getCell(j).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTH);
                } else {
                    XWPFTableCell newCell = table.getRow(i).getCell(j);
                    XWPFParagraph paragraph = newCell.addParagraph();
                    XWPFRun run = paragraph.createRun();
                    paragraph.setAlignment(ParagraphAlignment.CENTER); // Set alignment
                    run.setText(studentData[i - 1][j].equals("0") ? " " : studentData[i - 1][j]);
                    run.setFontSize(10);
//                    table.getRow(i).getCell(j).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                }
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
