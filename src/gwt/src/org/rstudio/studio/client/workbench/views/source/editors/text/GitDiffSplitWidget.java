/*
 * GitDiffSplitWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.ClassIds;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.GitDiffSplitNative;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

// The pane holding the base-version editor of a side-by-side Git diff. The
// editor itself (a GitDiffSplitNative) is created and owned by the
// document's TextEditingTargetGitDiffGutter, which needs the gutter to exist
// first; this widget just gives it a place in the layout and relays resizes.
public class GitDiffSplitWidget extends Widget implements RequiresResize
{
   public GitDiffSplitWidget()
   {
      setElement(Document.get().createDivElement());
      FontSizer.applyNormalFontSize(this);
      ClassIds.assignClassId(getElement(), ClassIds.EDITOR_DIFF_VIEW);

      Style style = getElement().getStyle();
      style.setPosition(Style.Position.RELATIVE);
      style.setOverflow(Style.Overflow.HIDDEN);
      setSize("100%", "100%");
   }

   public void setView(GitDiffSplitNative view)
   {
      view_ = view;
      if (view_ == null)
         return;

      view_.onFocus(() ->
      {
         if (focusHandler_ != null)
            focusHandler_.execute();
      });

      if (isAttached())
         view_.resize();
   }

   // Runs when the base editor takes focus, like a click into the live
   // editor does; set by the owning widget to make its document current.
   public void setFocusHandler(Command handler)
   {
      focusHandler_ = handler;
   }

   public boolean isFocused()
   {
      return view_ != null && view_.isFocused();
   }

   public void focus()
   {
      if (view_ != null)
         view_.focus();
   }

   @Override
   public void onResize()
   {
      if (view_ != null)
         view_.resize();
   }

   private GitDiffSplitNative view_;
   private Command focusHandler_;
}
