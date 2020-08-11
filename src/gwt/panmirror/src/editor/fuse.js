/*
 * fuse.js
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

const { task, context } = require("fuse-box/sparky");
const { FuseBox, CSSPlugin, CSSResourcePlugin, ImageBase64Plugin, WebIndexPlugin, QuantumPlugin } = require("fuse-box");

const path = require('path');
const fs = require('fs');

const kLibraryName = "Panmirror"
const kLibraryNameLower = kLibraryName.toLowerCase()
const kIdeJsDir = "../../../www/js"
const kIdeOutputDir = path.join(kIdeJsDir, kLibraryNameLower);
const kGlobalNodeModulesPath = "/opt/rstudio-tools/panmirror/node_modules";

context(
  class {
    getConfig(outputDir, webIndex = false, vendorSrcMap = false) {
      return FuseBox.init({
        homeDir: "src",
        modulesFolder: ["./node_modules", fs.existsSync(kGlobalNodeModulesPath) ? kGlobalNodeModulesPath : ""],
        target: "browser@es6",
        globals: { default: kLibraryName },
        output: path.join(outputDir, "$name.js"),
        sourceMaps: { inline: true, project: true, vendor: vendorSrcMap && !this.isProduction },
        plugins: [
          webIndex && WebIndexPlugin({ template: "dev/index.html" }),
          [CSSResourcePlugin({ inline: true }), CSSPlugin()],
          ImageBase64Plugin(),
          this.isProduction && QuantumPlugin({ uglify: { es6: true }, bakeApiIntoBundle: true, containedAPI: true }),
        ],
      });
    }
  },
);

const bundle = (fuse) => {
  return fuse
    .bundle(kLibraryNameLower)
    .instructions("> index.ts")
}

const copyDevTools = (outputDir) => {
  // copy prosemirror-devtools
  const devtools = 'prosemirror-dev-tools.min.js';
  const nodeModules = fs.existsSync(kGlobalNodeModulesPath) ? kGlobalNodeModulesPath : './node_modules';
  fs.copyFileSync(
    path.join(nodeModules + '/prosemirror-dev-tools/dist/umd', devtools),
    path.join(outputDir, devtools)
  );
}

const watch = (fuse, hmrReload) => {
  return fuse
    .hmr({ reload: hmrReload })
    .watch()
}

const dev = (context, webIndex, watchChanges, hmrReload, outputDir) => {

  // setup fuse for development
  const fuse = context.getConfig(outputDir, webIndex, true);
  const bdl = bundle(fuse)
  if (watchChanges)
    watch(bdl, hmrReload)

  // copy prosemirror-devtools
  copyDevTools(outputDir)

  return fuse;
}

const dist = (context, outputDir) => {
  context.isProduction = true;
  const fuse = context.getConfig(outputDir);
  bundle(fuse);
  copyDevTools(outputDir)
  return fuse;
}

task("ide-dev", async context => {
  await dev(context, false, false, false, kIdeOutputDir).run()
});

task("ide-dev-watch", async context => {
  const fuse = dev(context, false, true, true, kIdeOutputDir);
  fuse.dev();
  await fuse.run();
})

task("ide-dist", async context => {
  await dist(context, kIdeOutputDir).run();
});


