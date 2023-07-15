import * as React from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';

function createData( date, uniform, absence, inventory, hygiene, behaviour ) {
  return { date, uniform, absence, inventory, hygiene, behaviour };
}

const rows = [
  createData('19-23-2023', 159, 6.0, 24, 4.0, 'gio'),
  createData('19-23-2023', 237, 9.0, 37, 4.3, 'gio'),
  createData('19-23-2023', 262, 16.0, 24, 6.0, 'gio'),
  createData('19-23-2023', 305, 3.7, 67, 4.3, 'gio'),
  createData('19-23-2023', 356, 16.0, 49, 3.9, 'gio'),
];

export default function EthicTable() {
  return (
    <div className='ethicCnt'>
    <TableContainer component={Paper}>
      <Table sx={{ minWidth: 650 }} aria-label="simple table">
        <TableHead>
          <TableRow>
            <TableCell align="center">თარიღი</TableCell>
            <TableCell align="center"><div>მოსწავლის ფორმითი გამოცხადება</div>
            <TableRow>
              <TableCell className='ethicCnt__w week' rowSpan={1}>I კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>II კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>III კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>IV კვირა</TableCell>
              </TableRow>
            </TableCell>
            <TableCell align="center"><div>მოსწავლის დაგვიანება</div>
            <TableRow>
              <TableCell className='ethicCnt__w week' rowSpan={1}>I კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>II კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>III კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>IV კვირა</TableCell>
              </TableRow>
            </TableCell>
            <TableCell align="center"><div>საკლასო ინვენტარის მოვლა</div>
            <TableRow>
              <TableCell className='ethicCnt__w week' rowSpan={1}>I კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>II კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>III კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>IV კვირა</TableCell>
              </TableRow>
            </TableCell>
            <TableCell align="center"><div>მოსწავლის მიერ ჰიგიენური ნორმების დაცვა</div>
            <TableRow>
              <TableCell className='ethicCnt__w week' rowSpan={1}>I კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>II კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>III კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>IV კვირა</TableCell>
              </TableRow>
            </TableCell>
            <TableCell align="center"><div>მოსწავლის ყოფაქცევა</div>
            <TableRow>
              <TableCell className='ethicCnt__w week' rowSpan={1}>I კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>II კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>III კვირა</TableCell>
              <TableCell className='ethicCnt__w week' rowSpan={1}>IV კვირა</TableCell>
              </TableRow>
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow
              key={row.date}
              sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
            >
              <TableCell>
                {row.date}
              </TableCell>
              <TableCell align="right">
              <TableRow>
                <TableCell className='ethicCnt__w' rowSpan={1}>{'HELLO'}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{'HELLO'}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{'HELLO'}</TableCell>
                <TableCell className='ethicCnt__w v2' rowSpan={1}>{'HELLO'}</TableCell>
                </TableRow>
              </TableCell>
              <TableCell align="right">
              <TableRow>
                <TableCell className='ethicCnt__w' rowSpan={1}>{'HELLO'}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{'HELLO'}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{'HELLO'}</TableCell>
                <TableCell className='ethicCnt__w v2' rowSpan={1}>{'HELLO'}</TableCell>
                </TableRow>
              </TableCell>
              <TableCell align="right">
              <TableRow>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.inventory}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.inventory}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.inventory}</TableCell>
                <TableCell className='ethicCnt__w v2' rowSpan={1}>{row.inventory}</TableCell>
              </TableRow>
              </TableCell>
              <TableCell align="right">
              <TableRow>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.hygiene}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.hygiene}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.hygiene}</TableCell>
                <TableCell className='ethicCnt__w v2' rowSpan={1}>{row.hygiene}</TableCell>
              </TableRow>
              </TableCell>
              <TableCell align="right">
              <TableRow>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.hygiene}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.hygiene}</TableCell>
                <TableCell className='ethicCnt__w' rowSpan={1}>{row.hygiene}</TableCell>
                <TableCell className='ethicCnt__w v2' rowSpan={1}>{row.hygiene}</TableCell>
              </TableRow>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
    </div>
  );
}