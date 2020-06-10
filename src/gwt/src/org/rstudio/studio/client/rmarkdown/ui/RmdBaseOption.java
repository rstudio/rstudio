/*
 * RmdBaseOption.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import org.rstudio.studio.client.rmarkdown.model.RmdTemplateFormatOption;

import com.google.gwt.user.client.ui.Composite;

public abstract class RmdBaseOption extends Composite 
                                       implements RmdFormatOption
{
   public RmdBaseOption(RmdTemplateFormatOption option)
   {
      option_ = option;
   }
   
   @Override
   public RmdTemplateFormatOption getOption()
   {
      return option_;
   }
   
   private RmdTemplateFormatOption option_;
}
