/*
 * SourceNavigator.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionHandler;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.event.shared.HandlerRegistration;


//TODO: implement Back and Forward (Cmd+Shift+[, Cmd+Shift+])

//TODO: move Back and Forward menu items?

//TODO: consider making history part of client state


public interface NavigableSourceEditor 
{
   SourcePosition findFunctionPositionFromCursor(String functionName);
   
   void recordCurrentNavigationPosition();
   
   void navigateToPosition(SourcePosition position, 
                           boolean recordCurrentPosition);
   
   void restorePosition(SourcePosition position);
   
   boolean isAtPosition(SourcePosition position);
   
   HandlerRegistration addRecordNavigationPositionHandler(
                              RecordNavigationPositionHandler handler);
}
