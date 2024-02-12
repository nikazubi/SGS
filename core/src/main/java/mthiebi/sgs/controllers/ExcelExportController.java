package mthiebi.sgs.controllers;

import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.GradeComponentWrapper;
import mthiebi.sgs.dto.SubjectComponentWrapper;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.service.AcademyClassService;
import mthiebi.sgs.utils.AuthConstants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static mthiebi.sgs.utils.ExcelUtils.sortGradeArrayForExcel;
import static mthiebi.sgs.utils.ExcelUtils.subjectPattern;

@RestController
@RequestMapping("/test")
public class ExcelExportController {

    @Autowired
    private GradeController gradeController;

    @Autowired
    private AcademyClassService academyClassService;

    @GetMapping("/exportToExcel")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcel(@RequestParam Long classId,
                              @RequestParam String createDate,
                              @RequestParam String component,
                              @RequestParam boolean isDecimalSystem,
                              HttpServletResponse response) throws IOException, SGSException {
        // Create a new workbook and sheet
        List<GradeComponentWrapper> list = gradeController.getGradesByComponent(classId, null, null,  createDate, component);
        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(academyClass.getClassName());

        // Create a header row
        Row mainRow = sheet.createRow(0);
        Cell headerCell = mainRow.createCell(0);
        headerCell.setCellValue("სკოლა პანსიონ იბ მთიები - კლასი " + academyClass.getClassName() + " - " + adjustMonth(date.getMonth() + 1));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));


        Row headerRow = sheet.createRow(1);
        GradeComponentWrapper first = list.get(0);
        List<SubjectComponentWrapper> subjectComponentWrappersList = first.getGradeList();
        Cell firstCell = headerRow.createCell(0);
        firstCell.setCellValue("მოსწავლის გვარი, სახელი");
        for (int i = 0; i < subjectComponentWrappersList.size() ; i++) {
            Cell dataCell = headerRow.createCell(i + 1);
            dataCell.setCellValue(adjustName(subjectComponentWrappersList.get(i).getSubject().getName()));
        }
        for (int i = 0; i < list.size(); i++) {
            Row studentRow = sheet.createRow(i + 2);
            for (int j = 0; j < list.get(i).getGradeList().size() + 1; j++) {
                if (j == 0) {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue((i+1) + ". " +  list.get(i).getStudent().getLastName() + " " + list.get(i).getStudent().getFirstName());
                } else {
                    Cell dataCell = studentRow.createCell(j);
                    dataCell.setCellValue(adjustGradeValue(list.get(i).getGradeList().get(j-1), isDecimalSystem));
                }
            }
        }

        int teacherIndex = list.size() + 2;
        Row teacherRow = sheet.createRow(teacherIndex);
        Cell teacgerCell = teacherRow.createCell(0);
        teacgerCell.setCellValue("პედაგოგი");

        for (int i = 0; i < subjectComponentWrappersList.size() ; i++) {
            Cell dataCell = teacherRow.createCell(i + 1);
            dataCell.setCellValue(subjectComponentWrappersList.get(i).getSubject().getTeacher());
        }

        for (int i = 0; i <= sheet.getRow(1).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
        }

        CellStyle thickBorderStyle = workbook.createCellStyle();
        thickBorderStyle.setBorderTop(BorderStyle.THIN);
        thickBorderStyle.setBorderBottom(BorderStyle.THIN);
        thickBorderStyle.setBorderLeft(BorderStyle.THIN);
        thickBorderStyle.setBorderRight(BorderStyle.THIN);
        thickBorderStyle.setAlignment(HorizontalAlignment.CENTER);

        // Apply the thick border style to all cells in the sheet
        for (Row row : sheet) {
            for (Cell cell : row) {
                cell.setCellStyle(thickBorderStyle);
            }
        }

        // Set the response headers for Excel download
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exported_data.xlsx");

        // Write the workbook to the response stream
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private String adjustGradeValue(SubjectComponentWrapper obj, Boolean isDecimalSystem) {
        Object value = obj.getValue();
        if (value == null || Objects.equals(value.toString(), "0")) {
            return " ";
        }
        if (value.toString().startsWith("-50")) {
            return "ჩთ";
        }
        BigDecimal val = new BigDecimal(value.toString());
        String subjName = obj.getSubject().getName();
        return isDecimalSystem
                && !subjName.equalsIgnoreCase("rating")
                && !subjName.equalsIgnoreCase("behaviour")
                && !subjName.equalsIgnoreCase("absence") ? String.valueOf(val.add(new BigDecimal(3)).longValue()) : String.valueOf(val.longValue());
    }

    private String adjustName(String name) {
        switch (name) {
            case "rating":
                return "რეიტინგი";
            case "behaviour":
                return "ეთიკური ნორმა";
            case "absence":
                return "გაცდენილი საათები";
            default:
                return name;
        }
    }

    private String adjustMonth(int i) {
        switch (i) {
            case 1:
            case 2:
                return "იანვარი-თებერვალი";
            case 3:
                return "მარტი";
            case 4:
                return "აპრილი";
            case 5:
                return "მაისი";
            case 6:
                return "ივნისი";
            case 9:
            case 10:
                return "სექტემბერი-ოქტომბერი";
            case 11:
                return "ნოემბერი";
            case 12:
                return "დეკემბერი";
            default:
                return "თვე";
        }
    }
}