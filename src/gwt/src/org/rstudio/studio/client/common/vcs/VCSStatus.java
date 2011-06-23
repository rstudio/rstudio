/*
 * VCSStatus.java
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
package org.rstudio.studio.client.common.vcs;

import org.rstudio.core.client.StringUtil;

public class VCSStatus implements Comparable<VCSStatus>
{
   public VCSStatus(String status)
   {
      status_ = StringUtil.notNull(status);
   }

   public String getStatus()
   {
      return status_;
   }

   public char charAt(int offset)
   {
      return offset < status_.length() ? status_.charAt(offset) : ' ';
   }

   public int compareTo(VCSStatus vcsStatus)
   {
      // TODO: implement
      return 0;
   }

   private String status_;
}
