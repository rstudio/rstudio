/*
 * ChunkToolbar.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

public class ChunkToolbar extends Composite
{
   public ChunkToolbar(ChunkIconsManager manager, AceEditor editor,
         boolean isSetupChunk, boolean isDark)
   {
      commands_ = RStudioGinjector.INSTANCE.getCommands();
      manager_ = manager;
      FlowPanel toolbarPanel = new FlowPanel();
      toolbarPanel.addStyleName(ThemeStyles.INSTANCE.inlineChunkToolbar());
    
      Image optionsIcon = createOptionsIcon(isDark, isSetupChunk);
      optionsIcon.getElement().getStyle().setMarginRight(9, Unit.PX);
      toolbarPanel.add(optionsIcon);

      // Note that 'run current chunk' currently only operates within Rmd
      if (editor.getSession().getMode().getId().equals("mode/rmarkdown"))
      {
         if (!isSetupChunk)
         {
            Image runPreviousIcon = createRunPreviousIcon(isDark);
            runPreviousIcon.getElement().getStyle().setMarginRight(8, Unit.PX);
            toolbarPanel.add(runPreviousIcon);
         }

         Image runIcon = createRunIcon();
         toolbarPanel.add(runIcon);
      }
      initWidget(toolbarPanel);
   }

   private Image createRunIcon()
   {
      Image icon = new Image(ThemeResources.INSTANCE.runChunk());
      icon.addStyleName(ThemeStyles.INSTANCE.highlightIcon());
      icon.setTitle(commands_.executeCurrentChunk().getTooltip());
      bindNativeClickToExecuteChunk(manager_, icon.getElement());
      return icon;
   }
   
   private Image createRunPreviousIcon(boolean dark)
   {
      Image icon = new Image(dark ? 
            ThemeResources.INSTANCE.runPreviousChunksDark() :
            ThemeResources.INSTANCE.runPreviousChunksLight());
      icon.addStyleName(ThemeStyles.INSTANCE.highlightIcon());
           
      icon.setTitle(commands_.executePreviousChunks().getTooltip());
      bindNativeClickToExecutePreviousChunks(manager_, icon.getElement());
      return icon;
   }
   
   private Image createOptionsIcon(boolean dark, boolean setupChunk)
   {
      Image icon = new Image(dark ? 
            ThemeResources.INSTANCE.chunkOptionsDark() :
            ThemeResources.INSTANCE.chunkOptionsLight());
      icon.addStyleName(ThemeStyles.INSTANCE.highlightIcon());
      
      if (setupChunk)
         icon.addStyleName(ChunkIconsManager.RES.styles().setupChunk());
         
      icon.setTitle("Modify chunk options");
      bindNativeClickToOpenOptions(manager_, icon.getElement());
      return icon;
   }
   
   private static final native void bindNativeClickToExecutePreviousChunks(
                                   ChunkIconsManager manager, Element element) 
   /*-{
      element.addEventListener("click", $entry(function(evt) {
         manager.@org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkIconsManager::fireExecutePreviousChunksEvent(Ljava/lang/Object;)(evt);
      }));
   }-*/;

   
   private static final native void bindNativeClickToExecuteChunk(ChunkIconsManager manager,
                                                                  Element element) 
   /*-{
      element.addEventListener("click", $entry(function(evt) {
         manager.@org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkIconsManager::fireExecuteChunkEvent(Ljava/lang/Object;)(evt);
      }));
   }-*/;
   
   private static final native void bindNativeClickToOpenOptions(ChunkIconsManager manager,
                                                                 Element element) 
   /*-{
      element.addEventListener("click", $entry(function(evt) {
         manager.@org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkIconsManager::fireDisplayChunkOptionsEvent(Ljava/lang/Object;)(evt);
      }));
   }-*/;
   
   private final Commands commands_;
   private final ChunkIconsManager manager_;
}
