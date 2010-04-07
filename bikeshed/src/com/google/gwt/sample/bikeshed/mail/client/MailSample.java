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
package com.google.gwt.sample.bikeshed.mail.client;

import com.google.gwt.bikeshed.cells.client.ButtonCell;
import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.client.SimpleColumn;
import com.google.gwt.bikeshed.list.client.TextColumn;
import com.google.gwt.bikeshed.list.shared.DefaultSelectionModel;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * A demo of selection features.
 */
public class MailSample implements EntryPoint, ClickHandler {

  static class MailSelectionModel extends DefaultSelectionModel<Message> {
    enum Type {
      ALL(), NONE(), READ(), SENDER(), SUBJECT(), UNREAD();

      Type() {
        typeMap.put(this.toString(), this);
      }
    }

    // A map from enum names to their values
    private static Map<String, Type> typeMap = new HashMap<String, Type>();

    private String search;
    private Type type = Type.NONE;

    @Override
    public boolean isDefaultSelected(Message object) {
      switch (type) {
        case NONE:
          return false;
        case ALL:
          return true;
        case READ:
          return object.isRead();
        case UNREAD:
          return !object.isRead();
        case SENDER:
          if (search == null || search.length() == 0) {
            return false;
          }
          return canonicalize(object.getSender()).contains(search);
        case SUBJECT:
          if (search == null || search.length() == 0) {
            return false;
          }
          return canonicalize(object.getSubject()).contains(search);
        default:
          throw new IllegalStateException("type = " + type);
      }
    }

    public void setSearch(String search) {
      this.search = canonicalize(search);
      scheduleSelectionChangeEvent();
    }

    public void setType(String type) {
      this.type = typeMap.get(type);
      clearExceptions();
      scheduleSelectionChangeEvent();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(type.name());
      sb.append(' ');
      if (type == Type.SENDER || type == Type.SUBJECT) {
        sb.append(search);
        sb.append(' ');
      }

      // Copy the exceptions into a TreeMap in order to sort by message id
      TreeMap<Message, Boolean> exceptions = new TreeMap<Message, Boolean>();
      getExceptions(exceptions);

      appendExceptions(sb, exceptions, true);
      appendExceptions(sb, exceptions, false);

      return sb.toString();
    }

    protected void scheduleSelectionChangeEvent() {
      selectionLabel.setText("Selected " + this.toString());
      super.scheduleSelectionChangeEvent();
    }

    private void appendExceptions(StringBuilder sb,
        Map<Message, Boolean> exceptions, boolean selected) {
      boolean first = true;
      for (Message message : exceptions.keySet()) {
        if (exceptions.get(message) != selected) {
          continue;
        }

        if (first) {
          first = false;
          sb.append(selected ? '+' : '-');
          sb.append("msg(s) ");
        }
        sb.append(message.id);
        sb.append(' ');
      }
    }

    private String canonicalize(String input) {
      return input.toUpperCase();
    }
  }

  // Hashing, comparison, and equality are based on the message id
  class Message implements Comparable<Message> {
    int id;
    boolean isRead;
    String sender;
    String subject;

    public Message(int id, String sender, String subject) {
      super();
      this.id = id;
      this.sender = sender;
      this.subject = subject;
    }

    public int compareTo(Message o) {
      return id - o.id;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Message)) {
        return false;
      }
      return id == ((Message) obj).id;
    }

    public int getId() {
      return id;
    }

    public String getSender() {
      return sender;
    }

    public String getSubject() {
      return subject;
    }

    @Override
    public int hashCode() {
      return id;
    }

    public boolean isRead() {
      return isRead;
    }

    @Override
    public String toString() {
      return "Message [id=" + id + ", sender=" + sender + ", subject="
          + subject + ", read=" + isRead + "]";
    }
  }

  private static Label selectionLabel = new Label("Selected NONE");

  private static final String[] senders = {
      "test@example.com", "spam1@spam.com", "gwt@google.com", "Mai Oleta",
      "Barbara Myles", "Celsa Ocie", "Elwood Holloway", "Bolanle Alford",
      "Amaka Ackland", "Afia Audley", "Pearlene Cher", "Pei Sunshine",
      "Zonia Dottie", "Krystie Jetta", "Alaba Banvard", "Ines Azalee",
      "Kaan Boulier", "Emilee Naoma", "Atino Alice", "Debby Renay",
      "Versie Nereida", "Ramon Erikson", "Karole Crissy", "Nelda Olsen",
      "Mariana Dann", "Reda Cheyenne", "Edelmira Jody", "Agueda Shante",
      "Marla Dorris"
  };

  private static final String[] subjects = {
      "GWT rocks", "What's a widget?", "Money in Nigeria",
      "Impress your colleagues with bling-bling", "Degree available",
      "Rolex Watches", "Re: Re: yo bud", "Important notice"
  };

  private MailSelectionModel selectionModel = new MailSelectionModel();

  private PagingTableListView<Message> table;

  // Handle events for all buttons here in order to avoid creating multiple
  // ClickHandlers
  public void onClick(ClickEvent event) {
    String id = ((Button) event.getSource()).getElement().getId();
    if ("PAGE".equals(id)) {
      // selectionModel.setType(MailSelectionModel.NONE);
      List<Message> selectedItems = table.getDisplayedItems();
      for (Message item : selectedItems) {
        selectionModel.setSelected(item, true);
      }
    } else {
      selectionModel.setType(id);
    }
  }

  public void onModuleLoad() {
    ListListModel<Message> listModel = new ListListModel<Message>();
    List<Message> messages = listModel.getList();
    Random rand = new Random();
    for (int i = 0; i < 1000; i++) {
      Message message = new Message(10000 + i,
          senders[rand.nextInt(senders.length)],
          subjects[rand.nextInt(subjects.length)]);
      message.isRead = rand.nextBoolean();
      messages.add(message);
    }

    table = new PagingTableListView<Message>(listModel, 10);
    table.setSelectionModel(selectionModel);

    // The state of the checkbox is taken from the selection model
    SimpleColumn<Message, Boolean> selectedColumn = new SimpleColumn<Message, Boolean>(
        new CheckboxCell()) {
      @Override
      public Boolean getValue(Message object) {
        return selectionModel.isSelected(object);
      }
    };
    // Update the selection model when the checkbox is changed manually
    selectedColumn.setFieldUpdater(new FieldUpdater<Message, Boolean, Void>() {
      public void update(int index, Message object, Boolean value, Void viewData) {
        selectionModel.setSelected(object, value);
      }
    });
    table.addColumn(selectedColumn, "Selected");

    TextColumn<Message> isReadColumn = new TextColumn<Message>() {
      @Override
      public String getValue(Message object) {
        return object.isRead ? "read" : "unread";
      }
    };
    table.addColumn(isReadColumn, "Read");

    TextColumn<Message> senderColumn = new TextColumn<Message>() {
      @Override
      public String getValue(Message object) {
        return object.getSender();
      }
    };
    table.addColumn(senderColumn, "Sender");

    TextColumn<Message> subjectColumn = new TextColumn<Message>() {
      @Override
      public String getValue(Message object) {
        return object.getSubject();
      }
    };
    table.addColumn(subjectColumn, "Subject");

    SimpleColumn<Message, String> toggleColumn =
      new SimpleColumn<Message, String>(ButtonCell.getInstance()) {
      @Override
      public String getValue(Message object) {
        return object.isRead ? "Mark Unread" : "Mark Read";
      }
    };
    toggleColumn.setFieldUpdater(new FieldUpdater<Message, String, Void>() {
      public void update(int index, Message object, String value, Void viewData) {
        object.isRead = !object.isRead;
        table.refresh();
      }
    });
    table.addColumn(toggleColumn, "Toggle Read/Unread");

    Label searchLabel = new Label("Search Sender or Subject:");
    final TextBox searchBox = new TextBox();
    searchBox.addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        selectionModel.setSearch(searchBox.getText());
      }
    });

    HorizontalPanel panel = new HorizontalPanel();
    panel.add(searchLabel);
    panel.add(searchBox);

    RootPanel.get().add(panel);
    RootPanel.get().add(makeButton("Search Subject", "SUBJECT"));
    RootPanel.get().add(makeButton("Search Senders", "SENDER"));
    RootPanel.get().add(new HTML("<br>"));
    RootPanel.get().add(table);
    RootPanel.get().add(new HTML("<br>"));
    RootPanel.get().add(makeButton("Select None", "NONE"));
    RootPanel.get().add(makeButton("Select All On This Page", "PAGE"));
    RootPanel.get().add(makeButton("Select All", "ALL"));
    RootPanel.get().add(makeButton("Select Read", "READ"));
    RootPanel.get().add(makeButton("Select Unread", "UNREAD"));
    RootPanel.get().add(new HTML("<hr>"));
    RootPanel.get().add(selectionLabel);
  }

  private Button makeButton(String label, String id) {
    Button button = new Button(label);
    button.getElement().setId(id);
    button.addClickHandler(this);
    return button;
  }
}
