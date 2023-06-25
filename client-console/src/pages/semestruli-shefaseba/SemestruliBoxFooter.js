
// testID

import SmallBox from "./SmallBox";

const SemestruliBoxFooter = ({testID}) => {

    const renderGrades = () => {

        if (testID === 1){
            return(
                <div className="grades__footerCnt">
                    <SmallBox boxLabel={1} testID={testID}/>
                    <SmallBox boxLabel={2} testID={testID}/>
                    <SmallBox boxLabel={'აღდ'} testID={testID}/>
                    <SmallBox boxLabel={'თვე'} testID={testID}/>
                    <SmallBox boxLabel={'%'} testID={testID}/>
                </div>
            )
        }

        else if (testID === 2){
            return(
                <div className="grades__footerCnt">
                    <SmallBox boxLabel={1} testID={testID}/>
                    <SmallBox boxLabel={2} testID={testID}/>
                    <SmallBox boxLabel={3} testID={testID}/>
                    <SmallBox boxLabel={'თვე'} testID={testID}/>
                    <SmallBox boxLabel={'%'} testID={testID}/>
                    <div className="grades__footerCnt_poa1">წერითი დავალება</div>
                    <div className="grades__footerCnt_poa2">შემოქმედებითი დავალება</div>
                </div>
            )
        }

        else if (testID === 3){
            return(
                <div className="grades__footerCnt">
                    <SmallBox boxLabel={1} testID={testID}/>
                    <SmallBox boxLabel={2} testID={testID}/>
                    <SmallBox boxLabel={'თვე'} testID={testID}/>
                    <SmallBox boxLabel={'%'} testID={testID}/>
                </div>
            )
        }

    }

    return ( 
        renderGrades()
     );
}
 
export default SemestruliBoxFooter;