/*
 * RSConnectAccountEntry.java
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class RSConnectAccountEntry extends Composite
{
   public RSConnectAccountEntry()
   {
      this(null);
   }
   
   public RSConnectAccountEntry(RSConnectAccount account)
   {
      panel_ = new HorizontalPanel();
      initWidget(panel_);
      if (account != null)
         setAccount(account);
   }
   
   /**
    * Shows the given account in the widget, or clears the widget if given a null account.
    * 
    * @param account The account to show, or null to clear the widget.
    */
   public void setAccount(RSConnectAccount account)
   {
      panel_.clear();
      account_ = account;
      
      if (account == null)
         return;
      
      DecorativeImage icon = new DecorativeImage(account.isCloudAccount() ? 
            new ImageResource2x(RSConnectResources.INSTANCE.cloudAccountIconSmall2x()) : 
            new ImageResource2x(RSConnectResources.INSTANCE.localAccountIconSmall2x()));
      icon.getElement().getStyle().setMarginRight(2, Unit.PX);
      panel_.add(icon);

      Label nameLabel = new Label(account.getName() + ":");
      nameLabel.getElement().getStyle().setCursor(Cursor.POINTER);
      nameLabel.getElement().getStyle().setFontWeight(FontWeight.BOLD);
      nameLabel.getElement().getStyle().setMarginRight(4, Unit.PX);
      panel_.add(nameLabel);

      Label serverLabel = new Label(account.getServer());
      serverLabel.getElement().getStyle().setCursor(Cursor.POINTER);
      panel_.add(serverLabel);
   }
   
   public RSConnectAccount getAccount()
   {
      return account_;
   }
   
   private RSConnectAccount account_;
   private final HorizontalPanel panel_;
}

