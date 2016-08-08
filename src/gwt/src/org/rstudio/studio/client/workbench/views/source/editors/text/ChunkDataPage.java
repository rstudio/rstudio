package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Widget;

public class ChunkDataPage implements ChunkOutputPage
{
   public ChunkDataPage(JavaScriptObject data, NotebookFrameMetadata metadata)
   {
      this(new ChunkDataWidget(data), metadata);
   }
   
   public ChunkDataPage(ChunkDataWidget widget, NotebookFrameMetadata metadata)
   {
      content_ = widget;
      if (metadata == null)
      {
         thumbnail_ = new ChunkOutputThumbnail("data.frame", "",
               new ChunkFramePreview());
      }
      else
      {
         String clazz = metadata.getClasses().length() > 0 ? 
               metadata.getClasses().get(0) : "data";
         thumbnail_ = new ChunkOutputThumbnail(clazz, + 
               metadata.numRows() + "x" + metadata.numCols(),
               new ChunkFramePreview());
      }
   }
   
   @Override
   public Widget thumbnailWidget()
   {
      return thumbnail_;
   }

   @Override
   public Widget contentWidget()
   {
      return content_;
   }

   private final Widget thumbnail_;
   private final ChunkDataWidget content_;
}
