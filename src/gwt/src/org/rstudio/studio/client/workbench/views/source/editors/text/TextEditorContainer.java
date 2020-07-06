/*
 * TextEditorContainer.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import java.util.ArrayList;

import org.rstudio.core.client.patch.TextChange;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.IsHideableWidget;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.LayoutPanel;

// container that holds a text editor, but which also supports juxtoposing
// additional widgets over the top of the editor

public class TextEditorContainer extends LayoutPanel implements CanFocus
{     
   
   
   public static class Changes
   {
      public Changes(TextChange[] changes, Navigator navigator)
      {
         this.changes = changes;
         this.navigator = navigator;
      }
      
      public final TextChange[] changes;
      public final Navigator navigator;
   }

   public static interface Navigator
   {
      void onNavigate(DocDisplay docDisplay);
   }
   
   public static interface Editor extends IsHideableWidget
   {
      String getCode();
      void setCode(String code);
      void applyChanges(Changes changes, boolean activatingEditor);
   }
   
   public TextEditorContainer(Editor editor)
   {
      editor_ = editor;
      addStyleName("ace_editor_theme");
      addWidget(editor, false);
   }
   
   @Override
   public void focus()
   {
      widgets_.forEach(widget -> {
         if (widget.isVisible()) 
         {
            widget.focus();
            return;
         }
      });
   }
   
   
   public Editor getEditor()
   {
      return editor_;
   }
   
   public boolean isEditorActive()
   {
      return isWidgetActive(editor_);
   }
   
   public boolean isWidgetActive(IsHideableWidget widget)
   {
      if (widget != null)
      {
         int idx = widgets_.indexOf(widget);
         if (idx != -1)
            return widgets_.get(idx).isVisible();
         else
            return false;
      } 
      else
      {
         return false;
      }
   }
   
   public void activateEditor()
   {
      activateWidget(editor_);
   }
   
   public void activateEditor(boolean focus)
   {
      activateWidget(editor_, focus);
   }
   
   public void activateWidget(IsHideableWidget widget)
   {
      activateWidget(widget, false);
   }
  
   // activate a widget
   public void activateWidget(IsHideableWidget widget, boolean focus)
   {
      // add the widget if don't already have it
      if (!widgets_.contains(widget))
         addWidget(widget, true);
      // otherwise just set it visible
      else
         setWidgetVisible(widget.asWidget(), true);
      
      // set others invisible
      widgets_.forEach(w -> {
         if (w != widget)
            setWidgetVisible(w.asWidget(), false);
      });
      
      // force layout
      forceLayout();
      
      // focus if requested
      if (focus)
         widget.focus();
   }
   
   // remove a widget
   public boolean removeWidget(IsHideableWidget widget)
   {
      if (widget != null)
      {
         widgets_.remove(widget);
         remove(widget.asWidget());
         forceLayout();
         return true;
      } 
      else
      {
         return false;
      }
   }
   
   
   // add a widget (not activated by default)
   private void addWidget(IsHideableWidget widget, boolean visible)
   {
      // add editor to container
      add(widget.asWidget());
      
      // have it take up the full container
      setWidgetVisible(widget.asWidget(), visible);
      setWidgetLeftRight(widget.asWidget(), 0, Unit.PX, 0, Unit.PX);
      setWidgetTopBottom(widget.asWidget(), 0, Unit.PX, 0, Unit.PX);
      
      // add to list of editors we are managimg
      widgets_.add(widget);
      
      // layout
      forceLayout();
   }
  
   private final Editor editor_;
   private ArrayList<IsHideableWidget> widgets_ = new ArrayList<IsHideableWidget>();
}
