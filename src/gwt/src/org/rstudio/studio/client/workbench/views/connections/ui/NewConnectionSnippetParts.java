/*
 * NewConnectionSnippetParts.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.connections.ui;

public class NewConnectionSnippetParts {
   public NewConnectionSnippetParts(int order,
                                    String key,
                                    String value,
                                    String connStringField) {
      order_ = order;
      key_ = key;
      value_ = value;
      connStringField_ = connStringField;
   }

   public int getOrder() {
      return order_;
   }

   public String getKey() {
      return key_;
   }

   public String getValue() {
      return value_;
   }
   
   public String getConnStringField() {
      return connStringField_;
   }

   private int order_;
   private String key_;
   private String value_;
   private String connStringField_;
}