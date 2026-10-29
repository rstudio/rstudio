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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

// Wraps the acesupport GitDiffGutter, which marks changed lines in the gutter.
// Requires Ace's diff extension to be loaded; see AceEditor.loadDiffExtension().
public class GitDiffGutterNative extends JavaScriptObject
{
   protected GitDiffGutterNative() {}

   public static GitDiffGutterNative create(AceEditorNative editor)
   {
      return create(editor, constants_);
   }

   // the peek's text comes from the IDE's translations rather than the module
   private static native GitDiffGutterNative create(AceEditorNative editor,
                                                    EditorsTextConstants constants) /*-{
      var strings = {
         removedLines: $entry(function(n) {
            return constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitPeekRemovedLines(I)(n);
         }),
         addedLines: $entry(function(n) {
            return constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitPeekAddedLines(I)(n);
         }),
         changedLines: $entry(function(n) {
            return constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitPeekChangedLines(I)(n);
         }),
         changedLinesShort: $entry(function(n) {
            return constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitPeekChangedLinesShort(I)(n);
         }),
         revert: constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitPeekRevert()(),
         close: constants.@org.rstudio.studio.client.workbench.views.source.editors.text.EditorsTextConstants::gitPeekClose()()
      };

      var GitDiffGutter = $wnd.require("rstudio/git_diff_gutter").GitDiffGutter;
      return new GitDiffGutter(editor, strings);
   }-*/;

   // Sets the text to diff against; null clears all markers.
   public final native void setBase(String contents) /*-{
      this.setBase(contents);
   }-*/;

   public final native void setIgnoreWhitespace(boolean ignore) /*-{
      this.setIgnoreWhitespace(ignore);
   }-*/;

   // Installs what the gutter calls to re-fetch the base ahead of a revert.
   // The refresher receives a JS function to call once the base is current;
   // see TextEditingTargetGitDiffGutter.refreshBase().
   public final native void setBaseRefresher(CommandWithArg<JavaScriptObject> refresher) /*-{
      this.setBaseRefresher($entry(function(callback) {
         refresher.@org.rstudio.core.client.CommandWithArg::execute(Ljava/lang/Object;)(callback);
      }));
   }-*/;

   // Navigation wraps around; each returns false when there are no changes.
   public final native boolean goToNextChange() /*-{
      return this.goToNextChange();
   }-*/;

   public final native boolean goToPreviousChange() /*-{
      return this.goToPreviousChange();
   }-*/;

   // Restores the base version of the change under the cursor, once the base
   // has been re-fetched; returns false when the cursor isn't on a change.
   public final native boolean revertChangeAtCursor() /*-{
      return this.revertChangeAtCursor();
   }-*/;

   // Toggles the peek at the change under the cursor; returns false when the
   // cursor isn't on a change.
   public final native boolean showChangeAtCursor() /*-{
      return this.showChangeAtCursor();
   }-*/;

   public final native void dispose() /*-{
      this.dispose();
   }-*/;

   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
