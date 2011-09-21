/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.cellview.client;

import com.google.gwt.dom.builder.shared.TableSectionBuilder;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;

/**
 * Builds the DOM elements for the footer section of a CellTable. It also
 * provides queries on elements in the last DOM subtree that it created.
 * 
 * <p>
 * {@link FooterBuilder} provides two optional ways to handle events, via a
 * {@link Column}, a {@link Header}, or both. If {@link #getColumn(Element)}
 * returns a {@link Column} given the target {@link Element} of an event, cell
 * table will use it to enable features such as sorting. If
 * {@link #getHeader(Element)} returns a {@link Header}, cell table will forward
 * the event to the {@link Header}. You can specify both a {@link Column} and
 * {@link Header}.
 * </p>
 * 
 * <p>
 * The default implementation used by cell widgets is
 * {@link DefaultHeaderOrFooterBuilder}.
 * </p>
 * 
 * @param <T> the row data type
 */
public interface FooterBuilder<T> {

  /**
   * Builds the DOM subtree for this footer. The root of the subtree must be a
   * TFOOT element, as appropriate. This method may be called multiple times and
   * should return a new DOM subtree each time.
   * 
   * <p>
   * If the footer is empty, return null.
   * </p>
   * 
   * @return a {@link TableSectionBuilder} representing the new footer, or null
   *         if the footer is empty
   */
  TableSectionBuilder buildFooter();

  /**
   * Given an element in the DOM subtree returned by the most recent call to
   * {@link #buildFooter()}, returns the Column that should be the target of any
   * button clicks or other events on that element, or null if the events should
   * be discarded. The column is used to support features such as column
   * sorting.
   * 
   * @param elem the element that the contains column
   * @return the immediate column contained by the element
   */
  Column<T, ?> getColumn(Element elem);

  /**
   * If you want to handle browser events using a subclass of {@link Header},
   * implement this method to return the appropriate instance and cell table
   * will forward events originating in the element to the {@link Header}.
   * Return null if events from the element should be discarded.
   * 
   * @param elem the element that the contains header
   * @return the immediate {@link Header} contained by the element
   */
  Header<?> getHeader(Element elem);

  /**
   * Get the row index from the associated
   * {@link TableRowElement} (an TR element).
   * 
   * @param row the row element
   * @return the row value index
   */
  int getRowIndex(TableRowElement row);
  
  /**
   * Check if an element contains a {@link Column}. This method should return
   * false if and only if {@link #getColumn(Element)} would return null.
   * 
   * @param elem the element of interest
   */
  boolean isColumn(Element elem);

  /**
   * Check if an element contains a {@link Header}. This method should return
   * false if and only if {@link #getHeader(Element)} would return null.
   * 
   * @param elem the element of interest
   */
  boolean isHeader(Element elem);
}
