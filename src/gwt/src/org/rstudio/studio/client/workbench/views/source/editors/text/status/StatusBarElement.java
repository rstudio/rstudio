/*
 * StatusBarElement.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;

public interface StatusBarElement extends HasSelectionHandlers<String>,
                                          HasMouseDownHandlers
{
   public void setValue(String value);
   public String getValue();

   void addOptionValue(String label);
   void clearOptions();

   void setVisible(boolean visible);

   void click();
}
