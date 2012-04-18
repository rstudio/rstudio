/*
 * VCSHelpLink.java
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
package org.rstudio.studio.client.common.vcs;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;

public class VCSHelpLink extends Composite
{
   public VCSHelpLink()
   {
      HorizontalPanel helpPanel = new HorizontalPanel();
    

      Image helpImage = new Image(ThemeResources.INSTANCE.help());
      helpImage.getElement().getStyle().setMarginRight(4, Unit.PX);
      helpPanel.add(helpImage);
      helpLink_ = new HyperlinkLabel("Using Version Control with RStudio");
      helpLink_.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().openRStudioLink(
                                                      "using_version_control");
         }  
      });
      helpPanel.add(helpLink_);

      initWidget(helpPanel);
   }
   
   public void setCaption(String caption)
   {
      helpLink_.setText(caption);
   }
   
   private HyperlinkLabel helpLink_;
}
