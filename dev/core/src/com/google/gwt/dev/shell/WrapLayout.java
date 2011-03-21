/*
 * Copyright 2010 Google Inc.
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

/*
 * Taken from http://tips4java.wordpress.com/2008/11/06/wrap-layout/ and
 * originally written by Rob Camick.  The About link for the web site at
 * http://tips4java.wordpress.com/about/ says this about IP rights:
 *
 * "You are free to use and/or modify any or all code posted on the Java Tips
 * Weblog without restriction. A credit in the code comments would be nice,
 * but not in any way mandatory."
 */
package com.google.gwt.dev.shell;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 */
public class WrapLayout extends FlowLayout {
  private Dimension preferredLayoutSize;

  /**
   * Constructs a new <code>WrapLayout</code> with a left alignment and a
   * default 5-unit horizontal and vertical gap.
   */
  public WrapLayout() {
    super();
  }

  /**
   * Constructs a new <code>FlowLayout</code> with the specified alignment and a
   * default 5-unit horizontal and vertical gap. The value of the alignment
   * argument must be one of <code>WrapLayout</code>, <code>WrapLayout</code>,
   * or <code>WrapLayout</code>.
   *
   * @param align the alignment value
   */
  public WrapLayout(int align) {
    super(align);
  }

  /**
   * Creates a new flow layout manager with the indicated alignment and the
   * indicated horizontal and vertical gaps.
   * <p>
   * The value of the alignment argument must be one of <code>WrapLayout</code>,
   * <code>WrapLayout</code>, or <code>WrapLayout</code>.
   *
   * @param align the alignment value
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   */
  public WrapLayout(int align, int hgap, int vgap) {
    super(align, hgap, vgap);
  }

  /**
   * Layout the components in the Container using the layout logic of the parent
   * FlowLayout class.
   *
   * @param target the Container using this WrapLayout
   */
  @Override
  public void layoutContainer(Container target) {
    Dimension size = preferredLayoutSize(target);

    // When a frame is minimized or maximized the preferred size of the
    // Container is assumed not to change. Therefore we need to force a
    // validate() to make sure that space, if available, is allocated to
    // the panel using a WrapLayout.

    if (size.equals(preferredLayoutSize)) {
      super.layoutContainer(target);
    } else {
      preferredLayoutSize = size;
      Container top = target;

      while (top.getParent() != null) {
        top = top.getParent();
      }

      top.validate();
    }
  }

  /**
   * Returns the minimum dimensions needed to layout the <i>visible</i>
   * components contained in the specified target container.
   *
   * @param target the component which needs to be laid out
   * @return the minimum dimensions to lay out the subcomponents of the
   *         specified container
   */
  @Override
  public Dimension minimumLayoutSize(Container target) {
    return layoutSize(target, false);
  }

  /**
   * Returns the preferred dimensions for this layout given the <i>visible</i>
   * components in the specified target container.
   *
   * @param target the component which needs to be laid out
   * @return the preferred dimensions to lay out the subcomponents of the
   *         specified container
   */
  @Override
  public Dimension preferredLayoutSize(Container target) {
    return layoutSize(target, true);
  }

  /*
   * A new row has been completed. Use the dimensions of this row to update the
   * preferred size for the container.
   *
   * @param dim update the width and height when appropriate
   *
   * @param rowWidth the width of the row to add
   *
   * @param rowHeight the height of the row to add
   */
  private void addRow(Dimension dim, int rowWidth, int rowHeight) {
    dim.width = Math.max(dim.width, rowWidth);

    if (dim.height > 0) {
      dim.height += getVgap();
    }

    dim.height += rowHeight;
  }

  /**
   * Returns the minimum or preferred dimension needed to layout the target
   * container.
   *
   * @param target target to get layout size for
   * @param preferred should preferred size be calculated
   * @return the dimension to layout the target container
   */
  private Dimension layoutSize(Container target, boolean preferred) {
    synchronized (target.getTreeLock()) {
      // Each row must fit with the width allocated to the containter.
      // When the container width = 0, the preferred width of the container
      // has not yet been calculated so lets ask for the maximum.

      int targetWidth = target.getSize().width;

      if (targetWidth == 0) {
        targetWidth = Integer.MAX_VALUE;
      }

      int hgap = getHgap();
      int vgap = getVgap();
      Insets insets = target.getInsets();
      int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
      int maxWidth = targetWidth - horizontalInsetsAndGap;

      // Fit components into the allowed width

      Dimension dim = new Dimension(0, 0);
      int rowWidth = 0;
      int rowHeight = 0;

      int nmembers = target.getComponentCount();

      for (int i = 0; i < nmembers; i++) {
        Component m = target.getComponent(i);
        if (m.isVisible()) {
          Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
          if (rowWidth + d.width > maxWidth) {
            // Can't add the component to current row. Start a new row.
            addRow(dim, rowWidth, rowHeight);
            rowWidth = 0;
            rowHeight = 0;
          }
          if (rowWidth != 0) {
            // Add a horizontal gap for all components after the first
            rowWidth += hgap;
          }
          rowWidth += d.width;
          rowHeight = Math.max(rowHeight, d.height);
        }
      }

      addRow(dim, rowWidth, rowHeight);

      dim.width += horizontalInsetsAndGap;
      dim.height += insets.top + insets.bottom + vgap * 2;

      // When using a scroll pane or the DecoratedLookAndFeel we need to
      // make sure the preferred size is less than the size of the
      // target containter so shrinking the container size works
      // correctly. Removing the horizontal gap is an easy way to do this.

      dim.width -= (hgap + 1);

      return dim;
    }
  }
}