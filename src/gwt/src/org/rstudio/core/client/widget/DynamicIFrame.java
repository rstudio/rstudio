package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.ui.Frame;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.resources.StaticDataResource;

public abstract class DynamicIFrame extends Frame
{
   interface Resources extends ClientBundle
   {
      @Source("dynamicFrame.html")
      StaticDataResource dynamicFrame();
   }

   public DynamicIFrame()
   {
      Resources res = GWT.create(Resources.class);
      setUrl(res.dynamicFrame().getUrl());
      attachCallback();
   }

   protected abstract void onFrameLoaded();

   protected IFrameElementEx getIFrame()
   {
      return getElement().cast();
   }

   protected WindowEx getWindow()
   {
      return getIFrame().getContentWindow();
   }

   protected final Document getDocument()
   {
      return getWindow().getDocument();
   }

   private native final void attachCallback() /*-{
      var self = this;
      var el = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
      el.__dynamic_init__ = $entry(function() {
         self.@org.rstudio.core.client.widget.DynamicIFrame::onFrameLoaded()();
      });
   }-*/;
}
