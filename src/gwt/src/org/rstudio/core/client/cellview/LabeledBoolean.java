/*
 * LabeledBoolean.java
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
package org.rstudio.core.client.cellview;

import org.rstudio.core.client.StringUtil;

/**
 * A string/boolean pair
 */
public class LabeledBoolean
{
   public LabeledBoolean(String label, boolean bool)
   {
      label_ = label;
      bool_ = bool;
   }

   public String getLabel()
   {
      return label_;
   }

   public boolean getBool()
   {
      return bool_;
   }

   public void setLabel(String label)
   {
      label_ = label;
   }

   public void setBool(boolean bool)
   {
      bool_ = bool;
   }

   public boolean equals(Object rhsObject)
   {
      if (rhsObject == null || !(rhsObject instanceof LabeledBoolean))
         return false;

      LabeledBoolean rhs = (LabeledBoolean)rhsObject;
      return (bool_ == rhs.getBool()) && StringUtil.equals(label_, rhs.getLabel());
   }

   private String label_;
   private boolean bool_;
}
