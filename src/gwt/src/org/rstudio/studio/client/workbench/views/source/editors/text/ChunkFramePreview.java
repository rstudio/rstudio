package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class ChunkFramePreview extends Composite
{

   private static ChunkFramePreviewUiBinder uiBinder = GWT
         .create(ChunkFramePreviewUiBinder.class);

   interface ChunkFramePreviewUiBinder
         extends UiBinder<Widget, ChunkFramePreview>
   {
   }

   public ChunkFramePreview()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }

}
