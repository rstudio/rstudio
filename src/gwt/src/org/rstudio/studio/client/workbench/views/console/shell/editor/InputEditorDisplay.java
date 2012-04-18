/*
 * InputEditorDisplay.java
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
package org.rstudio.studio.client.workbench.views.console.shell.editor;

import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasText;
import org.rstudio.core.client.Rectangle;

public interface InputEditorDisplay extends HasAllFocusHandlers,
                                            HasClickHandlers,
                                            HasText
{
   boolean hasSelection() ;
   InputEditorSelection getSelection() ;
   void setSelection(InputEditorSelection selection) ;
   String getSelectionValue();
   Rectangle getCursorBounds() ;
   Rectangle getPositionBounds(InputEditorPosition position);
   Rectangle getBounds() ;
   void setFocus(boolean focused) ;
   /**
    * @param value New value
    * @return Original value
    */
   String replaceSelection(String value, boolean collapseSelection) ;
   boolean isSelectionCollapsed() ;
   void clear() ;

   void collapseSelection(boolean collapseToStart);

   InputEditorSelection getStart();
   InputEditorSelection getEnd();

   int getCurrentLineNum();
   int getCurrentLineCount();
}
