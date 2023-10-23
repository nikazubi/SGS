import Box from "./Box";
import useSubjects from "../Discipline/useSubjects";
const AfterLoginPage = () => {

    const {data: subjectData, isLoading, isError, error, isSuccess} = useSubjects();

    if(isLoading) {
        return <></>
    }

    return ( 
        <>
        <div className="boxCnt">
            <div className="boxWrap">
                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შეფასება აკადემიური საგნობრივი დისციპლინის მიხედვით'} link={`/grades/${subjectData? subjectData[0].name: ''}`} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შეფასება თვის რეიტინგების მიხედვით'} link={'/tvis-reitingi'} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შეფასება ეთიკური ნორმების მიხედვით'} link={'/ethicalPage'} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის სემესტრული შეფასება'} link={'/semestruli-shefaseba'} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის წლიური შეფასება'} link={'/tsliuri-shefaseba'} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის მიერ გაცდენილი საათები'} link={'/absence-page'} />
                </div>
            </div>   
        </div>
        <div class="body__wallpaper"></div>
        </>
     );
}
 
export default AfterLoginPage;