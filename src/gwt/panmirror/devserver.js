
const express = require('express');  
const devserver = require('./src/editor/dev/server.js')

const app = express();
devserver.initialize(app);
app.listen(8080);


