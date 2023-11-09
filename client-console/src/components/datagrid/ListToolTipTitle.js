import React from "react";

const ListToolTipTitle = (values) => {

    return (
        <div>
            <ul style={{paddingLeft: 15}}>
                {values.map((values) => {
                    return <li key={values}>{values}</li>;
                })}
            </ul>
        </div>
    );
}

export default ListToolTipTitle;