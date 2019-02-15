
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

// Load R mode
editor.getSession().setMode(new RMode(false, editor.getSession()));
 