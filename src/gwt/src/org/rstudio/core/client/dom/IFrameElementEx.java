/*
 * IFrameElementEx.java
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
package org.rstudio.core.client.dom;

import com.google.gwt.dom.client.IFrameElement;

public class IFrameElementEx extends IFrameElement
{
   protected IFrameElementEx()
   {
   }
   
   public final native WindowEx getContentWindow() /*-{
      return this.contentWindow ;
   }-*/;
}
