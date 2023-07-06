import Box from "./Box";
const AfterLoginPage = () => {
    return ( 
        <div className="boxCnt">
            <div className="boxWrap">
                <Box text={'მოსწავლის შეფასება აკადემიური საგნობრივი დისციპლინის მიხედვით'} link={'/dat'} color={'#106cb7'}/>
                <Box text={'მოსწავლის შეფასება თვის რეიტინგების მიხედვით'} link={'/dat'} color={'#7ccfd7'}/>
                <Box text={'მოსწავლის შეფასება ეთიკური ნომრების მიხედვით'} link={'/ethicalPage'} color={'#f6a21e'}/>
                <Box text={'მოსწავლის სემესტრული შეფასება'} link={'/semestruli-shefaseba'} color={'#f25d23'}/>
                <Box text={'მოსწავლის წლიური შეფასება'} link={'/dat'} color={'#1c4678'}/>
                <Box text={'მოსწავლის მიერ გაცდენილი საათები'} link={'/dat'} color={'#16987c'}/>
            </div>   
        </div>
     );
}
 
export default AfterLoginPage;