var fs = require('fs');
var data = fs.readFileSync('autoindent_test.txt', 'utf8').trim();
var cases = data.split(/\n?=====\n?/);

for (var i = 0; i < cases.length; i++)
{
    
}