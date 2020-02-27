/*
 * fuse.js
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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
const { FuseBox, CSSPlugin, WebIndexPlugin, QuantumPlugin } = require("fuse-box");

const path = require('path');
const fs = require('fs');
const devserver = require('./dev/server.js')

const kLibraryName = "Panmirror"
const kLibraryNameLower = kLibraryName.toLowerCase()
const kOutputDir = "./dist"
const kIdeJsDir = "../../../www/js"
const kIdeOutputDir = path.join(kIdeJsDir, kLibraryNameLower);

context(
  class {
    getConfig(outputDir, webIndex = false, vendorSrcMap = false) {
      return FuseBox.init({
        homeDir: "src",
        modulesFolder: "../../node_modules",
        target: "browser@es6",
        globals: { default: kLibraryName },
        output: path.join(outputDir, "$name.js"),
        sourceMaps: { inline: true , project: true, vendor: vendorSrcMap },
        plugins: [
          CSSPlugin(),
          webIndex && WebIndexPlugin({ template: "dev/index.html" }),
          this.isProduction && QuantumPlugin({ uglify: { es6: true }, bakeApiIntoBundle: true, containedAPI: true }),
        ],
      });
    }
  },
);

const bundle = (fuse) => {
  return fuse
    .bundle(kLibraryNameLower)
    .instructions("> editor.ts")
} 

const watch = (fuse, hmrReload) => {
  return fuse
    .hmr({ reload: hmrReload })
    .watch()
}

const dev = (context, webIndex, watchChanges, hmrReload, outputDir = kOutputDir) => {
  
  // setup fuse for development
  const fuse = context.getConfig(outputDir, webIndex, true);
  const bdl = bundle(fuse)
  if (watchChanges)
    watch(bdl, hmrReload)
  
  // copy prosemirror-devtools
  const devtools = 'prosemirror-dev-tools.min.js';
  fs.copyFileSync(
    path.join('../../node_modules/prosemirror-dev-tools/dist/umd', devtools),
    path.join(outputDir, devtools)
  );

  return fuse;
}

const dist = (context, outputDir = kOutputDir) => {
  context.isProduction = true;
  const fuse = context.getConfig(outputDir);
  bundle(fuse);
  return fuse;
}

task("dev", async context => {
  const fuse = dev(context, true, true, false);
  fuse.dev( { root: false }, server => {
    const app = server.httpServer.app;
    devserver.initialize(app)
    const dist = path.resolve(kOutputDir);
    app.get("*", function(req, res) {
      res.sendFile(path.join(dist, req.path));
    });
  })
  await fuse.run()
});

task("dist", async context => {
  await dist(context).run();
});


task("ide-dev", async context => {
  await dev(context, false, false, false, kIdeOutputDir).run()
});

task("ide-dev-watch", async context => {
  const fuse = dev(context, false, true, true, kIdeOutputDir);
  fuse.dev( { httpServer: false } )
  await fuse.run();
})


task("ide-dist",async context => {
  await dist(context, kIdeOutputDir).run();
});



