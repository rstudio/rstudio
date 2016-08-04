package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class ChunkDataPage implements ChunkOutputPage
{
   public ChunkDataPage(JavaScriptObject data)
   {
      content_ = new ChunkDataWidget(data);
      thumbnail_ = new HTML("Data");
   }
   
   public ChunkDataPage(ChunkDataWidget content)
   {
      content_ = content;
      thumbnail_ = new HTML("Data");
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
