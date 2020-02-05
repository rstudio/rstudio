/*
 * server.js
 *
 * Copyright (C) 2019-20 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

const express = require('express');
const child_process = require('child_process')
const bodyParser = require('body-parser')


function initialize(app) {

  function pandoc(args, input, response_fn, error_fn) {

    let spawn = child_process.spawn;
    let process = spawn('pandoc', args);
     
    let output = '';
    process.stdout.setEncoding = 'utf-8';
    process.stdout.on('data', data => {
      output = output + data;
    });
  
    let error = '';
    process.stderr.setEncoding = 'utf-8';
    process.stderr.on('data', data => {
      error = error + data;
    });
  
    process.on('close', status => {
      if (status === 0) {
        response_fn(output);
      } else {
        error_fn(error.trim());
      }
    })
   
    if (input !== null) {
      process.stdin.setEncoding = 'utf-8';
      process.stdin.write(input);
      process.stdin.end();
    }
  }

  app.use(bodyParser.json({limit: '1000mb', extended: true}))

  app.post('/pandoc/ast', express.json(), function(request, response) {
    pandoc(
      ['--from', request.body.format, '--to', 'json'].concat(request.body.options || []),
      request.body.markdown,
      output => { response.json( { ast: JSON.parse(output) } ) } ,
      error => { response.json( { error: error }) }
    );
  });

  app.post('/pandoc/markdown', express.json(), function(request, response) {
    pandoc(
      ['--from', 'json', '--to', request.body.format].concat(request.body.options || []),
      JSON.stringify(request.body.ast),
      output => { response.json( { markdown: output }) },
      error => { response.json( { error: error }) }
    );
  })

  app.post('/pandoc/extensions', express.json(), function(request, response) {
    const format = request.body.format;
    const list = '--list-extensions' + (format ? `=${format}` : '');
    pandoc(
      [list],
      null,
      output => { response.json( { extensions: output }) },
      error => { response.json( { error: error }) }
    )
  })
}


module.exports.initialize = initialize;


