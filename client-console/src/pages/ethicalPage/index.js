import { useEffect } from "react";
import { useUpdate } from "../../context/userDataContext";
import DisciplineBox from "./EthicalBox";

const EthicPage = () => {
    const updateData = useUpdate()


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

          return () =>{
            updateData([])
          }

      },[])

    return ( 
        <>
        <div className="pageName">მოსწავლის შეფასება ეთიკური ნომრების მიხედვით</div>
        <div className="ibCnt">
            <div className="termEstCnt">
                <DisciplineBox />
            </div>
        </div>
        </>
     );
}
 
export default EthicPage;