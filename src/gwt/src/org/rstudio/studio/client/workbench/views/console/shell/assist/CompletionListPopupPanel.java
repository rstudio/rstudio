/*
 * CompletionListPopupPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HTML;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.ThemedPopupPanel;

public class CompletionListPopupPanel extends ThemedPopupPanel
   implements HasSelectionCommitHandlers<String>
{
   public CompletionListPopupPanel(String[] entries)
   {
      super(true);
      list_ = new CompletionList<String>(entries, 10, false, true);
      setWidget(list_);
   }

   public HandlerRegistration addSelectionCommitHandler(
         SelectionCommitHandler<String> handler)
   {
      return list_.addSelectionCommitHandler(handler);
   }

   public String getSelectedValue()
   {
      if (list_ == null || !list_.isAttached())
         return null ;

      return list_.getSelectedItem() ;
   }

   public boolean selectNext()
   {
      return list_.selectNext() ;
   }

   public boolean selectPrev()
   {
      return list_.selectPrev() ;
   }

   public boolean selectPrevPage()
   {
      return list_.selectPrevPage() ;
   }

   public boolean selectNextPage()
   {
      return list_.selectNextPage() ;
   }

   public boolean selectFirst()
   {
      return list_.selectFirst() ;
   }

   public boolean selectLast()
   {
      return list_.selectLast() ;
   }

   public void setMaxWidth(int pixels)
   {
      list_.setMaxWidth(pixels);
   }

   public void setText(String s)
   {
      HTML html = new HTML();
      html.setText(s);
      setWidget(html);
   }

   private final CompletionList<String> list_;
}
