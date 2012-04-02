/*
 * MacSpellingLanguageWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.spelling.ui;

import org.rstudio.studio.client.common.prefs.PrefsWidgetHelper;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class MacSpellingLanguageWidget extends Composite
{
   public MacSpellingLanguageWidget()
   {
      HorizontalPanel panel = new HorizontalPanel();
      
      panel.add(
           new Label("Using Mac OS X spell-checker and language preferences"));
      
      panel.add(PrefsWidgetHelper.createHelpButton("spelling_dictionaries"));
      
      initWidget(panel);
   }

}
