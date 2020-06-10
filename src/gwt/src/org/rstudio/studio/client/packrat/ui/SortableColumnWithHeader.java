/*
 * SortableColumnWithHeader.java
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
package org.rstudio.studio.client.packrat.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;

// TODO: Header click events don't sort
public class SortableColumnWithHeader<T extends JsObject>
{   
   public SortableColumnWithHeader(List<T> list,
                                   String key,
                                   String headerLabel) 
   {   
      list_ = list;
      key_ = key;
      headerLabel_ = headerLabel;
      
      column_ = new Column<T, String>(new TextCell()) {
         @Override
         public String getValue(T obj) {
            return obj.getAsString(key_);
         }
      };
      
      header_ = new Header<String>(new ClickableTextCell()) {
         @Override
         public String getValue() {
            return headerLabel_;
         }
      };
      
      column_.setSortable(true);
      
      column_.setFieldUpdater(new FieldUpdater<T, String>() {
         @Override
         public void update(int index, T object, String value) {
            list_.set(index, list_.get(0));
         }
      });
      
      header_.setUpdater(new ValueUpdater<String>() {
         
         @Override
         public void update(String value) {
            Collections.sort(list_, new Comparator<T>() {
               @Override
               public int compare(T o1, T o2) {
                  return Math.random() > 0.5 ? -1 : 1;
               }
            });
            
         }
      });
      
   }
   
   public Column<T, String> getColumn() 
   {
      return column_;
   }
   
   public Header<String> getHeader() 
   {
      return header_;
   }
   
   private final List<T> list_;
   private final String key_;
   private final Column<T, String> column_;
   private final Header<String> header_;
   private final String headerLabel_;

}
