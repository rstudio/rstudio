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
import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.r.knitr.RMarkdownChunkHeaderParser;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.inject.Inject;

public class TextEditingTargetChunks
             implements PinnedLineWidget.Host,
                        ScopeTreeReadyEvent.Handler,
                        EditorModeChangedEvent.Handler
{
   public TextEditingTargetChunks(TextEditingTarget target)
   {
      target_ = target;
      toolbars_ = new ArrayList<ChunkContextUi>();
      initialized_ = false;
      lastRow_ = 0;
      renderPass_ = 0;
      
      target.getDocDisplay().addScopeTreeReadyHandler(this);
      target.getDocDisplay().addEditorModeChangedHandler(this);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   // Public methods ----------------------------------------------------------
   
   @Override
   public void onScopeTreeReady(ScopeTreeReadyEvent event)
   {
      if (target_.getDocDisplay().getModeId() == "mode/rmarkdown")
         syncWidgets();
   }

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

   @Override
   public void onEditorModeChanged(EditorModeChangedEvent event)
   {
      // clean up all chunks when moving out of rmarkdown mode
      if (event.getMode() != "mode/rmarkdown")
         removeAllToolbars();
      else 
         syncWidgets();
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

               // detach all widgets...
               removeAllToolbars();

               // .. and rebuild them
               syncWidgets();
            }
         }
      });
   }
   
   private void removeAllToolbars()
   {
      for (ChunkContextUi toolbar: toolbars_)
         toolbar.detach();
      toolbars_.clear();
   }
   
   private void syncWidgets()
   {
      Scope currentScope = target_.getDocDisplay().getCurrentScope();
      if (initialized_ && currentScope != null && 
          lastRow_ == currentScope.getPreamble().getRow())
      {
         // if initialized and in the same scope as last sync, just sync the 
         // current scope
         syncChunkToolbar(currentScope);
      }
      else
      {
         // mark all chunks below as rendered in a new pass
         renderPass_ = (renderPass_ + 1) % RENDER_PASS_MOD;

         // not initialized, or scope changed; do a full sync
         ScopeList scopes = new ScopeList(target_.getDocDisplay());
         for (Scope scope: scopes)
         {
            syncChunkToolbar(scope);
         }
         
         // we should have touched every toolbar--remove those we didn't
         for (ChunkContextUi toolbar: toolbars_)
         {
            if (toolbar.getRenderPass() != renderPass_)
            {
               toolbar.detach();
               toolbars_.remove(toolbar);
            }
         }
      }

      if (currentScope != null)
         lastRow_ = currentScope.getPreamble().getRow();
   }
   
   private void syncChunkToolbar(Scope chunk)
   {
      // see if we've already drawn a toolbar for this chunk; if so, just
      // update it
      for (ChunkContextUi toolbar: toolbars_)
      {
         int preambleRow = chunk.getPreamble().getRow();
         if (toolbar.getPreambleRow() == preambleRow)
         {
            if (chunk.isChunk() && isRunnableChunk(preambleRow))
            {
               // still a runnable chunk, sync the toolbar
               toolbar.syncToChunk();
               toolbar.setRenderPass(renderPass_);
            }
            else
            {
               // if this chunk is no longer runnable, we need to remove it
               toolbar.detach();
               toolbars_.remove(toolbar);
            }
            return;
         }
      }
         
      // if this is a runnable chunk and we got here, it needs a toolbar but
      // doesn't have one
      if (chunk.isChunk() && isRunnableChunk(chunk.getPreamble().getRow()))
      {
         ChunkContextUi ui = new ChunkContextUi(target_, renderPass_, dark_,
               chunk, this);
         toolbars_.add(ui);
      }
   }
   
   private boolean isRunnableChunk(int row)
   {
      // extract chunk header
      String header = target_.getDocDisplay().getLine(row);
      
      // parse contents
      Map<String, String> options = 
            RMarkdownChunkHeaderParser.parse(header);
      
      // check runnable engine
      String engine = StringUtil.stringValue(options.get("engine"));
      return isExecutableKnitrEngine(engine);
   }
   
   private boolean isExecutableKnitrEngine(String engine)
   {
      // TODO: only enable non-R engine work for notebooks for now
      if (!target_.getDocDisplay().showChunkOutputInline())
         return engine.equalsIgnoreCase("r");
      
      return RE_RUNNABLE_ENGINES.indexOf(engine.toLowerCase() + "|") != -1;
   }
   
   private final TextEditingTarget target_;
   private final ArrayList<ChunkContextUi> toolbars_;
   
   private boolean dark_;
   private boolean initialized_;
   private AceThemes themes_;

   private int lastRow_;
   
   // runnable engines within the R Notebook mode
   private static final String RE_RUNNABLE_ENGINES =
         "r|rscript|rcpp|python|ruby|perl|bash|sh|stan|";
   
   // renderPass_ need only be unique from one pass through the scope tree to
   // the next; we wrap it at 255 to avoid the possibility of overflow
   private int renderPass_;
   private final static int RENDER_PASS_MOD = 255;
}
