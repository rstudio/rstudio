
const { task, context } = require("fuse-box/sparky");
const { FuseBox, CSSPlugin, WebIndexPlugin, QuantumPlugin } = require("fuse-box");

const path = require('path');
const devserver = require('./dev/server.js')

const kLibraryName = "panmirror"
const kOutputDir = "./dist"
const kIdeJsDir = "../../../www/js"
const kIdeOutputDir = path.join(kIdeJsDir, kLibraryName);

context(
  class {
    getConfig(outputDir, devServer = false) {
      return FuseBox.init({
        homeDir: "src",
        modulesFolder: "../../node_modules",
        target: "browser@es6",
        globals: { default: kLibraryName },
        output: path.join(outputDir, "$name.js"),
        sourceMaps: { inline: true },
        plugins: [
          CSSPlugin(),
          devServer && WebIndexPlugin({ template: "dev/index.html" }),
          this.isProduction && QuantumPlugin({ uglify: { es6: true }, bakeApiIntoBundle: true, containedAPI: true }),
        ],
      });
    }
  },
);

const bundle = (fuse) => {
  return fuse
    .bundle(kLibraryName)
    .instructions("> editor.ts")
} 

const watch = (fuse) => {
  return fuse
    .hmr()
    .watch()
}

const dev = (context, devServer, watchChanges, outputDir = kOutputDir) => {
  const fuse = context.getConfig(outputDir, devServer);
  if (devServer) {
    fuse.dev( { root: false }, server => {
      const app = server.httpServer.app;
      devserver.initialize(app)
      const dist = path.resolve(outputDir);
      app.get("*", function(req, res) {
        res.sendFile(path.join(dist, req.path));
      });
    })
  }
  const bdl = bundle(fuse)
  if (watchChanges)
    watch(bdl)
  return fuse;
}

const dist = (context, outputDir = kOutputDir) => {
  context.isProduction = true;
  const fuse = context.getConfig(outputDir);
  bundle(fuse);
  return fuse;
}

task("dev", async context => {
  await dev(context, true, true).run()
});

task("dist", async context => {
  await dist(context).run();
});


task("ide-dev", async context => {
  await dev(context, false, false, kIdeOutputDir).run()
});

task("ide-dev-watch", async context => {
  await dev(context, false, true, kIdeOutputDir).run()
})


task("ide-dist",async context => {
  await dist(context, kIdeOutputDir).run();
});



