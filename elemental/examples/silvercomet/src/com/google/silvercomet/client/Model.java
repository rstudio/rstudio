// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.silvercomet.client;

import com.google.gwt.xhr.client.XMLHttpRequest;

import elemental.client.Browser;
import elemental.html.Window;
import elemental.util.ArrayOf;
import elemental.util.ArrayOfInt;
import elemental.util.ArrayOfString;
import elemental.util.MapFromStringTo;
import elemental.json.Json;
import elemental.js.util.StringUtil;
import elemental.js.util.Xhr;
import elemental.util.CanCompare;
import elemental.dom.TimeoutHandler;
import elemental.util.Collections;

/**
 * A very simple data model for the application.
 *
 * @author knorton@google.com (Kelly Norton)
 */
public class Model implements Xhr.Callback {

  /**
   * A listener to receive callbacks on model events.
   */
  public interface Listener {
    void modelDidFailLoading(Model model);

    void modelDidFinishBuildingIndex(Model model);

    void modelDidFinishLoading(Model model);
  }

  private static final String DATA_URL = "results.json";

  public static final int SECONDS_PER_HISTOGRAM_BUCKET = 600;

  /**
   * Compute a histogram with number of finishers per bucket of time where the
   * size of the bucket is indicated by <code>seconds</code>.
   */
  private static ArrayOfInt computeHistogram(ArrayOf<Runner> runners, int seconds) {
    final ArrayOfInt hist = Collections.arrayOfInt();

    for (int i = 0, n = runners.length(); i < n; ++i) {
      int index = runners.get(i).time() / seconds;
      hist.set(index, hist.isSet(index) ? hist.get(index) + 1 : 1);
    }

    int sum = 0;
    for (int i = 0, n = hist.length(); i < n; ++i) {
      if (hist.isSet(i)) {
        sum += hist.get(i);
      }
      hist.set(i, sum);
    }

    return hist;
  }

  /**
   * Sorts the list of runners by {@link Runner#time()} and updates their places
   * accordingly.
   */
  private static ArrayOf<Runner> normalize(ArrayOf<Runner> runners) {
    // Sort by time() which is based on bib time.
    runners.sort(new CanCompare<Runner>() {
      @Override
      public int compare(Runner a, Runner b) {
        return a.time() - b.time();
      }
    });

    // Update the runner's new place.
    for (int i = 0, n = runners.length(); i < n; ++i) {
      runners.get(i).setPlace(i + 1);
    }

    return runners;
  }

  /**
   * Update the model's index with all possible prefixes of the search key.
   */
  private static void updateIndexForAllPrefixes(
      MapFromStringTo<ArrayOf<Runner>> index, String key, Runner runner) {
    assert key.length() > 0 : "key.length must be > 0.";

    for (int i = 1, n = key.length(); i <= n; ++i) {
      final String prefix = key.substring(0, i);
      if (!index.hasKey(prefix)) {
        index.put(prefix, Collections.<Runner>arrayOf());
      }

      final ArrayOf<Runner> values = index.get(prefix);
      // Do not add the same runner twice.
      if (values.get(values.length() - 1) != runner) {
        index.get(prefix).push(runner);
      }
    }
  }

  private final Listener listener;

  private ArrayOfInt histogram = null;

  private MapFromStringTo<ArrayOf<Runner>> index = Collections.mapFromStringTo();

  /**
   * Create a new model.
   */
  public Model(Listener listener) {
    this.listener = listener;
  }

  /**
   * Get a reference to the models histogram.
   */
  public ArrayOfInt histogram() {
    return histogram;
  }

  /**
   * Load the remote data into the model.
   */
  public void load() {
    Xhr.get(DATA_URL, this);
  }

  /**
   * Called if the XHR fails to load data from there server.
   */
  @Override
  public void onFail(XMLHttpRequest xhr) {
    listener.modelDidFailLoading(this);
  }

  /**
   * Called when XHR successfully loads data from the server.
   */
  @Override
  public void onSuccess(XMLHttpRequest xhr) {
    update((ArrayOf<Runner>)Json.parse(xhr.getResponseText()));
    listener.modelDidFinishLoading(this);
  }

  /**
   * Performs a search and returns the list of runners that match.
   */
  public ArrayOf<Runner> search(String query) {
    return index.get(query);
  }

  /**
   * Update the model's internal data from an list of runners coming from the
   * server.
   */
  private void update(ArrayOf<Runner> data) {
    // Sort & mutate source data.
    final ArrayOf<Runner> runners = normalize(data);

    // Update indexes later.
    Browser.getWindow().setTimeout(new TimeoutHandler() {
      @Override
      public void onTimeoutHandler() {
        for (int i = 0, n = runners.length(); i < n; ++i) {
          final Runner runner = runners.get(i);

          final String name = runner.name().toLowerCase();
          updateIndexForAllPrefixes(index, name, runner);
          final ArrayOfString words = StringUtil.split(name, " ");
          for (int j = 0, m = words.length(); j < m; ++j) {
            updateIndexForAllPrefixes(index, words.get(j), runner);
          }

          updateIndexForAllPrefixes(index, "" + runner.place(), runner);
          updateIndexForAllPrefixes(index, "" + runner.bibNumber(), runner);
        }
        listener.modelDidFinishBuildingIndex(Model.this);
      }
    }, 0);

    // Compute histogram.
    final ArrayOfInt histogram = computeHistogram(runners, SECONDS_PER_HISTOGRAM_BUCKET);

    // Update fields.
    this.histogram = histogram;
  }
}
