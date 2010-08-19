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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.requestfactory.client.LoginWidget;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * UI shell for expenses sample app.
 */
public class ExpensesShell extends Composite {

  interface ShellUiBinder extends UiBinder<Widget, ExpensesShell> {
  }

  private static ShellUiBinder uiBinder = GWT.create(ShellUiBinder.class);

  @UiField
  ExpenseList expenseList;
  @UiField
  ExpenseTree expenseTree;
  @UiField
  LayoutPanel layoutPanel;
  @UiField
  LoginWidget loginWidget;
  @UiField
  DockLayoutPanel dockLayout;

  private final ExpenseDetails expenseDetails = new ExpenseDetails();

  public ExpensesShell() {
    initWidget(uiBinder.createAndBindUi(this));

    // Handle breadcrumb events from Expense Details.
    expenseDetails.getReportsLink().addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        showExpenseDetails(false);
      }
    });
  }

  public ExpenseDetails getExpenseDetails() {
    return expenseDetails;
  }

  public ExpenseList getExpenseList() {
    return expenseList;
  }

  public ExpenseTree getExpenseTree() {
    return expenseTree;
  }

  /**
   * @return the login widget
   */
  public LoginWidget getLoginWidget() {
    return loginWidget;
  }
  
  /**
   * Show or hide the expense details. When showing, the expense list is hidden.
   * 
   * @param isShowing true to show details, false to show reports list
   */
  public void showExpenseDetails(boolean isShowing) {
    if (isShowing) {
      showWidget(expenseDetails, false);
    } else {
      showWidget(expenseList, true);
    }
  }

  /**
   * Slide a widget into view.
   * 
   * @param widget the widget to show
   * @param fromLeft true to slide from left, false to slide from right
   */
  private void showWidget(Widget widget, boolean fromLeft) {
    // Early out if the widget is already in the layout panel.
    final Widget current = layoutPanel.getWidget(0);
    if (current == widget) {
      return;
    }

    // Initialize the layout.
    layoutPanel.add(widget);
    layoutPanel.setWidgetLeftWidth(current, 0, Unit.PCT, 100, Unit.PCT);
    if (fromLeft) {
      layoutPanel.setWidgetLeftWidth(widget, -100, Unit.PCT, 100, Unit.PCT);
    } else {
      layoutPanel.setWidgetLeftWidth(widget, 100, Unit.PCT, 100, Unit.PCT);
    }
    layoutPanel.forceLayout();

    // Slide into view.
    if (fromLeft) {
      layoutPanel.setWidgetLeftWidth(current, 100, Unit.PCT, 100, Unit.PCT);
    } else {
      layoutPanel.setWidgetLeftWidth(current, -100, Unit.PCT, 100, Unit.PCT);
    }
    layoutPanel.setWidgetLeftWidth(widget, 0, Unit.PCT, 100, Unit.PCT);
    layoutPanel.animate(500, new Layout.AnimationCallback() {
      public void onAnimationComplete() {
        // Remove the old widget when the animation completes.
        layoutPanel.remove(current);
      }

      public void onLayout(Layer layer, double progress) {
      }
    });
  }
}
