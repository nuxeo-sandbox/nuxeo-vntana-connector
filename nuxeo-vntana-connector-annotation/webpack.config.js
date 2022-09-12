// Generated using webpack-cli https://github.com/webpack/webpack-cli
const dotenv = require('dotenv');
const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');
const isProduction = process.env.NODE_ENV == 'production';


const stylesHandler = 'style-loader';

dotenv.config();

const config = {
    entry: './src/index.js',
    output: {
        path: path.resolve(__dirname, 'dist'),
    },
    devServer: {
        open: true,
        host: 'localhost',
        port: 8000,
        open: ['/index.html?docId='+process.env.TEST_DOC_ID],
        proxy: {
            '/nuxeo': {
              target: process.env.DEV_SERVER_URL,
              logLevel: "debug",
              ws: true,
              changeOrigin: true
            }
        }
    },
    plugins: [
        new ESLintPlugin(),
        new HtmlWebpackPlugin({
            template: 'index.html',
            title:"3D Annotation"
        }),

        // Add your plugins here
        // Learn more about plugins from https://webpack.js.org/configuration/plugins/
    ],
    module: {
        rules: [
            {
                test: /\.css$/i,
                use: [stylesHandler,'css-loader'],
            },
            {
                test: /\.(eot|svg|ttf|woff|woff2|png|jpg|gif)$/i,
                type: 'asset',
            },

            // Add your rules for custom modules here
            // Learn more about loaders from https://webpack.js.org/loaders/
        ],
    },
};

module.exports = () => {   
    if (isProduction) {
        config.mode = 'production';
    } else {
        config.mode = 'development';
    }
    return config;
};
