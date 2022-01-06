const { createFullPackageFileName } = require('./scripts/create-full-package-file-name.js');
// This function makes sure that the correct filename is created and saved for the final DMG file.
createFullPackageFileName();

// require('./scripts/fix-library-paths.js');

// loadCMakeVars(__dirname + '/../../../package/osx/build/CMakeCache.txt');


const dmgSize = {
  height: 450,
  width: 801,
};
const iconSize = Math.round((dmgSize.width * 12.3) / 100);

const dmgIconPosition = { x: Math.round((dmgSize.width * 29.23) / 100), y: Math.round((dmgSize.height * 57.99) / 100) };
const dmgApplicationsPosition = {
  x: Math.round((dmgSize.width * 68.69) / 100),
  y: Math.round((dmgSize.height * 57.99) / 100),
};

const config = {
  hooks: {
    postPackage: async (forgeConfig, options) => {
      // Run import-resources.ts script to copy all the non-Electron bits
      // produced by the cmake build/install into the Electron package
      const spin = options.spinner.start('Importing externally built dependencies');

      const util = require('util');
      const execFile = util.promisify(require('child_process').execFile);
      var path = require('path');
      const tsNode = path.join(__dirname, 'node_modules', '.bin', 'ts-node');
      const script = path.join(__dirname, 'scripts', 'import-resources.ts');
      const promise = execFile(tsNode, [script]);
      promise.catch(function (e) {
        spin.fail(e.message);
      });
      const child = promise.child;
      child.stdout.on('data', function (data) {
        spin.info(data);
      });
      child.stderr.on('data', function (data) {
        spin.info(data);
      });
      child.on('close', function (code) {
        if (code) {
          spin.fail(`Import-resources exited with code: ${code}`);
        }
      });
      return promise;
    },
  },
  makers: [
    {
      name: '@electron-forge/maker-squirrel',
      config: {
        name: 'rstudio',
      },
    },
    {
      name: '@electron-forge/maker-zip',
      platforms: ['linux', 'win32'],
    },
    {
      name: '@electron-forge/maker-deb',
      config: {},
    },
    {
      name: '@electron-forge/maker-rpm',
      config: {},
    },
    {
      name: '@electron-forge/maker-dmg',
      config: {
        format: 'ULFO',
        name: 'Rstudio-electron-app',
        background: './resources/background/dmg-background.tiff',
        icon: './resources/icons/RStudio.icns',
        iconSize,
        additionalDMGOptions: {
          window: {
            size: dmgSize,
          },
        },
        contents: [
          {
            ...dmgIconPosition,
            type: 'file',
            path: __dirname + '/out/RStudio-darwin-x64/RStudio.app',
          },
          {
            ...dmgApplicationsPosition,
            type: 'link',
            path: '/Applications',
          },
        ],
      },
    },
  ],
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
  packagerConfig: {
    icon: './resources/icons/RStudio',
  },
};

// if (
//   process.env.APPLE_ID !== undefined &&
//   process.env.APPLE_ID_PASSWORD !== undefined &&
//   process.env.APPLE_ID !== '' &&
//   process.env.APPLE_ID_PASSWORD !== ''
// ) {


// config.packagerConfig = {
//   ...config.packagerConfig,
//   appBundleId: 'org.rstudio.RStudio',
//   osxSign: {
//     'identity': 'Developer ID Application: RStudio Inc. (FYF2F5GFX4)',
//     'entitlements': 'resources/electron-entitlements.mac.plist',
//     'entitlements-inherit': 'resources/electron-entitlements.mac.plist',
//     'hardenedRuntime': true,
//     'hardened-runtime': true,
//     'gatekeeper-assess': false,
//     'signature-flags': 'library',
//   },
//   osxNotarize: {
//     appleId: process.env.APPLE_ID,
//     appleIdPassword: process.env.APPLE_ID_PASSWORD,
//     appBundleId: 'org.rstudio.RStudio',
//     // ascProvider: 'FYF2F5GFX4',
//   },
// };



config.packagerConfig = {
  ...config.packagerConfig,
  appBundleId: 'com.rstudio.mt-test',
  osxSign: {
    'identity': 'Developer ID Application: Matheus Tavares (QM39MZ3QRV)',
    'entitlements': 'resources/electron-entitlements.mac.plist',
    'entitlements-inherit': 'resources/electron-entitlements-inherit.mac.plist',
    'hardened-runtime': true,
    'gatekeeper-assess': false,
    'signature-flags': 'library',
  },
  osxNotarize: {
    appleId: 'contact@tavapps.com',
    appleIdPassword: 'igzh-ooso-rjvg-yyoy',
    appBundleId: 'com.rstudio.mt-test',
    ascProvider: 'QM39MZ3QRV',
  },
};


// } else {
//   console.warn('Should be notarizing, but environment variables APPLE_ID or APPLE_ID_PASSWORD are missing!');
// }

module.exports = config;
