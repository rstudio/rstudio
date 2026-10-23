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

import org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;

// Wraps the acesupport GitDiffSplitView: a read-only editor showing a
// document's base version beside the live editor, aligned and highlighted
// from the gutter's diff. Lives as long as the gutter and the container do.
public class GitDiffSplitNative extends JavaScriptObject
{
   protected GitDiffSplitNative() {}

   public static GitDiffSplitNative create(GitDiffGutterNative gutter, Element container)
   {
      return create(gutter, container, constants_);
   }

   // the pane's text comes from the IDE's translations rather than the module
   private static native GitDiffSplitNative create(GitDiffGutterNative gutter,
                                                   Element container,
                                                   EditorsTextConstants constants) /*-{
      var strings = {
         baseEditor: constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitDiffBaseEditor()(),
         noBase: constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitDiffBaseEmpty()()
      };

      var GitDiffSplitView = $wnd.require("rstudio/git_diff_split").GitDiffSplitView;
      return new GitDiffSplitView(gutter, container, strings);
   }-*/;

   // Shows the notice for a document with no staged or committed version
   // (and hides it otherwise, a base that is merely absent included).
   public final native void setUntracked(boolean untracked) /*-{
      this.setUntracked(untracked);
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

   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
