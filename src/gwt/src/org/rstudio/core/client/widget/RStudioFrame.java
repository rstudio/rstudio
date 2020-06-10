/*
 * RStudioFrame.java
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

package org.rstudio.core.client.widget;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;

import com.google.gwt.user.client.ui.Frame;

public class RStudioFrame extends Frame
{
   public RStudioFrame(String title)
   {
      this(title, null);
   }
   
   public RStudioFrame(String title, String url)
   {
      this(title, url, false, null);
   }
   
   public RStudioFrame(String title, String url, boolean sandbox, String sandboxAllow)
   {
      super();
      if (sandbox)
         getElement().setAttribute("sandbox", StringUtil.notNull(sandboxAllow));
      if (url != null)
         setUrl(url);
      setTitle(title);
   }
   
   public WindowEx getWindow()
   {
      return getIFrame().getContentWindow();
   }
   
   public IFrameElementEx getIFrame()
   {
      return getElement().cast();
   }
   
   public String getSrcUrl()
   {
      return getIFrame().getSrc();
   }
   
   public void setSrcUrl(String url)
   {
      getIFrame().setSrc(url);
   }
   
   public String getWindowName()
   {
      return getIFrame().getContentWindow().getName();
   }
   
   public String getWindowUrl()
   {
      return getIFrame().getContentWindow().getLocationHref();
   }
   
   public void setWindowUrl(String url)
   {
      getIFrame().getContentWindow().setLocationHref(url);
   }
   
   public void replaceWindowUrl(String url)
   {
      getIFrame().getContentWindow().replaceLocationHref(url);
   }

}
