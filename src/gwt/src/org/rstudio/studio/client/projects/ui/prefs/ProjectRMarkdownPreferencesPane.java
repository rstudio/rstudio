/*
 * ProjectRMarkdownPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ProjectRMarkdownPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectRMarkdownPreferencesPane()
   {
      addHeader("Visual Editor Markdown Writer");
      
      LayoutGrid grid = new LayoutGrid(2, 2);
      grid.addStyleName(RESOURCES.styles().workspaceGrid());
      grid.setCellSpacing(8);

      Label infoLabel = new Label("Use (Default) to inherit the global default setting");
      infoLabel.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().infoLabel());
      grid.setWidget(0, 0, infoLabel);

      // canonical mode
      canonical_ = new YesNoAskDefault(false);
      grid.setWidget(1, 0, new FormLabel("Write canonical visual mode markdown in source mode", canonical_));
      grid.setWidget(1, 1, canonical_);
      
      add(grid);
      
      // help on per-file markdown options
      HelpLink markdownPerFileOptions = new HelpLink(
            "Setting markdown options on a per-file basis",
            "visual_markdown_editing-file-options",
            false // no version info
      );
      add(markdownPerFileOptions);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(ProjectPreferencesDialogResources.INSTANCE.iconRMarkdown2x());
   }

   @Override
   public String getName()
   {
      return "R Markdown";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      
      canonical_.setSelectedIndex(config.getMarkdownCanonical());
   }

  

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();

      config.setMarkdownWrap(RProjectConfig.MARKDOWN_WRAP_DEFAULT);
      config.setMarkdownWrapAtColumn(72);
      config.setMarkdownReferences(RProjectConfig.MARKDOWN_REFERENCES_DEFAULT);
      
      
      config.setMarkdownCanonical(canonical_.getSelectedIndex());
      

      return new RestartRequirement();
   }


   interface Resources extends ClientBundle
   {
      @Source("ProjectRMarkdownPreferencesPane.css")
      Styles styles();
   }

   private static Resources RES = GWT.create(Resources.class);

   public interface Styles extends CssResource
   {
   }

   static
   {
      RES.styles().ensureInjected();
   }

  
   private YesNoAskDefault canonical_;
   



}
