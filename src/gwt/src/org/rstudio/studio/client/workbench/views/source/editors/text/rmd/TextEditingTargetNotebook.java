/*
 * TextEditingTargetNotebook.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdChunkOutputFinishedEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputFrame;
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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.inject.Inject;

public class TextEditingTargetNotebook 
               implements EditorThemeStyleChangedEvent.Handler,
                          RmdChunkOutputEvent.Handler,
                          RmdChunkOutputFinishedEvent.Handler
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
      rmdHelper_ = rmdHelper;
      RStudioGinjector.INSTANCE.injectMembers(this);
      
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
                        elementForChunkOutput(chunkOutput), 
                        chunkOutput);
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
         RMarkdownServerOperations server)
   {
      events_ = events;
      uiPrefs_ = uiPrefs;
      server_ = server;
      
      events_.addHandler(RmdChunkOutputEvent.TYPE, this);
      events_.addHandler(RmdChunkOutputFinishedEvent.TYPE, this);
   }
   
   public void executeChunk(Scope chunk, String code)
   {
      // maximize the source window if it's paired with the console
      maximizeSourcePaneIfNecessary();
      
      // get the row that ends the chunk
      int row = chunk.getEnd().getRow();

      ChunkDefinition chunkOutput;
      
      // if there is an existing widget just modify it in place
      LineWidget existingWidget = docDisplay_.getLineWidgetForRow(row);
      if (existingWidget != null && 
          existingWidget.getType().equals(ChunkDefinition.LINE_WIDGET_TYPE))
      {
         chunkOutput = existingWidget.getData();
      }
      // otherwise create a new one
      else
      {
         chunkOutput = ChunkDefinition.create(row, 1, true, 
               StringUtil.makeRandomId(12));
        
         LineWidget widget = LineWidget.create(
                               ChunkDefinition.LINE_WIDGET_TYPE,
                               row, 
                               elementForChunkOutput(chunkOutput), 
                               chunkOutput);
         widget.setFixedWidth(true);
         docDisplay_.addLineWidget(widget);
      }

      rmdHelper_.executeInlineChunk(docUpdateSentinel_.getPath(), 
            docUpdateSentinel_.getId(), chunkOutput.getChunkId(), "", code);
   }
   
   @Override
   public void onEditorThemeStyleChanged(EditorThemeStyleChangedEvent event)
   {
      // update cached style 
      editorStyle_ = event.getStyle();
      
      // update existing widgets
      JsArray<LineWidget> lineWidgets = docDisplay_.getLineWidgets();
      for (int i=0; i<lineWidgets.length(); i++)
      {
         LineWidget lineWidget = lineWidgets.get(i);
         if (lineWidget.getType().equals(ChunkDefinition.LINE_WIDGET_TYPE))
            setChunkOutputStyle(lineWidget.getElement());
      }
   }
   
   @Override
   public void onRmdChunkOutput(RmdChunkOutputEvent event)
   {
      // ignore if not targeted at this document
      if (event.getOutput().getDocId() != docUpdateSentinel_.getId())
         return;

      // find the line widget to update
      JsArray<LineWidget> widgets = docDisplay_.getLineWidgets();
      for (int i = 0; i < widgets.length(); i++)
      {
         final LineWidget widget = widgets.get(i);
         ChunkDefinition output = widget.getData();
         if (event.getOutput().getChunkId() == output.getChunkId())
         {
            ChunkOutputFrame frame = new ChunkOutputFrame(
                  event.getOutput().getUrl(), 
                  new ChunkOutputFrame.Host()
                  {
                     @Override
                     public void onOutputLoaded(int height, int width)
                     {
                        Debug.devlog("frame loaded: " + height);
                        IFrameElementEx frame = 
                              widget.getElement().getFirstChildElement().cast();
                        frame.getStyle().setHeight(Math.min(
                              MAX_CHUNK_HEIGHT, height), Unit.PX);
                        docDisplay_.onLineWidgetChanged(widget);
                     }
                  });
            frame.getElement().getStyle().setWidth(100, Unit.PCT);
            widget.getElement().removeAllChildren();
            widget.getElement().appendChild(frame.getElement());
            
            // TODO: this call causes the element to be measured to determine
            // the number of screen rows it consumes, but the element may not 
            // yet be at its final height if the HTML isn't done rendering
            break;
         }
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
   
   
   private DivElement elementForChunkOutput(ChunkDefinition chunkOutput)
   {
      DivElement div = Document.get().createDivElement();
      div.addClassName(ThemeStyles.INSTANCE.selectableText());
      setChunkOutputStyle(div);
      div.getStyle().setOpacity(1.0);
      return div;
   }
   
   private void setChunkOutputStyle(Element div)
   {
      if (editorStyle_ != null)
      {
         div.getStyle().setBackgroundColor(editorStyle_.getBackgroundColor());
      }
   }
   
   private JsArray<ChunkDefinition> initialChunkDefs_;
   
   private final TextEditingTargetRMarkdownHelper rmdHelper_;
   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel docUpdateSentinel_;

   private RMarkdownServerOperations server_;
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
   
   private final static int MAX_CHUNK_HEIGHT = 500;
}
