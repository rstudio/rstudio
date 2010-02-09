package com.google.gwt.list.client;

import com.google.gwt.cells.client.Cell;
import com.google.gwt.cells.client.Mutator;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.list.shared.ListEvent;
import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListModel;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

public class PagingTableListView2<T> extends Widget {

  public abstract static class Column<T, C> {
    private final Cell<C> cell;
    private Mutator<T, C> mutator;

    public Column(Cell<C> cell) {
      this.cell = cell;
    }

    public void onBrowserEvent(Element elem, final T object, NativeEvent event) {
      cell.onBrowserEvent(elem, getValue(object), event, new Mutator<C, C>() {
        public void mutate(C unused, C after) {
          mutator.mutate(object, after);
        }
      });
    }

    public void render(T object, StringBuilder sb) {
      cell.render(getValue(object), sb);
    }

    public void setMutator(Mutator<T, C> mutator) {
      this.mutator = mutator;
    }

    protected abstract C getValue(T object);

    protected Cell<C> getCell() {
      return cell;
    }
  }

  private int pageSize;
  private int numPages;
  private ListRegistration listReg;
  protected int curPage;
  private int totalSize;
  private List<Column<T, ?>> columns = new ArrayList<Column<T, ?>>();
  private ArrayList<T> data = new ArrayList<T>();

  public PagingTableListView2(ListModel<T> listModel, final int pageSize) {
    this.pageSize = pageSize;
    setElement(Document.get().createTableElement());
    createRows();

    // TODO: total hack.
    sinkEvents(Event.MOUSEEVENTS | Event.KEYEVENTS);

    // Attach to the list model.
    listReg = listModel.addListHandler(new ListHandler<T>() {
      public void onDataChanged(ListEvent<T> event) {
        render(event.getStart(), event.getLength(), event.getValues());
      }

      public void onSizeChanged(SizeChangeEvent event) {
        totalSize = event.getSize();
        if (totalSize <= 0) {
          numPages = 0;
        } else {
          numPages = 1 + (totalSize - 1) / pageSize;
        }
        setPage(curPage);
      }
    });
    listReg.setRangeOfInterest(0, pageSize);
  }

  // TODO: remove(Column)
  public void addColumn(Column<T, ?> col) {
    columns.add(col);
    createRows();
    setPage(curPage); // TODO: better way to refresh?
  }

  @Override
  public void onBrowserEvent(Event event) {
    EventTarget target = event.getEventTarget();
    Node node = Node.as(target);
    while (node != null) {
      if (Element.is(node)) {
        Element elem = Element.as(node);

        // TODO: We need is() implementations in all Element subclasses.
        if ("td".equalsIgnoreCase(elem.getTagName())) {
          TableCellElement td = TableCellElement.as(elem);
          TableRowElement tr = TableRowElement.as(td.getParentElement());

          // TODO: row/col assertions.
          int row = tr.getRowIndex(), col = td.getCellIndex();
          T value = data.get(row);
          Column<T, ?> column = columns.get(col);
          column.onBrowserEvent(elem, value, event);
          break;
        }
      }

      node = node.getParentNode();
    }
  }

  protected void render(int start, int length, List<T> values) {
    TableElement table = getElement().cast();
    int numCols = columns.size();
    int pageStart = curPage * pageSize;

    for (int r = start; r < start + length; ++r) {
      TableRowElement row = table.getRows().getItem(r - pageStart);
      T q = values.get(r - start);

      data.set(r - pageStart, q);

      for (int c = 0; c < numCols; ++c) {
        TableCellElement cell = row.getCells().getItem(c);
        StringBuilder sb = new StringBuilder();
        columns.get(c).render(q, sb);
        cell.setInnerHTML(sb.toString());

        // TODO: really total hack!
        Element child = cell.getFirstChildElement();
        if (child != null) {
          Event.sinkEvents(child, Event.ONCHANGE | Event.ONFOCUS | Event.ONBLUR);
        }
      }
    }
  }

  /**
   * Get the current page.
   * 
   * @return the current page
   */
  public int getPage() {
    return curPage;
  }

  /**
   * Set the current visible page.
   * 
   * @param page the page index
   */
  public void setPage(int page) {
    int newPage = Math.min(page, numPages - 1);
    newPage = Math.max(0, newPage);

    // Update the text showing the page number.
    updatePageText(newPage);

    // Early exit if we are already on the right page.
    if (curPage != newPage) {
      curPage = newPage;
      listReg.setRangeOfInterest(curPage * pageSize, pageSize);
    }

    updateRowVisibility();
  }

  private void updateRowVisibility() {
    int visible = Math.min(pageSize, totalSize - curPage * pageSize);

    TableElement table = getElement().cast();
    for (int r = 0; r < pageSize; ++r) {
      Style rowStyle = table.getRows().getItem(r).getStyle();
      if (r < visible) {
        rowStyle.clearDisplay();
      } else {
        rowStyle.setDisplay(Display.NONE);
      }
    }
  }

  /**
   * Set the number of rows per page.
   * 
   * @param pageSize the page size
   */
  public void setPageSize(int pageSize) {
    if (this.pageSize == pageSize) {
      return;
    }
    this.pageSize = pageSize;
    curPage = -1;
    setPage(curPage);
  }

  /**
   * Update the text that shows the current page.
   * 
   * @param page the current page
   */
  private void updatePageText(int page) {
//    if (table.getRowCount() > 0) {
//      int row = table.getRowCount() - 1;
//      table.setText(row, 1, "Page " + (page + 1) + " of " + numPages);
//    }
  }

  private void createRows() {
    TableElement table = getElement().cast();
    int numCols = columns.size();

    for (int r = 0; r < pageSize; ++r) {
      TableRowElement row = table.insertRow(r);

      // TODO: use cloneNode() to make this even faster.
      for (int c = 0; c < numCols; ++c) {
        row.insertCell(c);
      }
    }

    data.ensureCapacity(pageSize);
    while (data.size() < pageSize) {
      data.add(null);
    }
    // TODO: shrink as well.
  }
}
