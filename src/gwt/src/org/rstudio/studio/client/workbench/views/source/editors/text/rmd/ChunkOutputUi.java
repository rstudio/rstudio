/*
 * ChunkOutputUi.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputSize;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;
import org.rstudio.studio.client.workbench.views.source.events.ChunkChangeEvent;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

public abstract class ChunkOutputUi
             implements ChunkOutputHost,
                        RenderFinishedEvent.Handler
{
   public ChunkOutputUi(String docId, ChunkDefinition def)
   {
      this(docId, def, null);
   }

   public ChunkOutputUi(String docId, ChunkDefinition def, ChunkOutputWidget widget)
   {
      chunkId_ = def.getChunkId();
      docId_ = docId;
      def_ = def;
      boolean hasOutput = widget != null;
      if (widget == null) 
      {
         // create the widget
         widget = new ChunkOutputWidget(docId_, def.getChunkId(), 
               def.getOptions(), def.getExpansionState(), true, this, 
               getChunkOutputSize());

         // sync the widget's expanded/collapsed state to the underlying chunk
         // definition (which is persisted)
         widget.addExpansionStateChangeHandler(
               new ValueChangeHandler<Integer>()
         {
            @Override
            public void onValueChange(ValueChangeEvent<Integer> event)
            {
               def_.setExpansionState(event.getValue());
            }
         });
      }
      else
      {
         widget.setHost(this);
      }

      outputWidget_ = widget;

      // label_ should only be set by this function
      setChunkLabel(def.getChunkLabel());
      
      Element ele = outputWidget_.getElement();
      ele.addClassName(ThemeStyles.INSTANCE.selectableText());
      
      // if we didn't start with output, make the widget initially invisible
      // (until it gets some output)
      if (!hasOutput)
      {
         applyHeight(0);
         outputWidget_.setVisible(false);
      }
      
      attached_ = true;
   }
   
   // Public methods ----------------------------------------------------------

   public abstract int getCurrentRow();
   public abstract void ensureVisible();
   public abstract Scope getScope();
   
   public abstract void detach();
   public abstract void reattach();
   public abstract void applyHeight(int heightPx);
   public abstract ChunkOutputSize getChunkOutputSize();
   
   public String getChunkId()
   {
      return def_.getChunkId();
   }
   
   public String getChunkLabel()
   {
      return label_;
   }

   public void setChunkLabel(String label)
   {
      label_ = label;
      if (outputWidget_ != null)
         outputWidget_.setClassId(label);
   }

   public ChunkOutputWidget getOutputWidget()
   {
      return outputWidget_;
   }
   
   public RmdChunkOptions getOptions()
   {
      return def_.getOptions();
   }
   
   public void setOptions(RmdChunkOptions options)
   {
      def_.setOptions(options);
      outputWidget_.setOptions(options);
      setChunkLabel(def_.getChunkLabel());
   }
   
   public void remove()
   {
      if (!attached_)
         return;
      attached_ = false;
   }
   
   @Override
   public void onOutputRemoved(ChunkOutputWidget widget)
   {
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(
              new ChunkChangeEvent(docId_, chunkId_, "", 0, 
                                   ChunkChangeEvent.CHANGE_REMOVE));
   }

   public boolean hasErrors()
   {
      return outputWidget_.hasErrors();
   }
   
   public ChunkDefinition getDefinition()
   {
      return def_;
   }
   
   public String getDocId()
   {
      return docId_;
   }
   
   // Private methods ---------------------------------------------------------

   protected final ChunkOutputWidget outputWidget_;
   protected boolean attached_ = false;
   protected int height_ = 0;

   private final String chunkId_;
   private final String docId_;
   private final ChunkDefinition def_;

   private String label_;


   public final static int MIN_CHUNK_HEIGHT = 25;
   public final static int CHUNK_COLLAPSED_HEIGHT = 15;
   public final static int MAX_CHUNK_HEIGHT = 1000;
   
   public final static int MIN_PLOT_WIDTH = 400;
   public final static int MAX_PLOT_WIDTH = 700;
   public final static int MAX_HTMLWIDGET_WIDTH = 800;
   
   public final static double OUTPUT_ASPECT = 1.618;
}
