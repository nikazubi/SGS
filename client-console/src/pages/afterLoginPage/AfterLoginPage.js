import Box from "./Box";
const AfterLoginPage = () => {
    return ( 
        <div className="boxCnt">
            <div className="boxWrap">
                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შეფასება აკადემიური საგნობრივი დისციპლინის მიხედვით'} link={'/shefaseba-akademiuri-sagnobrivi-disciplinis-mixedvit'} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შეფასება თვის რეიტინგების მიხედვით'} link={'/tvis-reitingi'} />
                </div>

                <div className="boxWrap__div">
                    <Box text={'მოსწავლის შეფასება ეთიკური ნომრების მიხედვით'} link={'/ethicalPage'} />
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
     );
}
 
export default AfterLoginPage;