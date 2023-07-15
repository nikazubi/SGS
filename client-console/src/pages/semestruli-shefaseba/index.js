import ShefasebaTable from "../../components/ShefasebaTable";

const SemestruliShefaseba = () => {

    const data = [
        {
            subject: 'მუსიკა',
            grade: 7
        },

        {
            subject: 'მათემატიკა',
            grade: 7
        },

        {
            subject: 'ქართული',
            grade: 7
        },


]

    return ( 
        <ShefasebaTable data={data}/>
     );
}
 
export default SemestruliShefaseba;