/*
 * SourceNavigation.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import org.rstudio.core.client.FilePosition;

import com.google.gwt.core.client.JavaScriptObject;

public class SourceNavigation extends JavaScriptObject
{
   protected SourceNavigation()
   {
   }

   public static final native SourceNavigation create(
                                                 String document_id,
                                                 FilePosition position) /*-{
      var sourceNavPosition = new Object();
      sourceNavPosition.document_id = document_id ;
      sourceNavPosition.position = position ;
      return sourceNavPosition ;
   }-*/;
   
   public native final String getDocumentId() /*-{
      return this.document_id;
   }-*/;
   
   public native final FilePosition getPosition() /*-{
      return this.position;
   }-*/;
}
