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
                    <Box text={'მოსწავლის ტრიმესტრული შეფასება აკადემიური დისციპლინების მიხედვით'}
                         link={`/grades/${subjectData ? subjectData[0].name : ''}`}/>
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შემაჯამებელი ტრიმესტრული შეფასება'} link={'/trimester'}/>
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შეფასება ეთიკური ნორმების მიხედვით'} link={'/ethicalPage'} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის ტრიმესტრული და წლიური შეფასება'} link={'/annual'}/>
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