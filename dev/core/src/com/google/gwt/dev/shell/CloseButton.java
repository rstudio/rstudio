/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.dev.shell;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Represents a close button, shrink-wrapped to exactly fit the size of the
 * close icon.
 */
public class CloseButton extends JPanel {

  /**
   * Callback interface for clicking on the close button.
   */
  public interface Callback {

    /**
     * Called when the close button is clicked.
     */
    void onCloseRequest();
  }

  private Callback callback;

  /**
   * Create a close button.
   *
   * @param toolTipText text to use for tooltip if non-null
   */
  public CloseButton(String toolTipText) {
    super(new FlowLayout(FlowLayout.LEFT, 0, 0));
    setOpaque(false);
    ImageIcon closeIcon = Icons.getClose();
    JButton button = new JButton(closeIcon);
    button.setBorderPainted(false);
    button.setPreferredSize(new Dimension(closeIcon.getIconWidth(),
        closeIcon.getIconHeight()));
    if (toolTipText != null) {
      button.setToolTipText(toolTipText);
    }
    add(button);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (callback != null) {
          callback.onCloseRequest();
        }
      }
    });
  }

  /**
   * Set the callback for when this button is clicked.
   *
   * @param callback
   */
  public void setCallback(Callback callback) {
    this.callback = callback;
  }
}
