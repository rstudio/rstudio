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

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
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
import com.google.inject.Inject;

public class TextEditingTargetNotebook 
               implements EditorThemeStyleChangedEvent.Handler
{
   public TextEditingTargetNotebook(final TextEditingTarget editingTarget,
                                    DocDisplay docDisplay,
                                    DocUpdateSentinel docUpdateSentinel,
                                    SourceDocument document)
   {
      docDisplay_ = docDisplay;
      docUpdateSentinel_ = docUpdateSentinel;  
      initialChunkOutputs_ = document.getChunkOutput();
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      // single shot rendering of chunk output line widgets
      // (we wait until after the first render to ensure that
      // ace places the line widgets correctly)
      docDisplay_.addRenderFinishedHandler(new RenderFinishedEvent.Handler()
      { 
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            if (initialChunkOutputs_ != null)
            {
               for (int i = 0; i<initialChunkOutputs_.length(); i++)
               {
                  ChunkOutput chunkOutput = initialChunkOutputs_.get(i);
                  LineWidget widget = LineWidget.create(
                        ChunkOutput.LINE_WIDGET_TYPE,
                        chunkOutput.getRow(), 
                        elementForChunkOutput(chunkOutput), 
                        chunkOutput);
                  widget.setFixedWidth(true);
                  docDisplay_.addLineWidget(widget);
               }
               initialChunkOutputs_ = null;
               
               // sync to editor style changes
               editingTarget.addEditorThemeStyleChangedHandler(
                                             TextEditingTargetNotebook.this);
            }
         }
      });
      
      
      
   }
   
   @Inject
   public void initialize(EventBus events, UIPrefs uiPrefs)
   {
      events_ = events;
      uiPrefs_ = uiPrefs;
   }
     
   public void executeChunk(Scope chunk, String code)
   {
      // maximize the source window if it's paired with the console
      maximizeSourcePaneIfNecessary();
      
      // get the row that ends the chunk
      int row = chunk.getEnd().getRow();
      
      // if there is an existing widget just modify it in place
      LineWidget existingWidget = docDisplay_.getLineWidgetForRow(row);
      if (existingWidget != null && 
          existingWidget.getType().equals(ChunkOutput.LINE_WIDGET_TYPE))
      {
         setChunkOutput(existingWidget.getElement());
         docDisplay_.onLineWidgetChanged(existingWidget);
      }
      // otherwise create a new one
      else
      {
         ChunkOutput chunkOutput = ChunkOutput.create(row, 1, true, "ref");
        
         LineWidget widget = LineWidget.create(
                               ChunkOutput.LINE_WIDGET_TYPE,
                               row, 
                               elementForChunkOutput(chunkOutput), 
                               chunkOutput);
         widget.setFixedWidth(true);
         docDisplay_.addLineWidget(widget);
      }
      
      // still execute in console
      events_.fireEvent(new SendToConsoleEvent(code, true, false, false));
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
         if (lineWidget.getType().equals(ChunkOutput.LINE_WIDGET_TYPE))
            setChunkOutputStyle(lineWidget.getElement());
      }
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
   
   
   private DivElement elementForChunkOutput(ChunkOutput chunkOutput)
   {
      DivElement div = Document.get().createDivElement();
      div.addClassName(ThemeStyles.INSTANCE.selectableText());
      setChunkOutputStyle(div);
      div.getStyle().setOpacity(1.0);
      setChunkOutput(div);
      return div;
   }
   
   private void setChunkOutput(Element div)
   {
      div.setInnerText(Document.get().createUniqueId());
   }
   
   private void setChunkOutputStyle(Element div)
   {
      if (editorStyle_ != null)
      {
         div.getStyle().setBackgroundColor(editorStyle_.getBackgroundColor());
      }
   }
   
  
   
   private JsArray<ChunkOutput> initialChunkOutputs_;
   
   private final DocDisplay docDisplay_;
   @SuppressWarnings("unused")
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private EventBus events_;
   private UIPrefs uiPrefs_;
   
   private Style editorStyle_;
}
