/*
 * NavigableSourceEditor.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import org.rstudio.studio.client.workbench.views.source.events.RecordNavigationPositionEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.HandlerRegistration;


public interface NavigableSourceEditor
{
   SourcePosition findFunctionPositionFromCursor(String functionName);

   void recordCurrentNavigationPosition();

   void navigateToPosition(SourcePosition position,
                           boolean recordCurrentPosition);

   void navigateToPosition(SourcePosition position,
                           boolean recordCurrentPosition,
                           boolean highlightLine,
                           boolean restoreCursorPosition);

   void restorePosition(SourcePosition position);

   JsArray<ScopeFunction> getAllFunctionScopes();

   boolean isAtSourceRow(SourcePosition position);

   HandlerRegistration addRecordNavigationPositionHandler(
                              RecordNavigationPositionEvent.Handler handler);
}
