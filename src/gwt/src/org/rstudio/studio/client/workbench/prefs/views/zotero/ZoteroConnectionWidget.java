/*
 * ZoteroConnectionWidget.java
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
package org.rstudio.studio.client.workbench.prefs.views.zotero;

import java.util.ArrayList;

import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;
import org.rstudio.studio.client.workbench.prefs.views.PreferencesDialogResources;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class ZoteroConnectionWidget extends Composite
{
   public ZoteroConnectionWidget(PreferencesDialogResources res, boolean includeHelp)
   {
   
      HorizontalPanel panel = new HorizontalPanel();
      
      ArrayList<String> options = new ArrayList<String>();
      options.add("(None)");
      if (!webOnly())
         options.add("Local");
      options.add("Web");
      
      ArrayList<String> values = new ArrayList<String>();
      values.add(UserStateAccessor.ZOTERO_CONNECTION_TYPE_NONE);
      if (!webOnly())
         values.add(UserStateAccessor.ZOTERO_CONNECTION_TYPE_LOCAL);
      values.add(UserStateAccessor.ZOTERO_CONNECTION_TYPE_WEB);

      zoteroConnection_ = new SelectWidget(
            "Zotero Library:",
            options.toArray(new String[] {}),
            values.toArray(new String[] {}),
            false,
            true,
            false
         );
      zoteroConnection_.addStyleName(ZoteroResources.INSTANCE.styles().connection());
      zoteroConnection_.getElement().getStyle().setMarginBottom(0, Unit.PX);
      panel.add(zoteroConnection_);
      
      if (includeHelp)
      {
         HelpLink zoteroHelp = new HelpLink("Using Zotero", "visual_markdown_editing-zotero", false);
         zoteroHelp.addStyleName(res.styles().selectWidgetHelp());
         panel.add(zoteroHelp);
      }
      
      initWidget(panel);
   
   }
   
   public void setType(String type)
   {
      zoteroConnection_.setValue(type);
   }

   public String getType()
   {
      return zoteroConnection_.getValue();     
   }
   
   public HandlerRegistration addChangeHandler(ChangeHandler handler)
   {
      return zoteroConnection_.addChangeHandler(handler);
   }
  
   private boolean webOnly()
   {
      return !Desktop.isDesktop() && !SuperDevMode.isActive();
   }
   
   private final SelectWidget zoteroConnection_;


}
