const path = require('path');

module.exports = {
  entry: './node_modules/yaml/dist/index.js',
  output: {
    filename: 'yaml.min.js',
    path: path.resolve(__dirname, '../../www/js'),
    library: 'YAML',
    libraryTarget: 'umd',
  },
  mode: 'production', // Enables minification
};

