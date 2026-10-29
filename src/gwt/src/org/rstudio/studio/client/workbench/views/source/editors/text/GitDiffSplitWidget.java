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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

// The pane holding the base-version editor of a side-by-side Git diff. The
// editor itself (a GitDiffSplitNative) is created and owned by the
// document's TextEditingTargetGitDiffGutter, which needs the gutter to exist
// first; this widget just gives it a place in the layout, below a header
// naming the version, and relays resizes.
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

      getElement().appendChild(createHeaderElement(
            constants_.gitDiffBeforeLabel(),
            constants_.gitDiffBeforeDescription()));

      editorContainer_ = Document.get().createDivElement();
      Style containerStyle = editorContainer_.getStyle();
      containerStyle.setPosition(Style.Position.ABSOLUTE);
      containerStyle.setTop(HEADER_HEIGHT, Style.Unit.PX);
      containerStyle.setLeft(0, Style.Unit.PX);
      containerStyle.setRight(0, Style.Unit.PX);
      containerStyle.setBottom(0, Style.Unit.PX);
      getElement().appendChild(editorContainer_);
   }

   // The header above the document's side of the comparison, matching the
   // one this pane shows above the base version.
   public static Widget createLiveHeader()
   {
      Widget header = new Header(createHeaderElement(
            constants_.gitDiffAfterLabel(),
            constants_.gitDiffAfterDescription()));
      FontSizer.applyNormalFontSize(header);
      return header;
   }

   // Which side of the comparison a pane shows, in Git's terms followed by
   // plain ones: "Before" (the index) and "After" (the working tree).
   private static DivElement createHeaderElement(String side, String description)
   {
      DivElement header = Document.get().createDivElement();
      header.addClassName(HEADER_CLASS);

      SpanElement sideEl = Document.get().createSpanElement();
      sideEl.addClassName(HEADER_CLASS + "-side");
      sideEl.setInnerText(side);
      header.appendChild(sideEl);

      // read as words apart, not run together, by assistive technology
      header.appendChild(Document.get().createTextNode(" "));

      SpanElement descriptionEl = Document.get().createSpanElement();
      descriptionEl.setInnerText(description);
      header.appendChild(descriptionEl);

      return header;
   }

   private static class Header extends Widget
   {
      Header(Element element)
      {
         setElement(element);
      }
   }

   // Where the base editor goes, below the header.
   public Element getEditorContainer()
   {
      return editorContainer_;
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

   // Moves focus into the base editor; false when the pane has none, which
   // it does not until the gutter has attached, and not once it has gone.
   public boolean focus()
   {
      if (view_ == null)
         return false;

      view_.focus();
      return true;
   }

   // Scrolls the base editor's cursor into view; false when there is no
   // editor yet.
   public boolean ensureCursorVisible()
   {
      if (view_ == null)
         return false;

      view_.ensureCursorVisible();
      return true;
   }

   @Override
   public void onResize()
   {
      if (view_ != null)
         view_.resize();
   }

   private final DivElement editorContainer_;
   private GitDiffSplitNative view_;
   private Command focusHandler_;

   public static final int HEADER_HEIGHT = 22;
   private static final String HEADER_CLASS = "editor_diff_header";
   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
