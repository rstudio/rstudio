const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');
const copyWebpackPlugin = new CopyWebpackPlugin({
  patterns: [
    {
      from: path.resolve(__dirname, 'src', 'assets'),
      to: path.resolve(__dirname, '.webpack/main', 'assets'),
    },
  ],
});

module.exports = {
  /**
   * This is the main entry point for your application, it's the first file
   * that runs in the main process.
   */
  entry: './src/main/main.ts',
  // Put your normal webpack config below here
  module: {
    rules: require('./webpack.rules'),
  },
  resolve: {
    extensions: ['.js', '.ts', '.jsx', '.tsx', '.css', '.json'],
    modules: ['node_modules'],
  },
  plugins: [copyWebpackPlugin],
};
