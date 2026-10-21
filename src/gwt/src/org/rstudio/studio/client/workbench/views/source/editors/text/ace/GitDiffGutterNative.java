/*
 * GitDiffGutterNative.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

// Wraps the acesupport GitDiffGutter, which marks changed lines in the gutter.
// Requires Ace's diff extension to be loaded; see AceEditor.loadDiffExtension().
public class GitDiffGutterNative extends JavaScriptObject
{
   protected GitDiffGutterNative() {}

   public static native GitDiffGutterNative create(AceEditorNative editor) /*-{
      var GitDiffGutter = $wnd.require("rstudio/git_diff_gutter").GitDiffGutter;
      return new GitDiffGutter(editor);
   }-*/;

   // Sets the text to diff against; null clears all markers.
   public final native void setBase(String contents) /*-{
      this.setBase(contents);
   }-*/;

   public final native void dispose() /*-{
      this.dispose();
   }-*/;
}
