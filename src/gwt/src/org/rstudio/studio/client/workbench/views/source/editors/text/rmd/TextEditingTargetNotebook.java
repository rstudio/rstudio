/*
 * TextEditingTargetNotebook.java
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

import java.util.HashMap;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.events.SendToChunkConsoleEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.LineWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeStyleChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.MaximizeSourceWindowEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class TextEditingTargetNotebook 
               implements EditorThemeStyleChangedEvent.Handler,
                          RmdChunkOutputEvent.Handler,
                          RmdChunkOutputFinishedEvent.Handler,
                          SendToChunkConsoleEvent.Handler
{
   public TextEditingTargetNotebook(final TextEditingTarget editingTarget,
                                    TextEditingTargetRMarkdownHelper rmdHelper,
                                    DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel,
                                    SourceDocument document)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;  
      initialChunkDefs_ = document.getChunkDefs();
      outputWidgets_ = new HashMap<String, ChunkOutputWidget>();
      lineWidgets_ = new HashMap<String, LineWidget>();
      rmdHelper_ = rmdHelper;
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // initialize the display's default output mode 
      String outputType = 
            document.getProperties().getAsString(CHUNK_OUTPUT_TYPE);
      if (!outputType.isEmpty() && outputType != "undefined")
      {
         // if the document property is set, apply it directly
         docDisplay_.setShowChunkOutputInline(
               outputType == CHUNK_OUTPUT_INLINE);
      }
      else
      {
         // otherwise, use the global preference to set the value
         docDisplay_.setShowChunkOutputInline(
            RStudioGinjector.INSTANCE.getUIPrefs()
                                     .showRmdChunkOutputInline().getValue());
      }
      
      // listen for future changes to the preference and sync accordingly
      docUpdateSentinel_.addPropertyValueChangeHandler(CHUNK_OUTPUT_TYPE, 
            new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            changeOutputMode(event.getValue());
         }
      });
      
      // single shot rendering of chunk output line widgets
      // (we wait until after the first render to ensure that
      // ace places the line widgets correctly)
      docDisplay_.addRenderFinishedHandler(new RenderFinishedEvent.Handler()
      { 
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            if (initialChunkDefs_ != null)
            {
               for (int i = 0; i<initialChunkDefs_.length(); i++)
               {
                  ChunkDefinition chunkOutput = initialChunkDefs_.get(i);
                  LineWidget widget = LineWidget.create(
                        ChunkDefinition.LINE_WIDGET_TYPE,
                        chunkOutput.getRow(), 
                        elementForChunkDef(chunkOutput), 
                        chunkOutput);
                  lineWidgets_.put(chunkOutput.getChunkId(), widget);
                  widget.setFixedWidth(true);
                  docDisplay_.addLineWidget(widget);
               }
               initialChunkDefs_ = null;
               
               // sync to editor style changes
               editingTarget.addEditorThemeStyleChangedHandler(
                                             TextEditingTargetNotebook.this);

               // load initial chunk output from server
               loadInitialChunkOutput();
            }
         }
      });
   }
   
   @Inject
   public void initialize(EventBus events, UIPrefs uiPrefs,
         RMarkdownServerOperations server,
         ConsoleServerOperations console)
   {
      events_ = events;
      uiPrefs_ = uiPrefs;
      server_ = server;
      console_ = console;
      
      events_.addHandler(RmdChunkOutputEvent.TYPE, this);
      events_.addHandler(RmdChunkOutputFinishedEvent.TYPE, this);
      events_.addHandler(SendToChunkConsoleEvent.TYPE, this);
   }
   
   public void executeChunk(Scope chunk, String code)
   {
      // maximize the source window if it's paired with the console
      maximizeSourcePaneIfNecessary();
      
      // get the row that ends the chunk
      int row = chunk.getEnd().getRow();

      // find or create a matching chunk definition 
      final ChunkDefinition chunkDef = getChunkDefAtRow(row);

      // let the chunk widget know it's started executing
      outputWidgets_.get(chunkDef.getChunkId()).setChunkExecuting();

      rmdHelper_.executeInlineChunk(docUpdateSentinel_.getPath(), 
            docUpdateSentinel_.getId(), chunkDef.getChunkId(), "", code,
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onError(ServerError error)
               {
                  outputWidgets_.get(chunkDef.getChunkId())
                                .showServerError(error);
               }
            });
   }
   
   @Override
   public void onEditorThemeStyleChanged(EditorThemeStyleChangedEvent event)
   {
      // update cached style 
      editorStyle_ = event.getStyle();
      ChunkOutputWidget.cacheEditorStyle(event.getEditorContent(),
            editorStyle_);
      
      for (ChunkOutputWidget widget: outputWidgets_.values())
      {
         widget.applyCachedEditorStyle();
      }
   }
   
   @Override
   public void onSendToChunkConsole(final SendToChunkConsoleEvent event)
   {
      // not for our doc
      if (event.getDocId() != docUpdateSentinel_.getId())
         return;
      
      // create or update the chunk at the given row
      final ChunkDefinition chunkDef = getChunkDefAtRow(event.getRow());
      
      // have the server start recording output from this chunk
      server_.setChunkConsole(docUpdateSentinel_.getId(), 
            chunkDef.getChunkId(), new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            // execute the input
            console_.consoleInput(event.getCode(), chunkDef.getChunkId(), 
                  new VoidServerRequestCallback());
         }
         @Override
         public void onError(ServerError error)
         {
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Chunk Execution Error", error.getMessage());
            
         }
      });
      
      outputWidgets_.get(chunkDef.getChunkId()).showConsoleCode(
            event.getCode());
   }
   
   @Override
   public void onRmdChunkOutput(RmdChunkOutputEvent event)
   {
      // ignore if not targeted at this document
      if (event.getOutput().getDocId() != docUpdateSentinel_.getId())
         return;
      
      // if nothing at all was returned, this means the chunk doesn't exist on
      // the server, so clean it up here.
      if (event.getOutput().isEmpty())
      {
         removeChunk(event.getOutput().getChunkId());
         return;
      }

      // show output in matching chunk
      String chunkId = event.getOutput().getChunkId();
      if (outputWidgets_.containsKey(chunkId))
      {
         outputWidgets_.get(chunkId).showChunkOutput(event.getOutput());
      }
   }

   @Override
   public void onRmdChunkOutputFinished(RmdChunkOutputFinishedEvent event)
   {
      if (event.getData().getRequestId() == Integer.toHexString(requestId_)) 
      {
         state_ = STATE_INITIALIZED;
      }
   }

   private void loadInitialChunkOutput()
   {
      if (state_ != STATE_NONE)
         return;
      
      state_ = STATE_INITIALIZING;
      requestId_ = nextRequestId_++;
      server_.refreshChunkOutput(
            docUpdateSentinel_.getPath(),
            docUpdateSentinel_.getId(), 
            Integer.toHexString(requestId_), 
            new VoidServerRequestCallback());
   }
     
   private void maximizeSourcePaneIfNecessary()
   {
      if (SourceWindowManager.isMainSourceWindow())
      {
         // see if the Source and Console are paired
         PaneConfig paneConfig = uiPrefs_.paneConfig().getValue();
         if (hasSourceAndConsolePaired(paneConfig.getPanes()))
         {
            events_.fireEvent(new MaximizeSourceWindowEvent());
         }  
      }
   }
   
   private boolean hasSourceAndConsolePaired(JsArrayString panes)
   {
      // default config
      if (panes == null)
         return true;
      
      // if there aren't 4 panes this is a configuration we
      // don't recognize
      if (panes.length() != 4)
         return false;
      
      // check for paired config
      return hasSourceAndConsolePaired(panes.get(0), panes.get(1)) ||
             hasSourceAndConsolePaired(panes.get(2), panes.get(3));
   }
   
   private boolean hasSourceAndConsolePaired(String pane1, String pane2)
   {
      return (pane1.equals(PaneConfig.SOURCE) &&
              pane2.equals(PaneConfig.CONSOLE))
                 ||
             (pane1.equals(PaneConfig.CONSOLE) &&
              pane2.equals(PaneConfig.SOURCE));
   }
   
   
   private Element elementForChunkDef(final ChunkDefinition def)
   {
      ChunkOutputWidget widget;
      final String chunkId = def.getChunkId();
      if (outputWidgets_.containsKey(chunkId))
      {
         widget = outputWidgets_.get(chunkId);
      }
      else
      {
         widget = new ChunkOutputWidget(chunkId, new CommandWithArg<Integer>()
         {
            @Override
            public void execute(Integer arg)
            {
               if (!outputWidgets_.containsKey(chunkId))
                  return;
               outputWidgets_.get(chunkId).getElement().getStyle().setHeight(
                     Math.max(MIN_CHUNK_HEIGHT, 
                          Math.min(arg.intValue(), MAX_CHUNK_HEIGHT)), Unit.PX);
               if (!lineWidgets_.containsKey(chunkId))
                  return;
               docDisplay_.onLineWidgetChanged(lineWidgets_.get(chunkId));
            }
         },
         new Command()
         {
            @Override
            public void execute()
            {
               removeChunk(chunkId);
            }
         });
         widget.getElement().addClassName(ThemeStyles.INSTANCE.selectableText());
         widget.getElement().getStyle().setHeight(MIN_CHUNK_HEIGHT, Unit.PX);
         outputWidgets_.put(def.getChunkId(), widget);
      }
      
      return widget.getElement();
   }
   
   private ChunkDefinition getChunkDefAtRow(int row)
   {
      ChunkDefinition chunkDef;
      
      // if there is an existing widget just modify it in place
      LineWidget widget = docDisplay_.getLineWidgetForRow(row);
      if (widget != null && 
          widget.getType().equals(ChunkDefinition.LINE_WIDGET_TYPE))
      {
         chunkDef = widget.getData();
      }
      // otherwise create a new one
      else
      {
         chunkDef = ChunkDefinition.create(row, 1, true, 
               "c" + StringUtil.makeRandomId(12));
        
         widget = LineWidget.create(
                               ChunkDefinition.LINE_WIDGET_TYPE,
                               row, 
                               elementForChunkDef(chunkDef), 
                               chunkDef);
         widget.setFixedWidth(true);
         docDisplay_.addLineWidget(widget);
         lineWidgets_.put(chunkDef.getChunkId(), widget);
      }
      return chunkDef;
   }
   
   private void removeChunk(String chunkId)
   {
      LineWidget widget = lineWidgets_.get(chunkId);
      if (widget == null)
         return;
      
      // remove the widget from the document
      docDisplay_.removeLineWidget(widget);
      
      // remove it from our internal cache
      lineWidgets_.remove(chunkId);
      outputWidgets_.remove(chunkId);
   }
   
   private void removeAllChunks()
   {
      docDisplay_.removeAllLineWidgets();
      lineWidgets_.clear();
      outputWidgets_.clear();
   }
   
   private void changeOutputMode(String mode)
   {
      docDisplay_.setShowChunkOutputInline(mode == CHUNK_OUTPUT_INLINE);

      // if we don't have any inline output, we're done
      if (lineWidgets_.size() == 0 || mode != CHUNK_OUTPUT_CONSOLE)
         return;
      
      // if we do have inline output, offer to clean it up
      RStudioGinjector.INSTANCE.getGlobalDisplay().showYesNoMessage(
            GlobalDisplay.MSG_QUESTION, 
            "Remove Inline Chunk Output", 
            "Do you want to clear all the existing chunk output from your " + 
            "notebook?", false, 
            new Operation()
            {
               @Override
               public void execute()
               {
                  removeAllChunks();
               }
            }, 
            new Operation()
            {
               @Override
               public void execute()
               {
                  // no action necessary
               }
            }, 
            null, 
            "Remove Output", 
            "Keep Output", 
            false);
   }
   
   private JsArray<ChunkDefinition> initialChunkDefs_;
   private HashMap<String, ChunkOutputWidget> outputWidgets_;
   private HashMap<String, LineWidget> lineWidgets_;
   
   private final TextEditingTargetRMarkdownHelper rmdHelper_;
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;

   private RMarkdownServerOperations server_;
   private ConsoleServerOperations console_;
   private EventBus events_;
   private UIPrefs uiPrefs_;
   
   private Style editorStyle_;

   private static int nextRequestId_ = 0;
   private int requestId_ = 0;
   
   private int state_ = STATE_NONE;

   // no chunk state
   private final static int STATE_NONE = 0;
   
   // synchronizing chunk state from server
   private final static int STATE_INITIALIZING = 0;
   
   // chunk state synchronized
   private final static int STATE_INITIALIZED = 0;
   
   private final static int MIN_CHUNK_HEIGHT = 75;
   private final static int MAX_CHUNK_HEIGHT = 750;
   
   public final static String CHUNK_OUTPUT_TYPE    = "chunk_output_type";
   public final static String CHUNK_OUTPUT_INLINE  = "inline";
   public final static String CHUNK_OUTPUT_CONSOLE = "console";
}
