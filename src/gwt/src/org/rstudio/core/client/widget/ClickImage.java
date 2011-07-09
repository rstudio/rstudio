package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;

public class ClickImage extends Image
{
   public ClickImage()
   {
      super();
      commonInit();
   }

   public ClickImage(ImageResource resource)
   {
      super(resource);
      commonInit();
   }

   public ClickImage(String url)
   {
      super(url);
      commonInit();
   }

   public ClickImage(String url, int left, int top, int width, int height)
   {
      super(url, left, top, width, height);
      commonInit();
   }

   public ClickImage(Element element)
   {
      super(element);
      commonInit();
   }

   private void commonInit()
   {
      getElement().getStyle().setCursor(Cursor.POINTER);
   }
}
