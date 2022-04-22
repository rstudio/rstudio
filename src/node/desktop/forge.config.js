const config = {
  plugins: [
    [
      '@electron-forge/plugin-webpack',
      {
        mainConfig: './webpack.main.config.js',
        renderer: {
          config: './webpack.renderer.config.js',
          entryPoints: [
            {
              js: './src/renderer/renderer.ts',
              name: 'main_window',
              preload: {
                js: './src/renderer/preload.ts',
              },
            },
            {
              html: './src/ui/loading/loading.html',
              js: './src/ui/loading/loading.ts',
              name: 'loading_window',
            },
            {
              html: './src/ui/error/error.html',
              js: './src/ui/error/error.ts',
              name: 'error_window',
            },
            {
              html: './src/ui/connect/connect.html',
              js: './src/ui/connect/connect.ts',
              name: 'connect_window',
            },
            {
              html: './src/ui/widgets/choose-r/ui.html',
              js: './src/ui/widgets/choose-r/load.ts',
              preload: {
                js: './src/ui/widgets/choose-r/preload.ts',
              },
              name: 'choose_r',
            },
          ],
        },
      },
    ],
  ],

  // https://electron.github.io/electron-packager/main/interfaces/electronpackager.options.html 
  packagerConfig: {
    icon: './resources/icons/RStudio',
    appBundleId: 'com.rstudio.desktop',
    appCopyright: 'Copyright (C) 2022 by RStudio, PBC',
    name: 'RStudio',
    executableName: 'rstudio',
    win32metadata: {
      CompanyName: "RStudio, PBC",
      FileDescription: "RStudio",
      InternalName: "RStudio",
      ProductName: "RStudio",
    }
  },
};

module.exports = config;
