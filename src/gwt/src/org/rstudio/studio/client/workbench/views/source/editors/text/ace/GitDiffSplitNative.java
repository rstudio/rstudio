/*
 * GitDiffSplitNative.java
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
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;

// Wraps the acesupport GitDiffSplitView: a read-only editor showing a
// document's base version beside the live editor, aligned and highlighted
// from the gutter's diff. Lives as long as the gutter and the container do.
public class GitDiffSplitNative extends JavaScriptObject
{
   protected GitDiffSplitNative() {}

   public static native GitDiffSplitNative create(GitDiffGutterNative gutter,
                                                  Element container) /*-{
      var GitDiffSplitView = $wnd.require("rstudio/git_diff_split").GitDiffSplitView;
      return new GitDiffSplitView(gutter, container);
   }-*/;

   public final native void resize() /*-{
      this.resize();
   }-*/;

   public final native boolean isFocused() /*-{
      return this.isFocused();
   }-*/;

   public final native void focus() /*-{
      this.focus();
   }-*/;

   // Runs the command each time the base editor takes focus.
   public final native void onFocus(Command command) /*-{
      this.onFocus($entry(function() {
         command.@com.google.gwt.user.client.Command::execute()();
      }));
   }-*/;

   public final native void dispose() /*-{
      this.dispose();
   }-*/;
}
