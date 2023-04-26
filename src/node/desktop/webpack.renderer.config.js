const rules = require('./webpack.rules');
const plugins = require('./webpack.plugins');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

rules.push({
  test: /\.css$/,
  use: [{ loader: 'style-loader' }, { loader: 'css-loader' }],
});

const copyWebpackPlugin = new CopyWebpackPlugin({
  patterns: [
    {
      from: path.resolve(__dirname, 'src', 'assets'),
      to: path.resolve(__dirname, '.webpack/renderer', 'assets'),
    },
  ],
});

plugins.push(copyWebpackPlugin);

module.exports = {
  module: {
    rules,
  },
  plugins,
  resolve: {
    extensions: ['.js', '.ts', '.jsx', '.tsx', '.css', '.vue'],
  },
};
