/*
 * NewQuartoWebsiteProjectPage.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui.newproject;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.quarto.model.QuartoCommandConstants;

public class NewQuartoBlogProjectPage extends NewQuartoProjectPage
{
   public NewQuartoBlogProjectPage()
   {
      super(QuartoCommandConstants.PROJECT_WEBSITE_BLOG,
           constants_.quartoBlogTitle(),
           constants_.quartoBlogSubTitle(),
           constants_.quartoBlogPageCaption(),
            new ImageResource2x(NewProjectResources.INSTANCE.quartoBlogIcon2x()),
            new ImageResource2x(NewProjectResources.INSTANCE.quartoBlogIconLarge2x()));
   }
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);

}
