/*
 * InputEditorDisplay.java
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
package org.rstudio.studio.client.workbench.views.console.shell.editor;

import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasText;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

public interface InputEditorDisplay extends HasAllFocusHandlers,
                                            HasClickHandlers,
                                            HasText
{
   boolean hasSelection();
   InputEditorSelection getSelection();
   void setSelection(InputEditorSelection selection);
   String getSelectionValue();
   Rectangle getCursorBounds();
   Rectangle getPositionBounds(InputEditorPosition position);
   Rectangle getBounds();
   void setFocus(boolean focused);
   boolean isFocused();
   /**
    * @param value New value
    * @return Original value
    */
   String replaceSelection(String value, boolean collapseSelection);
   boolean isSelectionCollapsed();
   void clear();
   
   void insertCode(String code);

   void collapseSelection(boolean collapseToStart);

   InputEditorSelection getStart();
   InputEditorSelection getEnd();

   int getCurrentLineNum();
   int getCurrentLineCount();
   
   boolean isCursorAtEnd();
   
   Position getCursorPosition();
   void setCursorPosition(Position position);
   String getLanguageMode(Position position);
   
   void goToLineStart();
   void goToLineEnd();
   
   HandlerRegistration addPasteHandler(PasteEvent.Handler handler);
}
