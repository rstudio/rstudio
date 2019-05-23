/*
 * LabelWithHelp.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class LabelWithHelp extends Composite
{
   public LabelWithHelp(String text, String helpTopic)
   {
      this(text, helpTopic, true);
   }
   
   public LabelWithHelp(String text, 
                        String helpTopic, 
                        boolean includeVersionInfo)
   {
      HorizontalPanel labelPanel = new HorizontalPanel();
      Label label = new Label(text);
      labelPanel.add(label);
      HelpButton helpButton =  new HelpButton(helpTopic, includeVersionInfo);
      helpButton.getElement().getStyle().setMarginLeft(3, Unit.PX);
      labelPanel.add(helpButton);
      initWidget(labelPanel);
   }  
}
