package mthiebi.sgs.controllers;

import lombok.RequiredArgsConstructor;
import mthiebi.sgs.SGSException;
import mthiebi.sgs.dto.GradeComponentWrapper;
import mthiebi.sgs.dto.GradeGroupByClause;
import mthiebi.sgs.dto.GradeWrapperDto;
import mthiebi.sgs.models.AcademyClass;
import mthiebi.sgs.service.AcademyClassService;
import mthiebi.sgs.service.ExcelExportService;
import mthiebi.sgs.utils.AuthConstants;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static mthiebi.sgs.utils.ExcelUtils.sortGradeArrayForExcel;

/**
 * Thin HTTP layer for the grade exports. Fetches the grade data and streams the workbook back to the client;
 * the actual spreadsheet construction lives in {@link ExcelExportService}.
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ExcelExportController {

    private final GradeController gradeController;

    private final AcademyClassService academyClassService;

    private final ExcelExportService excelExportService;

    @GetMapping("/exportToExcel")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcel(@RequestParam Long classId,
                              @RequestParam String createDate,
                              @RequestParam String component,
                              @RequestParam boolean isDecimalSystem,
                              HttpServletResponse response) throws IOException, SGSException {
        List<GradeComponentWrapper> list = gradeController.getGradesByComponent(classId, null, null, createDate, component);
        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);
        Date date = new Date();
        if (createDate != null) {
            date.setTime(Long.parseLong(createDate));
        }

        Workbook workbook = excelExportService.buildMonthlyExcel(list, academyClass, date, isDecimalSystem);
        writeWorkbook(workbook, response);
    }

    @GetMapping("/exportToExcel/semester")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcelSemester(@RequestParam Long classId,
                                      @RequestParam String yearRange,
                                      @RequestParam String component,
                                      @RequestParam boolean isDecimalSystem,
                                      HttpServletResponse response) throws IOException, SGSException {
        List<GradeComponentWrapper> list = gradeController.getGradesByComponent(classId, null, yearRange, null, component);
        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);

        Workbook workbook = excelExportService.buildSemesterExcel(list, academyClass, yearRange, component, isDecimalSystem);
        writeWorkbook(workbook, response);
    }

    @GetMapping("/exportToExcel/anual")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcelAnual(@RequestParam Long classId,
                                   @RequestParam String yearRange,
                                   @RequestParam String component,
                                   @RequestParam boolean isDecimalSystem,
                                   HttpServletResponse response) throws IOException, SGSException {
        List<GradeComponentWrapper> list = gradeController.getGradesByComponent(classId, null, yearRange, null, component);
        sortGradeArrayForExcel(list);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);

        Workbook workbook = excelExportService.buildAnnualExcel(list, academyClass, yearRange, isDecimalSystem);
        writeWorkbook(workbook, response);
    }

    @GetMapping("/exportToExcel/dashbord")
    @Secured({AuthConstants.MANAGE_GRADES})
    public void exportToExcelDashboard(@RequestParam Long classId,
                                       @RequestParam Long subjectId,
                                       @RequestParam String filterDate,
                                       @RequestParam String gradeTypePrefix,
                                       HttpServletResponse response) throws IOException, SGSException {
        List<GradeWrapperDto> list = gradeController.getGradeGrouped(classId, subjectId, null, filterDate, GradeGroupByClause.STUDENT, gradeTypePrefix);
        AcademyClass academyClass = academyClassService.findAcademyClassById(classId);
        Date date = new Date();
        if (filterDate != null) {
            date.setTime(Long.parseLong(filterDate));
        }

        Workbook workbook = excelExportService.buildDashboardExcel(list, academyClass, date, gradeTypePrefix);
        writeWorkbook(workbook, response);
    }

    private void writeWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=exported_data.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
