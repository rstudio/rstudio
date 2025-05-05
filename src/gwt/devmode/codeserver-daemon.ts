
import { spawn } from 'node:child_process';
import { createServer, Socket } from 'node:net'
import path = require('node:path');
import { chdir, platform } from 'node:process';
import { createInterface } from 'node:readline';
import { ENDPOINT } from './codeserver-common';

chdir(path.join(__dirname, '..'));

function ant() {
  if (platform === 'win32') {
    return 'ant.exe';
  } else if (platform === 'darwin') {
    return 'ant';
  } else {
    return '/usr/bin/ant';
  }
}

function broadcast(data: string) {
  buffer.push(data);
  for (const socket of sockets) {
    socket.write(`${data}\n`);
  }
}

let buffer: string[] = [];
let sockets: Socket[] = [];
let ready = false;

const server = createServer();

server.on('listening', () => {

  const child = spawn(ant(), ['devmode'], {
    stdio: 'pipe',
  });

  child.on('error', (err) => {
    process.exit(1);
  });

  child.on('exit', (code) => {
    process.exit(1);
  });

  const reader = createInterface({
    input: child.stdout,
    crlfDelay: Infinity,
  });

  reader.on('line', (data) => {
    console.log(data)
    ready = ready || data.includes('The code server is ready');
    broadcast(data);
  });

});

server.on('connection', (socket) => {

  sockets.push(socket);

  socket.on('close', () => {
    const index = sockets.indexOf(socket);
    sockets = sockets.splice(index, 1);
  });

  socket.on('error', (err) => {
    const index = sockets.indexOf(socket);
    sockets = sockets.splice(index, 1);
  });

  const status = { ready };
  socket.write(JSON.stringify(status) + '\n');
  socket.write(buffer.join('\n') + '\n');

});

server.listen(ENDPOINT);
