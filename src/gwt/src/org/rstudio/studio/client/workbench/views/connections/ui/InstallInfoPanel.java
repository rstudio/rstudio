/*
 * InstallInfoPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.workbench.views.connections.model.SparkVersion;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class InstallInfoPanel extends Composite
{
   public InstallInfoPanel()
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.addStyleName(NewSparkConnectionDialog.RES.styles().infoPanel());
      Image infoIcon = new Image(ThemeResources.INSTANCE.infoSmall());
      panel.add(infoIcon);
      infoLabel_ = new Label();
      panel.add(infoLabel_);
   
      
      initWidget(panel);
   }
   
   public void update(SparkVersion sparkVersion,
                      boolean remote)
   {
      infoLabel_.setText(getInfoText(sparkVersion, remote, false));
   }
   
   public static String getInfoText(SparkVersion sparkVersion, 
                                    boolean remote,
                                    boolean isInstall)
   {
      StringBuilder builder = new StringBuilder();
      if (remote)
         builder.append("A local version of Spark ");
      else
         builder.append("Spark ");
      builder.append(sparkVersion.getSparkVersionNumber());
      builder.append(" for ");
      builder.append(sparkVersion.getHadoopVersionLabel());
      if (remote)
         builder.append(" is required to connect to this cluster. ");
      else
         builder.append(" is not currently installed. ");
      
      if (isInstall)
         builder.append("\n\nDo you want to install this version of Spark now?");
      else
         builder.append("You will be prompted to install this version " +
                        "prior to connecting.");
      
      return builder.toString();
   }

   
   
   private final Label infoLabel_;
   
}
