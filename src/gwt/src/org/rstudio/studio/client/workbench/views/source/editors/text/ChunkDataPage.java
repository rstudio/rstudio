package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.rmarkdown.model.NotebookFrameMetadata;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.Widget;

public class ChunkDataPage implements ChunkOutputPage
{
   public ChunkDataPage(JavaScriptObject data, JavaScriptObject metadata)
   {
      content_ = new ChunkDataWidget(data);
      NotebookFrameMetadata meta = metadata.cast();
      String clazz = meta.getClasses().length() > 0 ? 
            meta.getClasses().get(0) : "data";
      thumbnail_ = new ChunkOutputThumbnail(clazz, + 
            meta.numRows() + "x" + meta.numCols());
   }
   
   public ChunkDataPage(ChunkDataWidget content)
   {
      content_ = content;
      thumbnail_ = new ChunkOutputThumbnail("data", "0x0");
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
