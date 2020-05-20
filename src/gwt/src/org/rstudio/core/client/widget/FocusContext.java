/*
 * FocusContext.java
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.dom.client.Document;


public class FocusContext
{
   public void record()
   {
      try
      {
         originallyActiveElement_ = DomUtils.getActiveElement();
      }
      catch(Exception e)
      {
      }
   }
   
   public void clear()
   {
      originallyActiveElement_ = null;
   }
   
   public void restore()
   { 
      try
      {
         if (originallyActiveElement_ != null
             && !originallyActiveElement_.getTagName().equalsIgnoreCase("body"))
         {
            Document doc = originallyActiveElement_.getOwnerDocument();
            if (doc != null)
            {
               restoreFocus(originallyActiveElement_);
            }
         }
      }
      catch (Exception e)
      {
         // focus() fail if the element is no longer visible. It's
         // easier to just catch this than try to detect it.
   
         // Also originallyActiveElement_.getTagName() can fail with:
         // "Permission denied to access property 'tagName' from a non-chrome context"
         // possibly due to Firefox "anonymous div" issue.
      }
      originallyActiveElement_ = null;
   }
   
   // restore focus to element
   private final native void restoreFocus(com.google.gwt.dom.client.Element element) /*-{
      // check for a custom restoreFocus handler
      if (element.restoreFocus)
         element.restoreFocus();
      else
         element.focus();
   }-*/;
   
   
   
   private com.google.gwt.dom.client.Element originallyActiveElement_ = null;
  
}
