/*
 * TextEditingTargetChunks.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.assist.RChunkHeaderParser;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.ScopeTreeReadyEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextCodeUi;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.inject.Inject;

public class TextEditingTargetChunks
             implements PinnedLineWidget.Host,
                        ScopeTreeReadyEvent.Handler,
                        EditorModeChangedEvent.Handler,
                        DocumentChangedEvent.Handler
{
   public TextEditingTargetChunks(TextEditingTarget target)
   {
      target_ = target;
      toolbars_ = new ArrayList<ChunkContextCodeUi>();
      modifiedRanges_ = new ArrayList<Range>();
      renderPass_ = 0;
      
      target.getDocDisplay().addScopeTreeReadyHandler(this);
      target.getDocDisplay().addEditorModeChangedHandler(this);
      target.getDocDisplay().addDocumentChangedHandler(this);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   // Public methods ----------------------------------------------------------
   
   @Override
   public void onScopeTreeReady(ScopeTreeReadyEvent event)
   {
      if (target_.getDocDisplay().getModeId() == "mode/rmarkdown")
         syncWidgets(false);
   }

   @Override
   public void onLineWidgetAdded(LineWidget widget)
   {
      // no action necessary; this just lets us know that a chunk toolbar has
      // been attached to the DOM
   }

   @Override
   public void onLineWidgetRemoved(LineWidget widget)
   {
      // remove the widget from our internal list
      for (ChunkContextCodeUi toolbar: toolbars_)
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
         syncWidgets(true);
   }
   
   @Override
   public void onDocumentChanged(DocumentChangedEvent event)
   {
      modifiedRanges_.add(event.getEvent().getRange());
   }
   
   public void setChunkState(int preambleRow, int state)
   {
      for (ChunkContextCodeUi toolbar: toolbars_)
      {
         if (toolbar.getPreambleRow() == preambleRow)
         {
            toolbar.setState(state);
         }
      }
   }
   
   // Private methods ---------------------------------------------------------
   
   @Inject
   private void initialize(UserPrefs prefs, UserState state)
   {
      prefs_ = prefs;
      state_ = state;
      dark_ = state.theme().getValue().getIsDark();
      
      state_.theme().addValueChangeHandler(new ValueChangeHandler<UserStateAccessor.Theme>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<UserStateAccessor.Theme> theme)
         {
            // recompute dark state
            boolean isDark = theme.getValue().getIsDark();
            
            // redraw all the toolbars if necessary
            if (isDark != dark_)
            {
               dark_ = isDark;

               // detach all widgets...
               removeAllToolbars();

               // .. and rebuild them
               syncWidgets(true);
            }
         }
      });
      
      prefs_.showInlineToolbarForRCodeChunks().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            boolean showToolbars = event.getValue();
            
            if (showToolbars)
               syncWidgets(true);
            else
               removeAllToolbars();
         }
      });
   }
   
   private void removeAllToolbars()
   {
      for (ChunkContextCodeUi toolbar: toolbars_)
         toolbar.detach();
      toolbars_.clear();
   }
   
   private void syncWidgets(boolean forceSyncAll)
   {
      // bail early if we don't want to render inline toolbars
      boolean showInlineToolbars = prefs_.showInlineToolbarForRCodeChunks().getValue();
      if (!showInlineToolbars)
         return;
      
      if (forceSyncAll)
      {
         syncAllWidgets();
      }
      else
      {
         syncModifiedWidgets();
      }
   }
   
   private void syncAllWidgets()
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
      for (ChunkContextCodeUi toolbar: toolbars_)
      {
         if (toolbar.getRenderPass() != renderPass_)
         {
            toolbar.detach();
            toolbars_.remove(toolbar);
         }
      }
   }
   
   private void syncModifiedWidgets()
   {
      Set<Scope> modifiedScopes = new HashSet<Scope>();
      
      for (Range range : modifiedRanges_)
      {
         Position startPos = range.getStart();
         Position endPos = range.getEnd();
         
         for (int row = startPos.getRow();
              row <= endPos.getRow();
              row++)
         {
            Position chunkPos = Position.create(row, 0);
            Scope scope = target_.getDocDisplay().getChunkAtPosition(chunkPos);
            ChunkContextCodeUi toolbar = getToolbarForRow(row);
            
            if (toolbar != null && scope == null)
            {
               // we have a toolbar associated with this row, but there
               // is no longer any chunk associated with this row --
               // remove the toolbar
               toolbar.detach();
               toolbars_.remove(toolbar);
            }
            else if (scope != null)
            {
               // there's a chunk associated with this row;
               // ensure an existing toolbar is re-sync'ed if needed
               modifiedScopes.add(scope);
            }
         }
      }
      
      modifiedRanges_.clear();
      
      for (Scope scope : modifiedScopes)
         syncChunkToolbar(scope);
   }
   
   private ChunkContextCodeUi getToolbarForRow(int row)
   {
      for (ChunkContextCodeUi toolbar : toolbars_)
      {
         if (toolbar.getPreambleRow() == row)
         {
            return toolbar;
         }
      }
      
      return null;
   }
   
   private void syncChunkToolbar(Scope chunk)
   {
      // see if we've already drawn a toolbar for this chunk; if so, just
      // update it
      for (ChunkContextCodeUi toolbar: toolbars_)
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
         ChunkContextCodeUi ui = new ChunkContextCodeUi(target_, dark_,
               chunk, this, renderPass_);
         toolbars_.add(ui);
      }
   }
   
   private boolean isRunnableChunk(int row)
   {
      // extract chunk header
      String header = target_.getDocDisplay().getLine(row);
      
      // parse contents
      Map<String, String> options = RChunkHeaderParser.parse(header);
      
      // check runnable engine
      String engine = StringUtil.stringValue(options.get("engine"));
      return isExecutableKnitrEngine(engine);
   }
   
   private boolean isExecutableKnitrEngine(String engine)
   {
      if (target_.getDocDisplay().showChunkOutputInline())
      {
         // treat all chunks as executable in notebook mode
         return true;
      }
      else
      {
         // when executing chunks in the R console, only R and Python chunks are
         // executable
         return engine.equalsIgnoreCase("r") ||
                engine.equalsIgnoreCase("python");
      }
   }
   
   private final TextEditingTarget target_;
   private final ArrayList<ChunkContextCodeUi> toolbars_;
   private final List<Range> modifiedRanges_;
   
   private boolean dark_;
   
   private UserPrefs prefs_;
   private UserState state_;

   // renderPass_ need only be unique from one pass through the scope tree to
   // the next; we wrap it at 255 to avoid the possibility of overflow
   private int renderPass_;
   private final static int RENDER_PASS_MOD = 255;
}
