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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.List;

/**
 * Entry point to create database entries for the Expenses app.
 */
public class LoadExpensesDB implements EntryPoint {

  private TextBox amountTextBox;

  private Button countEmployeesButton;
  private Label countEmployeesLabel;
  private Button countExpensesButton;
  private Label countExpensesLabel;
  private Button countReportsButton;
  private Label countReportsLabel;
  private final DataGenerationServiceAsync dataService = GWT.create(DataGenerationService.class);

  private Button deleteButton;
  private Button generateButton;
  
  private Label numEmployeesLabel;
  private Label numExpensesLabel;
  private Label numReportsLabel;
  private Button resetCountsButton;
  private Label resetCountsLabel;
  private Label statusLabel;

  public void onModuleLoad() {
    statusLabel = new Label("");
    numEmployeesLabel = new Label("-- Employees");
    numReportsLabel = new Label("-- Reports");
    numExpensesLabel = new Label("-- Expenses");
    
    generateButton = new Button("Generate Data");
    deleteButton = new Button("Delete everything");
    amountTextBox = new TextBox();
    amountTextBox.setText("200");
    
    countEmployeesButton = new Button("Count Employees");
    countEmployeesLabel = new Label("-- Employees");
    
    countExpensesButton = new Button("Count Expenses");
    countExpensesLabel = new Label("-- Expenses");
    
    countReportsButton = new Button("Count Reports");
    countReportsLabel = new Label("-- Reports");
    
    resetCountsButton = new Button("Reset Counts");
    resetCountsLabel = new Label("");

    generateButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        generateButton.setEnabled(false);
        generateData(Integer.parseInt(amountTextBox.getText()));
      }
    });
    
    deleteButton.addClickHandler(new ClickHandler() {      
      public void onClick(ClickEvent event) {
        deleteButton.setEnabled(false);
        deleteData();
      }
    });
    
    resetCountsButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        resetCountsButton.setEnabled(false);
        resetCounts();
      }
    });
    
    countEmployeesButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        countEmployeesButton.setEnabled(false);
        countEmployees();
      }
    });
    
    countExpensesButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        countExpensesButton.setEnabled(false);
        countExpenses();
      }
    });
    
    countReportsButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        countReportsButton.setEnabled(false);
        countReports();
      }
    });

    RootPanel root = RootPanel.get();
    
    root.add(resetCountsButton);
    root.add(resetCountsLabel);
    
    root.add(new HTML("<br><br>"));
    
    root.add(countEmployeesButton);
    root.add(countEmployeesLabel);
    
    root.add(new HTML("<br><br>"));
    
    root.add(countExpensesButton);
    root.add(countExpensesLabel);
    
    root.add(new HTML("<br><br>"));
    
    root.add(countReportsButton);
    root.add(countReportsLabel);
    
    root.add(new HTML("<br><br>"));
    
    root.add(generateButton);
    root.add(amountTextBox);
    root.add(statusLabel);
    root.add(numEmployeesLabel);
    root.add(numReportsLabel);
    root.add(numExpensesLabel);

    // This button deletes a random chunk from the data store -- be careful!
    // root.add(new HTML("<br><br><br><br><br><br><br><br><br>"));
    // root.add(deleteButton);

    updateCounts();
  }

  private void countEmployees() {
    countEmployeesLabel.setText("Counting...");
    dataService.countEmployees(new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        countEmployeesButton.setEnabled(true);
        countEmployeesLabel.setText("Failed");
      }

      public void onSuccess(Long result) {
        countEmployeesButton.setEnabled(true);
        countEmployeesLabel.setText("" + result);
      }
    });
  }

  private void countExpenses() {
    countExpensesLabel.setText("Counting...");
    dataService.countExpenses(new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        countExpensesButton.setEnabled(true);
        countExpensesLabel.setText("Failed");
      }

      public void onSuccess(Long result) {
        countExpensesButton.setEnabled(true);
        countExpensesLabel.setText("" + result);
      }
    });
  }
  
  private void countReports() {
    countReportsLabel.setText("Counting...");
    dataService.countReports(new AsyncCallback<Long>() {
      public void onFailure(Throwable caught) {
        countReportsButton.setEnabled(true);
        countReportsLabel.setText("Failed");
      }

      public void onSuccess(Long result) {
        countReportsButton.setEnabled(true);
        countReportsLabel.setText("" + result);
      }
    });
  }
  
  private void deleteData() {
    dataService.delete(new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        statusLabel.setText("Deletion failed");
        deleteButton.setEnabled(true);
        updateCounts();
      }

      public void onSuccess(Void result) {
        statusLabel.setText("Deletion succeeded");
        deleteButton.setEnabled(true);
        updateCounts();
      }
    });
  }

  private void generateData(int amount) {
    dataService.generate(amount, new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        statusLabel.setText("Data generation failed");
        generateButton.setEnabled(true);
        updateCounts();
      }

      public void onSuccess(Void result) {
        statusLabel.setText("Data generation succeeded");
        generateButton.setEnabled(true);
        updateCounts();
      }
    });
  }
  
  private void resetCounts() {
    resetCountsLabel.setText("Resetting counts...");
    dataService.resetCounters(new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        resetCountsButton.setEnabled(true);
        resetCountsLabel.setText("Resetting counts failed");
      }

      public void onSuccess(Void result) {
        resetCountsButton.setEnabled(true);
        resetCountsLabel.setText("Resetting counts succeeded");
      }
    });
  }

  private void updateCounts() {
    dataService.getCounts(new AsyncCallback<List<Integer>>() {
      public void onFailure(Throwable caught) {
        numEmployeesLabel.setText("? Employees");
        numReportsLabel.setText("? Reports");
        numExpensesLabel.setText("? Expenses");
      }

      public void onSuccess(List<Integer> result) {
        numEmployeesLabel.setText("" + result.get(0) + " Employees");
        numReportsLabel.setText("" + result.get(1) + " Reports");
        numExpensesLabel.setText("" + result.get(2) + " Expenses");
      }
    });
  }
}
