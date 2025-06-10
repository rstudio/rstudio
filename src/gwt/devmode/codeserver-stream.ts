import { Socket } from "net";
import { createInterface } from "readline";
import { ENDPOINT } from "./codeserver-common";

const socket = new Socket();

socket.on('error', (err) => {
    console.log(err);
    process.exit(1);
});

const reader = createInterface({
    input: socket,
    crlfDelay: Infinity,
});

reader.once('line', (_line) => {
    reader.on('line', (line) => {
        console.log(line);
    });
});

socket.connect(ENDPOINT);
