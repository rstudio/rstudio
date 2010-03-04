package com.google.gwt.sample.expenses.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.sample.expenses.shared.Employee;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.valuestore.shared.Values;

import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Expenses implements EntryPoint {
  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  private final ExpenseRequestFactory requestFactory = GWT.create(ExpenseRequestFactory.class);

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    RootLayoutPanel root = RootLayoutPanel.get();

    final Shell shell = new Shell();
    root.add(shell);
    Command refresh = new Command() {
      public void execute() {
        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
            "/expenses/data");
        builder.setCallback(new RequestCallback() {

          public void onResponseReceived(Request request, Response response) {
            if (200 == response.getStatusCode()) {
              String text = response.getText();
              JsArray<ValuesImpl<Employee>> valueArray = ValuesImpl.arrayFromJson(text);
              shell.setValueList(valueArray);
            } else {
              shell.error.setInnerText(SERVER_ERROR + " (" + response.getStatusText()
                  + ")");
            }
          }

          public void onError(Request request, Throwable exception) {
            shell.error.setInnerText(SERVER_ERROR);
          }
        });
        
        try {
          builder.send();
        } catch (RequestException e) {
          shell.error.setInnerText(SERVER_ERROR + " (" + e.getMessage() +")");
        }
      }
    };
    refresh.execute();
    shell.setRefresh(refresh);

    final HasValueList<Values<Employee>> employees = new HasValueList<Values<Employee>>() {

      public void editValueList(boolean replace, int index,
          List<Values<Employee>> newValues) {
        throw new UnsupportedOperationException();
      }

      public void setValueList(List<Values<Employee>> newValues) {
        shell.users.clear();
        for (Values<Employee> values : newValues) {
          shell.users.addItem(values.get(Employee.DISPLAY_NAME),
              values.get(Employee.USER_NAME));
        }
      }

      public void setValueListSize(int size, boolean exact) {
        throw new UnsupportedOperationException();
      }
    };

    requestFactory.employeeRequest().findAllEmployees() //
    .forProperty(Employee.DISPLAY_NAME) //
    .forProperty(Employee.USER_NAME) //
    .to(employees).fire();

    // TODO(rjrjr) now get details
    final TextBox nameHolder = new TextBox();

    shell.users.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        nameHolder.setText("gesundheit");
        // Remember the slots
        // requestFactory.employeeRequest().findEmployee(literal(shell.users.getValue());
      }
    });
  }
}
