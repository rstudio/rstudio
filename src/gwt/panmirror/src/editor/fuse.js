
const { task, context } = require("fuse-box/sparky");
const { FuseBox, CSSPlugin, WebIndexPlugin, QuantumPlugin } = require("fuse-box");

const path = require('path');
const devserver = require('./dev/server.js')

const kLibraryName = "Panmirror"
const kLibraryNameLower = kLibraryName.toLowerCase()
const kOutputDir = "./dist"
const kIdeJsDir = "../../../www/js"
const kIdeOutputDir = path.join(kIdeJsDir, kLibraryNameLower);

context(
  class {
    getConfig(outputDir, webIndex = false) {
      return FuseBox.init({
        homeDir: "src",
        modulesFolder: "../../node_modules",
        target: "browser@es6",
        globals: { default: kLibraryName },
        output: path.join(outputDir, "$name.js"),
        sourceMaps: { inline: true },
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
  const fuse = context.getConfig(outputDir, webIndex);
  const bdl = bundle(fuse)
  if (watchChanges)
    watch(bdl, hmrReload)
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



