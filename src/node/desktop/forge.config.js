
const process = require("process");

const config = {
  plugins: [
    {
      name: '@electron-forge/plugin-vite',
      config: {
        build: [
          {
            entry: 'src/main/main.ts',
            config: 'vite.main.config.ts',
          },
          {
            entry: 'src/renderer/preload.ts',
            config: 'vite.preload.config.ts',
            target: 'preload',
          },
          {
            entry: 'src/ui/widgets/choose-r/preload.ts',
            config: 'vite.choose-r-preload.config.ts',
            target: 'preload',
          },
        ],
        renderer: [
          {
            name: 'main_window',
            config: 'vite.renderer.config.ts',
          },
        ],
      },
    },
  ],

  // https://electron.github.io/electron-packager/main/interfaces/electronpackager.options.html
  packagerConfig: {
    icon: './resources/icons/RStudio',
    appBundleId: 'com.rstudio.desktop',
    appCopyright: `Copyright (C) ${new Date().getFullYear()} by Posit Software, PBC`,
    name: 'RStudio',
    executableName: process.platform === 'darwin' ? 'RStudio' : 'rstudio',
    win32metadata: {
      CompanyName: "Posit Software, PBC",
      FileDescription: "RStudio",
      InternalName: "RStudio",
      ProductName: "RStudio",
    },
    extendInfo: './Info.plist',
  },
};

module.exports = config;
