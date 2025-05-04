import { Socket } from "net";
import { createInterface } from "readline";

const socket = new Socket();

socket.on('error', (err) => {
    console.log(err);
    process.exit(1);
});

const reader = createInterface({
    input: socket,
    crlfDelay: Infinity,
});

reader.once('line', (line) => {
    const status = JSON.parse(line);
    reader.on('line', (line) => {
        console.log(line);
    });
});

socket.connect(6789, '127.0.0.1');
