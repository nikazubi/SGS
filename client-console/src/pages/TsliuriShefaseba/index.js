import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import React, {useCallback, useMemo, useState} from "react";
import CustomShefasebaBar from "../../components/CustomShefasebaBar";
import useFetchYear from "../semestruli-shefaseba/useYear";
import useGradeAnual from "./useGradeAnual";
import DataGridSGS from "../../components/datagrid/DataGrid";
import DataGridPaper from "../../components/datagrid/DataGridPaper";

const TsliuriShefaseba = () => {


    const {data: yearData, isLoading: isYearLoading} = useFetchYear();
    const [chosenYear, setChosenYear] = useState();

    const [selectedData, setSelectedData] = useState('2022-2023');
    const [currentData, setCurrentData] = useState([]);

    const {data, isLoading, isError, error, isSuccess} = useGradeAnual({yearRange: yearData});

    const parsedData = useMemo(() => {
        const result = [];
        if (!data) {
            return result
        }
        return data[0]?.gradeList?.map((grade) => ({
            name: grade.subject.name || "",
            value: grade.value["4"] ? grade.value["4"] < 0 ? 0 : grade.value["4"] : 0
        }))
    }, [data])


    const handleChange = (event) => {
        setChosenYear(event.target.value);
    };

    const getYearDropdown = useCallback(
        () => {
            if (!yearData) {
                return <div></div>
            }

            return (
                <div className="yearDropwdown">
                    <TextField
                        select
                        label="აირჩიეთ სასწავლო წელი"
                        value={chosenYear || yearData[yearData.length - 1]}
                        onChange={handleChange}
                        variant="outlined"
                    >
                        {yearData.map((m) => (
                            <MenuItem key={m} value={m}>
                                {m}
                            </MenuItem>
                        ))}
                    </TextField>
                </div>
            );
        },
        [yearData, chosenYear, handleChange],
    );


    const handleSearch = async () => {
        if (!!selectedData) {
            const studentYearlyGrades = [
                {
                    name: 'ქართული ენა და ლიტერატურა',
                    ქულა: 7,
                },

                {
                    name: 'მათემატიკა',
                    ქულა: 6,
                },

                {
                    name: 'ინგლისური',
                    ქულა: 7,
                },
                {
                    name: 'ისტორია',
                    ქულა: 6,
                },
                {
                    name: 'გეოგრაფია',
                    ქულა: 7,
                }]

            setCurrentData(studentYearlyGrades)
        }
    }

    let gradeClomuns3 = []

    const getFieldName = (o, num) => {
        return o.subject.id + "-" + num;
    }

    function customRound(value) {
        const res = value % 1 >= 0.5 ? Math.ceil(value) : Math.floor(value);
        return res === 0 ? "" : res
    }

    const gradeColumns = [
        {
            headerName: "საგანი",
            renderCell: ({row}) => {
                return (<div>
                    {row.subject.name}
                </div>);
            },
            renderHeader: (params) => (
                <div>
                    {'საგანი'}
                </div>
            ),
            field: 'name',
            align: 'center',
            headerAlign: 'center',
        },
        {
            headerName: "სემესტრი I",
            renderCell: ({row}) => {
                return (<div>
                    {row.value["1"] === 0 ? "" : row.value["1"]}
                </div>);
            },
            renderHeader: (params) => (
                <div>
                    {"სემესტრი I"}
                </div>
            ),
            field: 'semester1',
            // sortable: false,
            align: 'center',
            headerAlign: 'center',
        },
        {
            headerName: "სემესტრი II",
            renderCell: ({row}) => {
                return (<div>
                    {row.value["2"] === 0 ? "" : row.value["2"]}
                </div>);
            },
            renderHeader: (params) => (
                <div>
                    {"სემესტრი II"}
                </div>
            ),
            field: 'semester2',
            // sortable: false,
            align: 'center',
            headerAlign: 'center',
        },
        {
            headerName: "წლიური ქულა",
            renderCell: ({row}) => {
                return (<div>
                    {customRound((Number(row.value["1"]) + Number(row.value["2"])) / 2)}
                </div>);
            },
            renderHeader: (params) => (
                <div>
                    {"წლიური ქულა"}
                </div>
            ),
            field: 'yearly',
            // sortable: false,
            align: 'center',
            headerAlign: 'center',
        },
        {
            headerName: "გამოცდა",
            renderCell: ({row}) => {
                return (<div>
                    {row.value["3"] === 0 ? "" : row.value["3"]}
                </div>);
            },
            renderHeader: (params) => (
                <div>
                    {"გამოცდა"}
                </div>
            ),
            field: 'exam',
            // sortable: false,
            align: 'center',
            headerAlign: 'center',
        },
        {
            headerName: "საბოლოო ქულა",
            renderCell: ({row}) => {
                return (<div>
                    {row.value["4"] === 0 ? "" : row.value["4"]}
                </div>);
            },
            renderHeader: (params) => (
                <div>
                    {"საბოლოო ქულა"}
                </div>
            ),
            field: 'final',
            // sortable: false,
            align: 'center',
            headerAlign: 'center',
        },
    ];

    //
    // const gradeColumns = useMemo(() => {
    //     return
    // }, [])


    return (
        <>
            <div className="ib__center column">
                <div className="pageName">მოსწავლის წლიური შეფასება</div>
                <div style={{display: 'flex', alignItems: 'center', marginTop: '25px'}}>
                    {getYearDropdown()}
                    <div style={{marginLeft: '10px'}}>
                        <Button onClick={handleSearch} disabled={!selectedData}
                                style={{fontWeight: 'bold', height: '50px'}}
                                variant="contained">ძიება<SearchIcon/></Button>
                    </div>
                </div>
            </div>

            <div>
                {/*<SemesterGradeToolbar filters={filters} setFilters={setFilters} checked={checked} setChecked={setChecked}/>*/}
                <div style={{height: `45vh`, width: '100%', marginTop: 30, paddingLeft: 40, paddingRight: 40}}>
                    <DataGridPaper>
                        <DataGridSGS
                            queryKey={"ANNUAL_GRADE"}
                            // experimentalFeatures={{columnGrouping: true}}
                            // columnGroupingModel={getGradeColumns()}
                            columns={gradeColumns}
                            rows={data ? data[0]?.gradeList : []}
                            getRowId={(row) => {
                                return row.subject.id;
                            }}
                            getRowHeight={() => 50}
                            disableColumnMenu
                            // filters={filters}
                        />
                    </DataGridPaper>
                </div>
            </div>

            {!!parsedData && parsedData.length > 0 && <div style={{marginTop: 30}}>
                <CustomShefasebaBar color={'#45c1a4'} data={parsedData} height={450} width={window.innerWidth - 30}
                                    left={0}/>
            </div>}

        </>
    );
}

export default TsliuriShefaseba;