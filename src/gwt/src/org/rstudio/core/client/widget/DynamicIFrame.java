/*
 * DynamicIFrame.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
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
      setUrl(res.dynamicFrame().getSafeUri().asString());
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
