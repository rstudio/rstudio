/*
 * loader.js
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
define("rstudio/loader", function(require, exports, module) {

function loadEditor(container) {
    var catalog = require("pilot/plugin_manager").catalog;
	var env = null;
	var loaded = false;
    catalog.registerPlugins(["pilot/index"]).then(function() {
        env = require("pilot/environment").create();
        catalog.startupPlugins({ env: env }).then(function() {
			loaded = true;
        });
    });
	if (!loaded)
		throw new Error("Environment loading was not synchronous");

	var Editor = require("ace/editor").Editor;
	var Renderer = require("ace/virtual_renderer").VirtualRenderer;
	var UndoManager = require("ace/undomanager").UndoManager;

	var TextMode = require("ace/mode/text").Mode;
	var theme = require("theme/default");

	env.editor = new Editor(new Renderer(container, theme));
	var session = env.editor.getSession();
	session.setMode(new TextMode());
	session.setUndoManager(new UndoManager());
	session.setUseSoftTabs(true);
	session.setTabSize(2);

	// We handle these commands ourselves.
	var canon = require("pilot/canon");
	canon.removeCommand("findnext");
	canon.removeCommand("findprevious");
	canon.removeCommand("find");
	canon.removeCommand("replace");
	canon.removeCommand("togglecomment");
	canon.removeCommand("gotoline");
	return env.editor;
}

exports.loadEditor = loadEditor;
});
