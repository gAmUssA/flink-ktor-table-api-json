const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const webpack = require('webpack');

// Load environment variables from .env file if present
const dotenv = require('dotenv');
const env = dotenv.config().parsed || {};

// Create a default set of environment variables
const defaultEnv = {
  'process.env.REACT_APP_API_BASE_URL': JSON.stringify('http://localhost:8090'),
  'process.env.REACT_APP_WS_FLIGHTS_LIVE': JSON.stringify('/api/flights/live'),
  'process.env.REACT_APP_API_FLIGHTS_DENSITY': JSON.stringify('/api/flights/density'),
  'process.env.REACT_APP_API_FLIGHTS_DELAYED': JSON.stringify('/api/flights/delayed'),
};

// Override defaults with any environment variables from .env file
const envKeys = Object.keys(env || {}).reduce((prev, next) => {
  prev[`process.env.${next}`] = JSON.stringify(env[next]);
  return prev;
}, {...defaultEnv});

module.exports = {
  entry: './src/index.ts',
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader'],
      },
      {
        test: /\.(png|svg|jpg|jpeg|gif)$/i,
        type: 'asset/resource',
      },
      {
        test: /\.(woff|woff2|eot|ttf|otf)$/i,
        type: 'asset/resource',
      },
    ],
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js'],
  },
  output: {
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'dist'),
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: './src/index.html',
    }),
    // Make environment variables available in the application
    new webpack.DefinePlugin(envKeys),
  ],
  devServer: {
    static: {
      directory: path.join(__dirname, 'dist'),
    },
    compress: true,
    port: 9000,
    proxy: {
      '/api': 'http://localhost:8090',
      '/ws': {
        target: 'ws://localhost:8090',
        ws: true,
      },
    },
  },
};
