import {useEffect, useState} from "react";
import { useUpdate } from "../../context/userDataContext";
import DisciplineBox from "./EthicalBox";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import Button from "@mui/material/Button";
import SearchIcon from "@mui/icons-material/Search";

const EthicPage = () => {
    const updateData = useUpdate()

    const [selectedData, setSelectedData] = useState('სექტემბერი');
    const month = [
        'სექტემბერი',
        'ოქტომბერი',
        'ნოემბერი',
        'დეკემბერი',
        'იანვარი',
        'თებერვალი',
        'მარტი',
        'აპრილი',
        'მაისი',
        'ივნისი'
    ];
    const handleChange = (event) => {
        setSelectedData(event.target.value);
    };

    useEffect(()=>{
        const allStudentData = [
            {
                name:'მოსწავლის ფორმითი გამოცხადებ',
                testNumber: null,
                precent: null,
                boxdetails: [
                  { label: 'I კვირა',
                    grade: 0,
                  },
          
                  { label: 'II კვირა',
                    grade: 0,
                  },
          
                  { label: 'III კვირა',
                    grade: 0,
                  },
          
                  { label: 'IV კვირა',
                    grade: 0,
                  },
          
                ]
            },
          
            {
                name:'მოსწავლის დაგვიანება',
                testNumber: null,
                precent: null,
                boxdetails: [
                  { label: 'I კვირა',
                    grade: 0,
                  },
          
                  { label: 'II კვირა',
                    grade: 0,
                  },
          
                  { label: 'III კვირა',
                    grade: 0,
                  },
          
                  { label: 'IV კვირა',
                    grade: 0,
                  },
          
                ]
            },
          
            {
                name:'საკლასო ინვენტარის მოვლა',
                testNumber: null,
                precent: null,
                boxdetails: [
                  { label: 'I კვირა',
                    grade: 0,
                  },
          
                  { label: 'II კვირა',
                    grade: 0,
                  },
          
                  { label: 'III კვირა',
                    grade: 0,
                  },
          
                  { label: 'IV კვირა',
                    grade: 0,
                  },
          
                ]
            },

            {
                name:'მოსწავლის მიერ ჰიგიენური ნორმების დაცვა',
                testNumber: null,
                precent: null,
                boxdetails: [
                  { label: 'I კვირა',
                    grade: 0,
                  },
          
                  { label: 'II კვირა',
                    grade: 0,
                  },
          
                  { label: 'III კვირა',
                    grade: 0,
                  },
          
                  { label: 'IV კვირა',
                    grade: 0,
                  },
          
                ]
            },

            {
                name:'მოსწავლის ყოფაქცევა',
                testNumber: null,
                precent: null,
                boxdetails: [
                  { label: 'I კვირა',
                    grade: 0,
                  },
          
                  { label: 'II კვირა',
                    grade: 0,
                  },
          
                  { label: 'III კვირა',
                    grade: 0,
                  },
          
                  { label: 'IV კვირა',
                    grade: 0,
                  },
          
                ]
            }
          
          ]

        updateData(allStudentData)

        return () => {
          updateData([])
        }

      },[])

    function dropdown(){
        return (
            <div className="yearDropwdown">
                <TextField
                    select
                    label="აირჩიე თვე"
                    value={selectedData}
                    onChange={handleChange}
                    variant="outlined"
                >
                    {month.map((m) => (
                        <MenuItem key={m} value={m}>
                            {m}
                        </MenuItem>
                    ))}
                </TextField>&nbsp;&nbsp;&nbsp;
                <Button onClick={()=>{}} disabled={!selectedData} style={{ fontWeight: 'bold', height: '50px'}} variant="contained">ძიება<SearchIcon/></Button>
            </div>
        );
    }

    return ( 
        <>
        <div className="pageName">მოსწავლის შეფასება ეთიკური ნორმების მიხედვით</div>
        <div className="ibCnt">
            {dropdown()}
            <div className="termEstCnt">
                <DisciplineBox />
            </div>
        </div>
        </>
     );
}
 
export default EthicPage;