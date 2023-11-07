package mthiebi.sgs.utils;

import mthiebi.sgs.dto.GradeComponentWrapper;
import mthiebi.sgs.dto.SubjectComponentWrapper;
import mthiebi.sgs.models.Grade;

import java.util.*;

public class ExcelUtils {
    
    public static final List<String> subjectPattern = Arrays.asList(
            "ქართული ენა",
            "ქართული ლიტერატურა",
            "ქართული სტილისტიკა",
            "ქართული გრამატიკა",
            "ავტორის საათი",
            "ავტორის საათი / წერის საათი",
            "მათემატიკა",
            "ალგებრა / გეომეტრია",
            "ალგორითმიკა",
            "პრე-კალკულუსი",
            "კალკულუსი",
            "ბიზნესი / ეკონომიკა",
            "ბლოკური პროგრამირება",
            "ინგლისური ენა",
            "ინგლისური ლიტერატურა",
            "ინგლისური გრამატიკა და ლექსიკა",
            "ბიზნეს ინგლისური",
            "საკომუნიკაციო ინგლისური",
            "ინგლისური აკადემიური წერა",
            "გერმანული ენა",
            "რუსული ენა",
            "ფიზიკა",
            "ქიმია",
            "ბიოლოგია",
            "ისტორია",
            "ისტორია / ჩვენი საქართველო",
            "გეოგრაფია",
            "სამოქალაქო განათლება",
            "ინფორმაციული და საკომუნიკაციო ტექნოლოგიები",
            "რობოტიკა / ინჟინერია",
            "არტ ხელოვნება",
            "არტ სახელოსნო",
            "სამართალის შესავალი",
            "სასცენო ხელოვნება",
            "მუსიკა",
            "სპორტი",
            "rating",
            "behaviour",
            "absence"
    );

    public static void sortGradeArrayForExcel(List<GradeComponentWrapper> list) {
        Comparator<SubjectComponentWrapper> gradeComparator = new Comparator<SubjectComponentWrapper>() {
            @Override
            public int compare(SubjectComponentWrapper a, SubjectComponentWrapper b) {
                int indexA = subjectPattern.indexOf(a.getSubject().getName());
                int indexB = subjectPattern.indexOf(b.getSubject().getName());

                // If either subject name is not in subjectPattern, place it at the end
                if (indexA == -1 && indexB == -1) {
                    return 0; // Keep their relative order
                }
                if (indexA == -1) {
                    return 1; // Place a at the end
                }
                if (indexB == -1) {
                    return -1; // Place b at the end
                }

                // Compare based on their positions in subjectPattern
                return Integer.compare(indexA, indexB);
            }
        };

        for (GradeComponentWrapper gradeComponentWrapper : list) {
            gradeComponentWrapper.getGradeList().sort(gradeComparator);
        }
    }
    
}
