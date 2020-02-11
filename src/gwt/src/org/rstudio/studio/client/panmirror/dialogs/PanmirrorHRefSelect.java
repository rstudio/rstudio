/*
 * PanmirrorHRefSelect.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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


package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkTargets;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkType;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.LayoutPanel;


public class PanmirrorHRefSelect extends LayoutPanel
{
   public PanmirrorHRefSelect(PanmirrorLinkTargets targets, 
                              PanmirrorLinkCapabilities capabilities)
   {
    
      
      type_ = new SelectWidget("Link To", 
         new String[] { "URL", "Heading", "ID" }, 
         new String[] { Integer.toString(PanmirrorLinkType.URL),
                        Integer.toString(PanmirrorLinkType.Heading),
                        Integer.toString(PanmirrorLinkType.ID) },
         false
      );
      add(type_);
      setWidgetLeftWidth(type_, 0, Unit.PX, 100, Unit.PX);
      
      
      
      
   }
   
   
   public void setHRef(int type, String href)
   {
      
   }
   
   public int getType()
   {
      return Integer.parseInt(type_.getValue());
   }
   
   public String getHRef()
   {
      return "";
   }
   
   
   private final SelectWidget type_;
   /*
   private final TextBox href_;
   private final SelectWidget headings_;
   private final SelectWidget ids_;
   */
}
