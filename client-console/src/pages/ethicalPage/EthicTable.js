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
  createData('Frozen yoghurt', 159, 6.0, 24, 4.0, 'gio'),
  createData('Ice cream sandwich', 237, 9.0, 37, 4.3, 'gio'),
  createData('Eclair', 262, 16.0, 24, 6.0, 'gio'),
  createData('Cupcake', 305, 3.7, 67, 4.3, 'gio'),
  createData('Gingerbread', 356, 16.0, 49, 3.9, 'gio'),
];

export default function EthicTable() {
  return (
    <div className='ethicCnt'>
    <TableContainer component={Paper}>
      <Table sx={{ minWidth: 650 }} aria-label="simple table">
        <TableHead>
          <TableRow>
            <TableCell align="center">თარიღი</TableCell>
            <TableCell align="center">მოსწავლის ფორმითი გამოცხადება</TableCell>
            <TableCell align="center">მოსწავლის დაგვიანება</TableCell>
            <TableCell align="center">საკლასო ინვენტარის მოვლა</TableCell>
            <TableCell align="center">მოსწავლის მიერ ჰიგიენური ნორმების დაცვა</TableCell>
            <TableCell align="center">მოსწავლის ყოფაქცევა</TableCell>
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
              <TableCell align="right">{row.uniform}</TableCell>
              <TableCell align="right">{row.absence}</TableCell>
              <TableCell align="right">{row.inventory}</TableCell>
              <TableCell align="right">{row.hygiene}</TableCell>
              <TableCell align="right">{row.behaviour}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
    </div>
  );
}