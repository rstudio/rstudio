const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');

const kVersion = '--version';
const kVersionJson = '--version-json';

const exitArgs = [kVersion, kVersionJson, '--help', 'help'];

const async = !process.argv.some(arg => exitArgs.indexOf(arg) >= 0);

module.exports = [new ForkTsCheckerWebpackPlugin({ async })];
