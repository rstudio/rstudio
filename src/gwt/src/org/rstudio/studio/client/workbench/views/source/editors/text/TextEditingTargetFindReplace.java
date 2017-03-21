/*
 * TextEditingTargetFindReplace.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
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
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
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
      
      container_.getEditor().addEditorFocusHandler(new FocusHandler() {
         @Override
         public void onFocus(FocusEvent event)
         {
            if (findReplace_ != null)
               findReplace_.notifyEditorFocused();
         } 
      });
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
      ensureFindReplaceBar(defaultForward);
      
      String selection = container_.getEditor().getSelectionValue();
      boolean multiLineSelection = selection.indexOf('\n') != -1;
      
      String searchText = null;
      if ((selection.length() != 0) && !multiLineSelection)
         searchText = selection;
      
      findReplace_.activate(searchText, defaultForward, multiLineSelection);
   }

   private void ensureFindReplaceBar(boolean defaultForward)
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
         findReplace_.notifyClosing();
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
   
   public void selectAll()
   {
      if (findReplace_ != null)
         findReplace_.selectAll();
   }
   
   public void findPrevious()
   {
      if (findReplace_ != null)
         findReplace_.findPrevious();
   }
   
   public void findFromSelection()
   {
      String selection = container_.getEditor().getSelectionValue();
      boolean multiLineSelection = selection.indexOf('\n') != -1;
      if ((selection.length()) > 0 && !multiLineSelection)
      {
         boolean hasFindReplaceBar = findReplaceBar_ != null;
         if (hasFindReplaceBar)
         {
            findReplace_.activate(selection, true, false);
            findReplace_.findNext();
         }
         else
         {
            ensureFindReplaceBar(true);
            findReplace_.activate(selection, true, false);
         }
      }
   }
   
   public void replaceAndFind()
   {
      if (findReplace_ == null)
      {
         String selection = container_.getEditor().getSelectionValue();
         boolean isMultilineSelection = selection.indexOf('\n') != -1;
         if (!isMultilineSelection)
         {
            ensureFindReplaceBar(true);
            findReplace_.activate(selection, true, false);
         }
      }
      else
      {
         findReplace_.replaceAndFind();
      }
   }
  
   
   private final Container container_;
   private final boolean showReplace_;
   private FindReplace findReplace_;
   private FindReplaceBar findReplaceBar_;
   private ToolbarButton findReplaceButton_;
}
