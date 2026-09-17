/*
 * Regression tests for terminal file links, using the bundled xterm buffer.
 * Run with: node --test src/gwt/test/terminal_file_links.test.cjs
 */

const assert = require("node:assert/strict");
const test = require("node:test");
const xtermPath = "../src/org/rstudio/studio/client/workbench/views/terminal/xterm/";
const { Terminal } = require(xtermPath + "xterm.js");
require(xtermPath + "file-links.js");
const { FileLinkProvider } = globalThis.RStudioFileLinks;

async function createTerminal(t, text, cols = 80)
{
   const terminal = new Terminal({ cols, rows: 10, allowProposedApi: true });
   t.after(() => terminal.dispose());
   await new Promise(resolve => terminal.write(text, resolve));
   return terminal;
}

function activate(links)
{
   assert.equal(links.length, 1);
   links[0].activate({ button: 0, ctrlKey: true });
}

test("a cwd change discards pending links and their cache entries", async t => {
   const terminal = await createTerminal(t, "file.R");
   const requests = [];
   const opened = [];
   const provider = new FileLinkProvider(terminal, {
      resolve: (paths, callback) => requests.push(callback),
      open: path => opened.push(path)
   });

   let staleDelivered = false;
   provider.provideLinks(1, () => { staleDelivered = true; });
   provider.clearCache();
   requests[0](["/old/file.R"]);
   assert.equal(staleDelivered, false);

   provider.provideLinks(1, activate);
   assert.equal(requests.length, 2);
   requests[1](["/new/file.R"]);
   assert.deepEqual(opened, ["/new/file.R"]);
});

test("moving to an empty row supersedes a pending resolution", async t => {
   const terminal = await createTerminal(t, "file.R\r\n");
   let resolve;
   const provider = new FileLinkProvider(terminal, {
      resolve: (paths, callback) => { resolve = callback; }
   });
   let staleDelivered = false;
   provider.provideLinks(1, () => { staleDelivered = true; });
   provider.provideLinks(2, links => assert.equal(links, undefined));
   resolve(["/cwd/file.R"]);
   assert.equal(staleDelivered, false);
});

test("a late reply from the previous cwd cannot overwrite a fresh resolution", async t => {
   const terminal = await createTerminal(t, "file.R");
   const requests = [];
   const opened = [];
   const provider = new FileLinkProvider(terminal, {
      resolve: (paths, callback) => requests.push(callback),
      open: path => opened.push(path)
   });

   provider.provideLinks(1, () => assert.fail("stale links were delivered"));
   provider.clearCache();
   provider.provideLinks(1, activate);
   requests[1](["/new/file.R"]);
   requests[0](["/old/file.R"]);

   provider.provideLinks(1, activate);
   assert.equal(requests.length, 2);
   assert.deepEqual(opened, ["/new/file.R", "/new/file.R"]);
});

for (const row of [1, 2, 3])
{
   test(`a quoted path spanning three rows resolves when hovering row ${row}`, async t => {
      const path = "dir with spaces/and more/file name.R";
      const terminal = await createTerminal(t, '"' + path + '"', 16);
      assert.equal(terminal.buffer.active.getLine(2).isWrapped, true);
      const opened = [];
      const provider = new FileLinkProvider(terminal, {
         resolve: (paths, callback) => {
            assert.deepEqual(paths, [path]);
            callback(["/cwd/" + path]);
         },
         open: resolved => opened.push(resolved)
      });

      let delivered = false;
      provider.provideLinks(row, links => {
         delivered = true;
         assert.equal(links.length, 1);
         assert.equal(links[0].text, path);
         assert.deepEqual(links[0].range, {
            start: { x: 2, y: 1 },
            end: { x: (path.length + 1) % 16, y: 3 }
         });
         activate(links);
      });
      assert.equal(delivered, true);
      assert.deepEqual(opened, ["/cwd/" + path]);
   });
}

test("rewritten terminal output discards a pending link response", async t => {
   const terminal = await createTerminal(t, "old.R");
   const requests = [];
   const opened = [];
   const provider = new FileLinkProvider(terminal, {
      resolve: (paths, callback) => requests.push(callback),
      open: path => opened.push(path)
   });

   let staleDelivered = false;
   provider.provideLinks(1, () => { staleDelivered = true; });
   await new Promise(resolve => terminal.write("\rnew.R", resolve));
   requests[0](["/cwd/old.R"]);
   assert.equal(staleDelivered, false);

   provider.provideLinks(1, activate);
   requests[1](["/cwd/new.R"]);
   assert.deepEqual(opened, ["/cwd/new.R"]);
});

test("switching terminal buffers discards a pending link response", async t => {
   const terminal = await createTerminal(t, "file.R");
   let resolve;
   const provider = new FileLinkProvider(terminal, {
      resolve: (paths, callback) => { resolve = callback; }
   });
   let staleDelivered = false;
   provider.provideLinks(1, () => { staleDelivered = true; });
   await new Promise(done => terminal.write("\x1b[?1049hfile.R", done));
   resolve(["/cwd/file.R"]);
   assert.equal(staleDelivered, false);
});

test("a delivered link cannot open a file from a previous cwd", async t => {
   const terminal = await createTerminal(t, "file.R");
   const opened = [];
   const provider = new FileLinkProvider(terminal, {
      resolve: (paths, callback) => callback(["/old/file.R"]),
      open: path => opened.push(path)
   });
   let delivered;
   provider.provideLinks(1, links => { delivered = links; });
   provider.clearCache();
   activate(delivered);
   assert.deepEqual(opened, []);
});

/**
 * A stand-in terminal exposing a real terminal's buffer plus the DOM hooks the
 * provider listens on, so a test can deliver key and mouse events to it.
 */
function withEventHooks(terminal)
{
   const events = {};
   const listen = (type, handler) => { events[type] = handler; };
   return {
      cols: terminal.cols,
      buffer: terminal.buffer,
      element: {
         addEventListener: listen,
         ownerDocument: { addEventListener: listen, defaultView: { addEventListener: listen } }
      },
      events
   };
}

/**
 * Resolve the single link on the first row of `text`, along with the event
 * hooks that drive the modifier state. That state is global, so the modifier
 * is released again once the test ends.
 */
async function createLink(t, text = "file.R")
{
   const terminal = await createTerminal(t, text);
   const view = withEventHooks(terminal);
   const opened = [];
   const provider = new FileLinkProvider(view, {
      resolve: (paths, callback) => callback(paths.map(path => "/cwd/" + path)),
      open: path => opened.push(path),
      hover: () => {},
      leave: () => {}
   });

   let links;
   provider.provideLinks(1, result => { links = result; });
   assert.equal(links.length, 1);

   t.after(() => {
      links[0].leave();
      view.events.keyup({});
   });

   return { link: links[0], events: view.events, opened };
}

/**
 * Mimic the hover bookkeeping of xterm's linkifier: it paints the link with
 * the decorations it was built with, then replaces them with live accessors
 * that repaint as they are written. The object standing in for those accessors
 * is returned, so a test can see what the link currently looks like.
 */
function startHover(link)
{
   const painted = {
      underline: link.decorations.underline,
      pointerCursor: link.decorations.pointerCursor
   };

   link.hover();
   link.decorations = painted;
   return painted;
}

test("a link is decorated only while the open-link modifier is held", async t => {
   const { link, events } = await createLink(t);
   assert.equal(link.decorations.underline, false);
   assert.equal(link.decorations.pointerCursor, false);

   events.keydown({ ctrlKey: true });
   assert.equal(link.decorations.underline, true);
   assert.equal(link.decorations.pointerCursor, true);

   events.keyup({});
   assert.equal(link.decorations.underline, false);
   assert.equal(link.decorations.pointerCursor, false);
});

test("pressing and releasing the modifier repaints the hovered link", async t => {
   const { link, events } = await createLink(t);

   const painted = startHover(link);
   assert.deepEqual(painted, { underline: false, pointerCursor: false });

   events.keydown({ ctrlKey: true });
   assert.deepEqual(painted, { underline: true, pointerCursor: true });

   events.keyup({});
   assert.deepEqual(painted, { underline: false, pointerCursor: false });

   // a link the pointer has left is no longer repainted
   link.leave();
   events.keydown({ ctrlKey: true });
   assert.deepEqual(painted, { underline: false, pointerCursor: false });
});

test("hovering again consults the modifier rather than the previous hover", async t => {
   const { link, events } = await createLink(t);

   events.keydown({ ctrlKey: true });
   startHover(link);
   link.leave();
   events.keyup({});

   assert.deepEqual(startHover(link), { underline: false, pointerCursor: false });
});

test("the mouse reports a modifier pressed while the document was unfocused", async t => {
   const { link, events } = await createLink(t);

   events.mousemove({ ctrlKey: true });
   assert.equal(link.decorations.underline, true);

   // a release that happens in another window is never delivered as a keyup
   events.blur();
   assert.equal(link.decorations.underline, false);
});

test("a click without the modifier does not open the file", async t => {
   const { link, opened } = await createLink(t);

   link.activate({ button: 0 });
   assert.deepEqual(opened, []);

   link.activate({ button: 0, ctrlKey: true });
   assert.deepEqual(opened, ["/cwd/file.R"]);
});
