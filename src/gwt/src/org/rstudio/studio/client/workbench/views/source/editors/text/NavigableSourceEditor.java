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


// TODO: implement Forward

// TODO: for tab switching you get trapped in back cycle

// TODO: consider making history part of client state

// TODO: enable code navigation even when not in a project 
//          - for sure go to defintion
//          - what about search

// TODO: consider file search also finding other files in the project

// TODO: joe on whether we should save nav point for navigating to 

// TODO: see whether we can get commands to active even when src closed

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
