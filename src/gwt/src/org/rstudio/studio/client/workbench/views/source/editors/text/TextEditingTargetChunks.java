/*
 * TextEditingTargetChunks.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.inject.Inject;

public class TextEditingTargetChunks
             implements PinnedLineWidget.Host
{
   public TextEditingTargetChunks(TextEditingTarget target)
   {
      target_ = target;
      toolbars_ = new ArrayList<ChunkContextUi>();
      target.getDocDisplay().addScopeTreeReadyHandler(
            new ScopeTreeReadyEvent.Handler()
      {
         @Override
         public void onScopeTreeReady(ScopeTreeReadyEvent event)
         {
            initializeWidgets();
         }
      });
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   // Public methods ----------------------------------------------------------
   
   @Override
   public void onLineWidgetRemoved(LineWidget widget)
   {
      // remove the widget from our internal list
      for (ChunkContextUi toolbar: toolbars_)
      {
         if (toolbar.getLineWidget() == widget)
         {
            toolbars_.remove(toolbar);
            break;
         }
      }
   }
   

   public void setChunkState(int preambleRow, int state)
   {
      for (ChunkContextUi toolbar: toolbars_)
      {
         if (toolbar.getPreambleRow() == preambleRow)
         {
            toolbar.setState(state);
         }
      }
   }
   
   // Private methods ---------------------------------------------------------
   
   @Inject
   private void initialize(UIPrefs prefs, AceThemes themes)
   {
      themes_ = themes;
      dark_ = themes_.isDark(themes_.getEffectiveThemeName(
            prefs.theme().getValue()));
      prefs.theme().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> theme)
         {
            // recompute dark state
            boolean isDark = themes_.isDark(
                  themes_.getEffectiveThemeName(theme.getValue()));
            
            // redraw all the toolbars if necessary
            if (isDark != dark_)
            {
               dark_ = isDark;
               for (ChunkContextUi toolbar: toolbars_)
                  toolbar.detach();
               toolbars_.clear();
               initializeWidgets();
            }
         }
      });
   }
   
   private void initializeWidgets()
   {
      ScopeList scopes = new ScopeList(target_.getDocDisplay());
      for (Scope scope: scopes)
      {
         if (!scope.isChunk())
            continue;

         syncChunkToolbar(scope);
      }
   }
   
   private void syncChunkToolbar(Scope chunk)
   {
      // see if we've already drawn a toolbar for this chunk
      boolean hasToolbar = false;
      for (ChunkContextUi toolbar: toolbars_)
      {
         if (toolbar.getPreambleRow() == chunk.getPreamble().getRow())
         {
            hasToolbar = true; 
            break;
         }
      }
      if (hasToolbar)
         return;
         
      ChunkContextUi ui = new ChunkContextUi(target_, dark_, chunk, this);
      toolbars_.add(ui);
   }
   
   private final TextEditingTarget target_;
   private final ArrayList<ChunkContextUi> toolbars_;
   
   private boolean dark_;
   private AceThemes themes_;
}
