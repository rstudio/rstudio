/*
 * XTermInputEditor.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent.Handler;

import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

public class XTermInputEditor implements InputEditorDisplay
{
   public XTermInputEditor(XTermWidget widget)
   {
      widget_ = widget;
   }
   
   @Override
   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getText()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setText(String text)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean hasSelection()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public InputEditorSelection getSelection()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setSelection(InputEditorSelection selection)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public String getSelectionValue()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Rectangle getCursorBounds()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Rectangle getPositionBounds(InputEditorPosition position)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Rectangle getBounds()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setFocus(boolean focused)
   {
      widget_.setFocus(focused);
   }

   @Override
   public boolean isFocused()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String replaceSelection(String value, boolean collapseSelection)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean isSelectionCollapsed()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void clear()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void insertCode(String code)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void collapseSelection(boolean collapseToStart)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public InputEditorSelection getStart()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public InputEditorSelection getEnd()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public int getCurrentLineNum()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public int getCurrentLineCount()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public boolean isCursorAtEnd()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public Position getCursorPosition()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public String getLanguageMode(Position position)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void goToLineStart()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void goToLineEnd()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public HandlerRegistration addPasteHandler(Handler handler)
   {
      // TODO Auto-generated method stub
      return null;
   }

   private XTermWidget widget_;
}
