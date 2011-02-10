package org.rstudio.core.client.widget;

import org.rstudio.core.client.resources.CoreResources;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

public class ProgressImage extends Composite
{
   public ProgressImage()
   {
      this(CoreResources.INSTANCE.progress());
   }
   
   public ProgressImage(ImageResource image)
   {
      image_ = image;
      panel_ = new SimplePanel();
      initWidget(panel_);
   }
   
   public void show(boolean show)
   {
      if (!show)
      {
         panel_.setWidget(null);
         setVisible(false);
      }
      else
      {
         final Image img = new Image(image_);
         panel_.setWidget(img);
         setVisible(true);
      }
   }

   private ImageResource image_;
   private SimplePanel panel_;
}
