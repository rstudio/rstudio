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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.bikeshed.cells.client.ButtonCell;
import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.ClickableTextCell;
import com.google.gwt.bikeshed.cells.client.DatePickerCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.client.SimpleColumn;
import com.google.gwt.bikeshed.list.shared.DefaultSelectionModel;
import com.google.gwt.bikeshed.list.shared.ListViewAdapter;
import com.google.gwt.bikeshed.list.shared.ProvidesKey;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * A recipe for mail-like selection features.
 */
public class MailRecipe extends Recipe implements ClickHandler {

  static interface GetValue<T, C> {
    C getValue(T object);
  }

  static class MailSelectionModel extends DefaultSelectionModel<Message> {
    enum Type {
      ALL(), NONE(), READ(), SENDER(), SUBJECT(), UNREAD();

      Type() {
        typeMap.put(this.toString(), this);
      }
    }

    private static ProvidesKey<Message> keyProvider = new ProvidesKey<Message>() {
      public Object getKey(Message item) {
        return Integer.valueOf(item.id);
      }
    };

    // A map from enum names to their values
    private static Map<String, Type> typeMap = new HashMap<String, Type>();

    private String search;
    private Type type = Type.NONE;

    @Override
    public ProvidesKey<Message> getKeyProvider() {
      return keyProvider;
    }

    public String getType() {
      return type.toString();
    }

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
      TreeMap<Object, Boolean> exceptions = new TreeMap<Object, Boolean>();
      getExceptions(exceptions);

      appendExceptions(sb, exceptions, true);
      appendExceptions(sb, exceptions, false);

      return sb.toString();
    }

    @Override
    protected void scheduleSelectionChangeEvent() {
      selectionLabel.setText("Selected " + this.toString());
      super.scheduleSelectionChangeEvent();
    }

    private void appendExceptions(StringBuilder sb,
        Map<Object, Boolean> exceptions, boolean selected) {
      boolean first = true;
      for (Map.Entry<Object, Boolean> entry : exceptions.entrySet()) {
        if (entry.getValue() != selected) {
          continue;
        }

        if (first) {
          first = false;
          sb.append(selected ? '+' : '-');
          sb.append("msg(s) ");
        }
        sb.append(entry.getKey());
        sb.append(' ');
      }
    }

    private String canonicalize(String input) {
      return input.toUpperCase();
    }
  }

  // Hashing, comparison, and equality are based on the message id
  static class Message {
    Date date;
    int id;
    boolean isRead;
    String sender;
    String subject;

    public Message(int id, String sender, String subject, Date date) {
      super();
      this.id = id;
      this.sender = sender;
      this.subject = subject;
      this.date = date;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Message)) {
        return false;
      }
      return id == ((Message) obj).id;
    }

    public Date getDate() {
      return date;
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
          + subject + ", read=" + isRead + ", date=" + date + "]";
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
      "Marla Dorris"};

  private static final String[] subjects = {
      "GWT rocks", "What's a widget?", "Money in Nigeria",
      "Impress your colleagues with bling-bling", "Degree available",
      "Rolex Watches", "Re: Re: yo bud", "Important notice"};

  private Button add1Button;

  private FocusWidget add5Button;

  private List<Message> messages;

  private Button nextPageButton;

  private Button prevPageButton;

  private Button remove1Button;

  private Button remove5Button;

  private MailSelectionModel selectionModel = new MailSelectionModel();

  private PagingTableListView<Message> table;

  public MailRecipe() {
    super("Mail");
  }

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
    } else if ("NEXT".equals(id)) {
      table.nextPage();
      updatePagingButtons();
    } else if ("PREV".equals(id)) {
      table.previousPage();
      updatePagingButtons();
    } else if (id.startsWith("ADD")) {
      table.setPageSize(table.getPageSize() + Integer.parseInt(id.substring(3)));
      updatePagingButtons();
    } else if (id.startsWith("REM")) {
      table.setPageSize(Math.max(table.getPageSize() - Integer.parseInt(id.substring(3)), 0));
      updatePagingButtons();
    } else {
      selectionModel.setType(id);
    }
  }

  @Override
  protected Widget createWidget() {
    ListViewAdapter<Message> adapter = new ListViewAdapter<Message>();
    messages = adapter.getList();

    Date now = new Date();
    Random rand = new Random();
    for (int i = 0; i < 25; i++) {
      // Go back up to 90 days from the current date
      long dateOffset = rand.nextInt(60 * 60 * 24 * 90) * 1000L;
      Message message = new Message(10000 + i,
          senders[rand.nextInt(senders.length)],
          subjects[rand.nextInt(subjects.length)], new Date(now.getTime()
              - dateOffset));
      message.isRead = rand.nextBoolean();
      messages.add(message);
    }

    final Comparator<Message> idComparator = new Comparator<Message>() {
      public int compare(Message o1, Message o2) {
        // Integer comparison
        return o1.id - o2.id;
      }
    };

    final Comparator<Message> dateComparator = new Comparator<Message>() {
      public int compare(Message o1, Message o2) {
        long cmp = o1.date.getTime() - o2.date.getTime();
        if (cmp < 0) {
          return -1;
        } else if (cmp > 0) {
          return 1;
        } else {
          return 0;
        }
      }
    };

    sortMessages(idComparator, true);

    table = new PagingTableListView<Message>(10);
    table.setSelectionModel(selectionModel);
    adapter.addView(table);

    // The state of the checkbox is synchronized with the selection model
    SelectionColumn<Message> selectedColumn = new SelectionColumn<Message>(
        selectionModel);
    Header<Boolean> selectedHeader = new Header<Boolean>(new CheckboxCell()) {
      @Override
      public boolean dependsOnSelection() {
        return true;
      }

      @Override
      public Boolean getValue() {
        return selectionModel.getType().equals("ALL");
      }
    };
    selectedHeader.setUpdater(new ValueUpdater<Boolean, Void>() {
      public void update(Boolean value, Void viewData) {
        if (value == true) {
          selectionModel.setType("ALL");
        } else if (value == false) {
          selectionModel.setType("NONE");
        }
      }
    });
    table.addColumn(selectedColumn, selectedHeader);

    addColumn(table, "ID", TextCell.getInstance(),
        new GetValue<Message, String>() {
          public String getValue(Message object) {
            return "" + object.id;
          }
        }, idComparator);

    addColumn(table, "Read", new GetValue<Message, String>() {
      public String getValue(Message object) {
        return object.isRead ? "read" : "unread";
      }
    });

    Column<Message, Date, Void> dateColumn = addColumn(table, "Date",
        new DatePickerCell<Void>(), new GetValue<Message, Date>() {
          public Date getValue(Message object) {
            return object.date;
          }
        }, dateComparator);
    dateColumn.setFieldUpdater(new FieldUpdater<Message, Date, Void>() {
      public void update(int index, Message object, Date value, Void viewData) {
        Window.alert("Changed date from " + object.date + " to " + value);
        object.date = value;
        table.refresh();
      }
    });

    addColumn(table, "Sender", new GetValue<Message, String>() {
      public String getValue(Message object) {
        return object.getSender();
      }
    });

    addColumn(table, "Subject", new GetValue<Message, String>() {
      public String getValue(Message object) {
        return object.getSubject();
      }
    });

    SimpleColumn<Message, String> toggleColumn = new SimpleColumn<Message, String>(
        ButtonCell.getInstance()) {
      @Override
      public String getValue(Message object) {
        return object.isRead ? "Mark Unread" : "Mark Read";
      }
    };
    toggleColumn.setFieldUpdater(new FieldUpdater<Message, String, Void>() {
      public void update(int index, Message object, String value, Void viewData) {
        object.isRead = !object.isRead;
        messages.set(index, object);
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

    FlowPanel p = new FlowPanel();
    p.add(panel);
    p.add(makeButton("Search Subject", "SUBJECT"));
    p.add(makeButton("Search Senders", "SENDER"));
    p.add(new HTML("<br>"));
    p.add(table);
    p.add(prevPageButton = makeButton("Previous Page", "PREV"));
    prevPageButton.setEnabled(false);
    p.add(nextPageButton = makeButton("Next Page", "NEXT"));
    nextPageButton.setEnabled(true);
    p.add(remove1Button = makeButton("Remove row", "REM1"));
    remove1Button.setEnabled(true);
    p.add(add1Button = makeButton("Add row", "ADD1"));
    add1Button.setEnabled(true);
    p.add(remove5Button = makeButton("Remove 5 rows", "REM5"));
    remove5Button.setEnabled(true);
    p.add(add5Button = makeButton("Add 5 rows", "ADD5"));
    add5Button.setEnabled(true);
    p.add(new HTML("<br>"));
    p.add(makeButton("Select None", "NONE"));
    p.add(makeButton("Select All On This Page", "PAGE"));
    p.add(makeButton("Select All", "ALL"));
    p.add(makeButton("Select Read", "READ"));
    p.add(makeButton("Select Unread", "UNREAD"));
    p.add(new HTML("<hr>"));
    p.add(selectionLabel);
    return p;
  }

  private <C extends Comparable<C>> Column<Message, C, Void> addColumn(
      PagingTableListView<Message> table, final String text,
      final Cell<C, Void> cell, final GetValue<Message, C> getter,
      final Comparator<Message> comparator) {
    Column<Message, C, Void> column = new Column<Message, C, Void>(cell) {
      @Override
      public C getValue(Message object) {
        return getter.getValue(object);
      }
    };
    Header<String> header = new Header<String>(ClickableTextCell.getInstance()) {
      @Override
      public String getValue() {
        return text;
      }
    };
    header.setUpdater(new ValueUpdater<String, Void>() {
      boolean sortUp = true;

      public void update(String value, Void viewData) {
        if (comparator == null) {
          sortMessages(new Comparator<Message>() {
            public int compare(Message o1, Message o2) {
              return getter.getValue(o1).compareTo(getter.getValue(o2));
            }
          }, sortUp);
        } else {
          sortMessages(comparator, sortUp);
        }
        sortUp = !sortUp;
      }
    });
    table.addColumn(column, header);
    return column;
  }

  private Column<Message, String, Void> addColumn(
      PagingTableListView<Message> table, final String text,
      final GetValue<Message, String> getter) {
    return addColumn(table, text, TextCell.getInstance(), getter, null);
  }

  private Button makeButton(String label, String id) {
    Button button = new Button(label);
    button.getElement().setId(id);
    button.addClickHandler(this);
    return button;
  }

  private void sortMessages(final Comparator<Message> comparator, boolean sortUp) {
    if (sortUp) {
      Collections.sort(messages, comparator);
    } else {
      Collections.sort(messages, new Comparator<Message>() {
        public int compare(Message o1, Message o2) {
          return -comparator.compare(o1, o2);
        }
      });
    }
  }

  private void updatePagingButtons() {
    add1Button.setEnabled(table.canAddRows(1));
    remove1Button.setEnabled(table.canRemoveRows(1));
    add5Button.setEnabled(table.canAddRows(5));
    remove5Button.setEnabled(table.canRemoveRows(5));
    prevPageButton.setEnabled(table.hasPreviousPage());
    nextPageButton.setEnabled(table.hasNextPage());
  }
}
