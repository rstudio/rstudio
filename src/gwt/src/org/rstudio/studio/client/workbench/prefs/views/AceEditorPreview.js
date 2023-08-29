
// Load required Ace modules
var event = require('ace/lib/event');
var Editor = require('ace/editor').Editor;
var Renderer = require('ace/virtual_renderer').VirtualRenderer;
var container = document.getElementById('editor');
var RMode = require('mode/r').Mode;

// Initialize the Ace editor. We set the basePath config
// only to squelch an error otherwise emitted by Ace.
ace.config.set('basePath', 'ace');
var editor = ace.edit('editor');

// Set up options
editor.setReadOnly(true);
editor.setHighlightActiveLine(false);
editor.renderer.setHScrollBarAlwaysVisible(false);
editor.renderer.setShowGutter(false);
editor.renderer.setDisplayIndentGuides(false);
editor.renderer.setScrollMargin(3, 3, 3, 3);

// Set up line height
editor.container.style.lineHeight = 1.4;
editor.renderer.updateFontSize();

// Load R mode
editor.getSession().setMode(new RMode(false, editor.getSession()));

// Remove Textmate theme, as we'll be applying our own themes for display
var el = document.getElementById("ace-tm");
if (el != null) {
    el.parentNode.removeChild(el);
}
