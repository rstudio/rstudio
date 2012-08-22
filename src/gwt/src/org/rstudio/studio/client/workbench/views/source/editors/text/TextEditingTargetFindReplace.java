/*
 * TextEditingTargetFindReplace.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplace;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

public class TextEditingTargetFindReplace
{
   public interface Container
   {
      AceEditor getEditor();
      void insertFindReplace(FindReplaceBar findReplaceBar);
      void removeFindReplace(FindReplaceBar findReplaceBar);
   }
   
   public TextEditingTargetFindReplace(Container container)
   {
      this(container, true);
   }
   
   public TextEditingTargetFindReplace(Container container, boolean showReplace)                                  
   {
      container_ = container;
      showReplace_ = showReplace;
   }
   
   public Widget createFindReplaceButton()
   {
      if (findReplaceBar_ == null)
      {
         findReplaceButton_ = new ToolbarButton(
               FindReplaceBar.getFindIcon(),
               new ClickHandler() {
                  public void onClick(ClickEvent event)
                  {
                     if (findReplaceBar_ == null)
                        showFindReplace(true);
                     else
                        hideFindReplace();
                  }
               });
         String title = showReplace_ ? "Find/Replace" : "Find";
         findReplaceButton_.setTitle(title);
      }
      return findReplaceButton_;
   }
   
   public void showFindReplace(boolean defaultForward)
   {
      if (findReplaceBar_ == null)
      {
         findReplaceBar_ = new FindReplaceBar(showReplace_, defaultForward);
         findReplace_ = new FindReplace(
                               container_.getEditor(),
                               findReplaceBar_,
                               RStudioGinjector.INSTANCE.getGlobalDisplay(),
                               showReplace_);
         container_.insertFindReplace(findReplaceBar_);
         findReplaceBar_.getCloseButton().addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               hideFindReplace();
            }
         });

         findReplaceButton_.setLeftImage(FindReplaceBar.getFindLatchedIcon());
      }
      
      String selection = container_.getEditor().getSelectionValue();
      
      String searchText = null;
      if (selection.length() != 0 && selection.indexOf('\n') == -1)
         searchText = selection;
      findReplaceBar_.activate(searchText, defaultForward);
   }
   
   public boolean isShowing()
   {
      return findReplaceBar_ != null;
   }

   public void hideFindReplace()
   {
      if (findReplaceBar_ != null)
      {
         container_.removeFindReplace(findReplaceBar_);
         findReplace_ = null;
         findReplaceBar_ = null;
         findReplaceButton_.setLeftImage(FindReplaceBar.getFindIcon());
      }
      container_.getEditor().focus();
   }
   
   public void findNext()
   {
      if (findReplace_ != null)
         findReplace_.findNext();
   }
   
   public void findPrevious()
   {
      if (findReplace_ != null)
         findReplace_.findPrevious();
   }
   
   private final Container container_;
   private final boolean showReplace_;
   private FindReplace findReplace_;
   private FindReplaceBar findReplaceBar_;
   private ToolbarButton findReplaceButton_;
}
