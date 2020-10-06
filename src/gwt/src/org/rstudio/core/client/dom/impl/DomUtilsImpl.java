/*
 * DomUtilsImpl.java
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
package org.rstudio.core.client.dom.impl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import org.rstudio.core.client.Rectangle;

public interface DomUtilsImpl
{
   void focus(Element element, boolean alwaysDriveSelection);

   void collapseSelection(boolean toStart);

   boolean isSelectionCollapsed();

   boolean isSelectionInElement(Element element);

   boolean selectionExists();

   Rectangle getCursorBounds(Document doc);

   String replaceSelection(Document document, String text);

   String getSelectionText(Document document);

   int[] getSelectionOffsets(Element container);

   void setSelectionOffsets(Element container, int start, int end);

   boolean isSelectionAsynchronous();

   void selectElement(Element el);
}
