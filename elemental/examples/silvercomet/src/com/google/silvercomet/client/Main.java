package com.google.silvercomet.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.client.Browser;
import elemental.css.CSSStyleDeclaration;
import elemental.css.CSSStyleDeclaration.Display;
import elemental.css.CSSStyleDeclaration.Unit;
import elemental.css.CSSStyleDeclaration.Visibility;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.html.InputElement;
import elemental.html.StyleElement;
import elemental.util.ArrayOf;
import elemental.util.ArrayOfInt;
import elemental.util.Collections;
import elemental.dom.*;

/**
 * All the view code that is fit to run.
 *
 * @author knorton@google.com (Kelly Norton)
 */
public class Main implements EntryPoint, Model.Listener, EventListener {

  /**
   * Access to all the relevant classnames and constants for the CSS selectors.
   */
  public static interface Css extends CssResource {
    String bar();

    String browserInfo();

    String count();

    String label();

    String marker();

    String moreResults();

    String result();

    String resultLeft();

    String resultName();

    String resultRight();

    String root();

    int rootHeight();

    int rootWidth();

    String tickMajor();

    String tickMinor();
  }

  /**
   * Bundles the CSS resources.
   */
  public interface Resources extends ClientBundle {
    @Source("silver-comet.gwt.css")
    Css css();
  }

  /**
   * A simple view to show a runner's finishing info.
   */
  private static class RunnerView {
    /**
     * Returns a string describing a runner's place & finishing time. Example:
     * 325th (1:49:11)
     */
    private static String infoString(Runner runner) {
      return placeString(runner.place()) + " (" + secondsToTime(runner.time(), true) + ")";
    }

    /**
     * Produces a english locale human readable places. Example: 1st, 2nd, 11th,
     * etc.
     */
    private static String placeString(int place) {
      final int tens = place / 10;
      final int ones = place % 10;
      if (ones == 1 && tens != 1) {
        return place + "st";
      } else if (ones == 2 && tens != 1) {
        return place + "nd";
      } else if (ones == 3 && tens != 1) {
        return place + "rd";
      } else {
        return place + "th";
      }
    }

    /**
     * Sets the current runner being displayed.
     */
    private static native void setRunner(Element element, Runner runner) /*-{
      element.currentRunner = runner;
    }-*/;

    /**
     * This is a hack for sure, but I claim it demonstrates your ability to
     * commit clever atrosities in JavaScript even while working in Java.
     */
    static native Runner getRunnerFromElement(Element element) /*-{
      return element.currentRunner || element.parentNode.currentRunner;
    }-*/;

    private final Element root;

    private final Element name;

    private final Element info;

    private final double secondsPerPixel;

    /**
     * Create a new view with a classname for the root element and a scaling
     * factor for translating along the timeline.
     */
    RunnerView(String rootClass, double secondsPerPixel) {
      this(rootClass, null, secondsPerPixel);
    }

    /**
     * Create a new view classnames for the root element and text elements and a
     * scaling factor for translating along the timeline.
     */
    RunnerView(String rootClass, String textClass, double secondsPerPixel) {
      root = div(rootClass);
      name = textClass == null ? div() : div(textClass);
      info = textClass == null ? div() : div(textClass);

      root.appendChild(name);
      root.appendChild(info);

      this.secondsPerPixel = secondsPerPixel;
    }

    /**
     * Returns the root element for the view. Generally used to add the view to
     * the DOM.
     */
    Element element() {
      return root;
    }

    /**
     * Make the view invisible.
     */
    void hide() {
      root.getStyle().setVisibility(Visibility.HIDDEN);
    }

    /**
     * Make the view visible.
     */
    void show() {
      root.getStyle().removeProperty("visibility");
    }

    /**
     * Update the view to show the info of a runner.
     */
    void update(Runner runner) {
      setRunner(root, runner);
      name.setTextContent(runner.name());
      info.setTextContent(infoString(runner));
      final int x = (int) ((double) runner.time() / (double) Model.SECONDS_PER_HISTOGRAM_BUCKET
          * secondsPerPixel);

      // TODO(knorton): This is actually wrong because it sets left on all
      // markers.
      root.getStyle().setLeft(x, Unit.PX);
      show();
    }
  }

  private static final int NUM_SEARCH_RESULTS = 6;

  /*
   * Get the correct classname based on the index of the result. This allows for
   * different classnames on the first and last item.
   */
  private static String cssClassForResultItem(Css css, int index) {
    if (index == 0) {
      return css.result() + " " + css.resultLeft();
    }

    if (index == NUM_SEARCH_RESULTS - 1) {
      return css.result() + " " + css.resultRight();
    }

    return css.result();
  }

  /**
   * Convenience method for creating a new div.
   */
  private static Element div() {
    return Browser.getDocument().createElement("div");
  }

  /**
   * Convenience method for creating a new div with a classname.
   */
  private static Element div(String className) {
    final Element e = div();
    e.setClassName(className);
    return e;
  }

  private static String getBrowserInfoString() {
    final Browser.Info info = Browser.getInfo();
    if (info.isWebKit()) {
      return "WebKit Browser";
    } else if (info.isGecko()) {
      return "Gecko Browser";
    }
    return "Unsupported Browser";
  }

  private static void injectStyles(Document document, String css) {
    final StyleElement style = (StyleElement)document.createElement("style");
    style.setTextContent(css);
    document.getHead().appendChild(style);
  }

  /**
   * Converts a integer to a {@link String}, prepending a zero if the string
   * representation is only 1 character in length.
   */
  private static String pad(int number) {
    return number > 9 ? "" + number : "0" + number;
  }

  /**
   * Converts the number of seconds to a more human readable finishing time.
   */
  private static String secondsToTime(int seconds, boolean includeSeconds) {
    final int hrs = seconds / 3600;
    final int mns = seconds / 60 - hrs * 60;
    if (includeSeconds) {
      final int scs = seconds - hrs * 3600 - mns * 60;
      return hrs + ":" + pad(mns) + ":" + pad(scs);
    }
    return hrs + ":" + pad(mns);
  }

  private final Css css = GWT.<Resources>create(Resources.class).css();

  private Element root;

  private InputElement search;

  private RunnerView marker;

  private Model model;

  private String lastQuery;

  private Element results;

  private ArrayOf<RunnerView> resultItems;

  private double xAxisScale;

  private Element moreResults;

  /**
   * Handles all DOM events for the app.
   */
  @Override
  public void handleEvent(Event evt) {
    final Element target = (Element) evt.getTarget();

    // Handle searches.
    if (target == search) {
	//      if (((KeyboardEvent)evt).getKeyCode() == 42) {
	//        clearSearch();
	//      } else {
        final String query = search.getValue();
        updateSearch(query == null ? "" : query.trim());
	//      }
      return;
    }

    // Handle clicks on search results.
    final Runner runner = RunnerView.getRunnerFromElement(target);
    if (runner != null) {
      marker.update(runner);
      clearSearch();
      return;
    }
  }

  /**
   * Indicates that the data failed to load.
   */
  @Override
  public void modelDidFailLoading(Model model) {
  }

  /**
   * Indicates that the model finished building the search indexes.
   */
  @Override
  public void modelDidFinishBuildingIndex(Model model) {
    // TODO(knorton): File crbug about readOnly not working properly.
    // search.setReadOnly(false);
    search.focus();
  }

  /**
   * Indicates that all data has been loaded into the model.
   */
  @Override
  public void modelDidFinishLoading(Model model) {
    final ArrayOfInt histogram = model.histogram();
    assert histogram.length() > 0 : "histogram is empty.";

    xAxisScale = (double) css.rootWidth() / (double) histogram.length();

    // Build the histogram graph.
    render();

    // Create the marker.
    marker = new RunnerView(css.marker(), xAxisScale);
    marker.hide();
    root.appendChild(marker.element());
    Browser.getDocument().getBody().getStyle().setOpacity(1.0);
  }

  /**
   * The main entry point for the application.
   */
  public void onModuleLoad() {
    injectStyles(Browser.getDocument(), css.getText());

    // TODO(knorton): Bad Elemental pattern.
    search = (InputElement) Browser.getDocument().getElementById("search");
    // TODO(knorton): File crbug about readOnly not working properly.
    // search.setReadOnly(true);
    search.addEventListener("change", this, false);
    search.addEventListener("keyup", this, false);
    search.addEventListener("keydown", this, false);

    root = (Element) Browser.getDocument().getElementById("c");
    root.setClassName(css.root());

    results = (Element)Browser.getDocument().getElementById("r");
    results.getStyle().setVisibility(Visibility.HIDDEN);
    results.addEventListener("click", this, false);

    // Browser info indicator.
    // TODO(knorton): Put this in a debug perm.
    final Element info = Browser.getDocument().getElementById("f");
    info.setClassName(css.browserInfo());
    info.setTextContent(getBrowserInfoString());
    
    model = new Model(this);
    model.load();
  }

  /**
   * Clear the search box and hide the result item list.
   */
  private void clearSearch() {
    search.setValue("");
    hideSearchResults();
  }

  /**
   * Ensure that the DOM for result items has been built and appended to the
   * DOM.
   */
  private void ensureSearchResultItems() {
    if (resultItems != null) {
      return;
    }

    resultItems = Collections.arrayOf();
    for (int i = 0; i < NUM_SEARCH_RESULTS; ++i) {
      final RunnerView marker =
          new RunnerView(cssClassForResultItem(css, i), css.resultName(), xAxisScale);
      results.appendChild(marker.element());
      resultItems.push(marker);
    }

    moreResults = div(css.moreResults());
    results.appendChild(moreResults);
    moreResults.getStyle().setDisplay(Display.NONE);
  }
  
  /**
   * Hide the search result items list.
   */
  private void hideSearchResults() {
    results.getStyle().setVisibility(Visibility.HIDDEN);
  }

  /**
   * Renders the bar graph.
   */
  private void render() {
    final ArrayOfInt histogram = model.histogram();

    final int padding = 2;
    final int topPadding = 50;

    final double dx = xAxisScale;
    final double dy = (double) css.rootHeight() / histogram.get(histogram.length() - 1);
    final double halfDx = dx / 2.0;

    // Render all bars.
    for (int i = 0, n = histogram.length(); i < n; ++i) {
      final int value = histogram.get(i);
      if (value == 0) continue;

      final int x = (int) (dx * i) + padding;
      final int h = (int) (dy * value) - topPadding;
      final int w = (int) dx - padding * 2;

      // Create the vertical bar.
      final Element bar = div(css.bar());
      final CSSStyleDeclaration barStyle = bar.getStyle();
      barStyle.setLeft(x, Unit.PX);
      barStyle.setBottom("0");
      barStyle.setHeight(h, Unit.PX);
      barStyle.setWidth(w, Unit.PX);

      // Add a count at the top.
      final Element count = div(css.count());
      count.setTextContent("" + value);

      bar.appendChild(count);
      root.appendChild(bar);
    }

    // Render labels
    for (int i = 0, n = histogram.length(); i <= n; ++i) {
      final int x = (int) (dx * i);
      final int w = (int) dx;

      final String time = secondsToTime(i * Model.SECONDS_PER_HISTOGRAM_BUCKET, false);

      final Element label = div(css.label());
      label.setTextContent(time);
      final CSSStyleDeclaration labelStyle = label.getStyle();
      labelStyle.setLeft(x - dx, Unit.PX);
      labelStyle.setWidth(w, Unit.PX);
      root.appendChild(label);

      // TODO(knorton): Heh, that's pretty trashy. I should fix that. :-)
      final Element tick =
          div(time.charAt(time.length() - 1) == '0' && time.charAt(time.length() - 2) == '0'
              ? css.tickMajor() : css.tickMinor());
      tick.getStyle().setLeft(x, Unit.PX);
      root.appendChild(tick);
    }
  }

  /**
   * Show the search result items list and update it with the specified list of
   * results.
   */
  private void showSearchResults(ArrayOf<Runner> runners) {
    ensureSearchResultItems();
    results.getStyle().removeProperty("visibility");
    for (int i = 0; i < NUM_SEARCH_RESULTS; ++i) {
      final RunnerView item = resultItems.get(i);
      if (i < runners.length()) {
        item.show();
        item.update(runners.get(i));
      } else {
        item.hide();
      }
    }

    if (runners.length() > NUM_SEARCH_RESULTS) {
      moreResults.setTextContent("+" + (runners.length() - NUM_SEARCH_RESULTS) + " more");
      moreResults.getStyle().removeProperty("display");
    } else {
      moreResults.getStyle().setDisplay(Display.NONE);
    }
  }

  /**
   * Update the search results visually for the specified query.
   */
  private void updateSearch(String query) {
    if (query.equals(lastQuery)) {
      return;
    }

    lastQuery = query;
    if (query.length() == 0) {
      hideSearchResults();
    }

    final ArrayOf<Runner> runners = model.search(query);
    if (runners == null) {
      hideSearchResults();
    } else if (runners.length() == 1) {
      hideSearchResults();
      marker.update(runners.get(0));
    } else {
      showSearchResults(runners);
    }
  }
}
