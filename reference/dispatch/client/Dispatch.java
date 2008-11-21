package kellegous.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

public class Dispatch implements EntryPoint {
  private static native Event createMockEvent() /*-{
    return { type: "click" };
  }-*/;

  private static double[] runTrial(int numberOfObservers,
      int numberOfIterations, int numberOfSamples) {
    final double[] results = new double[numberOfSamples];
    final EventListener subject = Subject.create(numberOfObservers);
    final Event event = createMockEvent();
    for (int j = 0; j < numberOfSamples; ++j) {
      final Duration d = new Duration();
      for (int i = 0; i < numberOfIterations; ++i) {
        subject.onBrowserEvent(event);
      }
      results[j] = d.elapsedMillis();
    }
    return results;
  }

  private static native void schedule(Command command) /*-{
    $wnd.setTimeout(function() {
      command.@com.google.gwt.user.client.Command::execute()();
    }, 0);
  }-*/;

  // t-distribution for p = 0.05 (used to compute 95% confidence intervals).
  // This table is based at df = 2.
  private final static double[] TDIST = new double[] {
      4.3027, 3.1824, 2.7765, 2.5706, 2.4469, 2.3646, 2.3060, 2.2622, 2.2281,
      2.2010, 2.1788, 2.1604, 2.1448, 2.1315, 2.1199, 2.1098, 2.1009, 2.0930,
      2.0860, 2.0796, 2.0739, 2.0687, 2.0639, 2.0595, 2.0555, 2.0518, 2.0484,
      2.0452, 2.0423, 2.0395, 2.0369, 2.0345, 2.0322, 2.0301, 2.0281, 2.0262,
      2.0244, 2.0227, 2.0211, 2.0195, 2.0181, 2.0167, 2.0154, 2.0141, 2.0129,
      2.0117, 2.0106, 2.0096, 2.0086, 2.0076, 2.0066, 2.0057, 2.0049, 2.0040,
      2.0032, 2.0025, 2.0017, 2.0010, 2.0003, 1.9996, 1.9990, 1.9983, 1.9977,
      1.9971, 1.9966, 1.9960, 1.9955, 1.9949, 1.9944, 1.9939, 1.9935, 1.9930,
      1.9925, 1.9921, 1.9917, 1.9913, 1.9908, 1.9905, 1.9901, 1.9897, 1.9893,
      1.9890, 1.9886, 1.9883, 1.9879, 1.9876, 1.9873, 1.9870, 1.9867, 1.9864,
      1.9861, 1.9858, 1.9855, 1.9852, 1.9850, 1.9847, 1.9845, 1.9842, 1.9840};

  private static double computeT(int df) {
    return TDIST[df - 2];
  }

  private static double computeMean(double[] s) {
    double sum = 0.0;
    final int n = s.length;
    for (int i = 0; i < n; ++i) {
      sum += s[i];
    }
    return sum / n;
  }

  private static double computeStandardError(double[] data, double mean) {
    final int n = data.length;
    double sum = 0.0;
    for (int i = 0; i < n; ++i) {
      final double d = data[i] - mean;
      sum += d * d;
    }

    return Math.sqrt(sum / n) / Math.sqrt(n);
  }

  private static class Stats {
    private final double mean, upper, lower;

    Stats(double[] data) {
      mean = computeMean(data);
      final double error = computeStandardError(data, mean);
      final double t = computeT(data.length - 1);
      upper = mean + t * error;
      lower = mean - t * error;
    }

    @Override
    public String toString() {
      return mean + ", " + lower + ", " + upper;
    }
  }

  public static class Runner {
    private int n;
    private final int max, numIterations, numSamples;
    private Stats[] results;

    Runner(int min, int max, int numIterations, int numSamples) {
      n = min;
      this.max = max;
      this.numIterations = numIterations;
      this.numSamples = numSamples;
      results = new Stats[max - min + 1];
    }

    void finish() {
      final Document document = Document.get();
      final DivElement root = document.createDivElement();
      document.getBody().appendChild(root);
      for (int i = 0, n = results.length; i < n; ++i) {
        final DivElement div = document.createDivElement();
        root.appendChild(div);
        div.setInnerText("" + results[i].toString());
      }
    }

    void next() {
      schedule(new Command() {
        public void execute() {
          final double[] results = runTrial(n, numIterations, numSamples);
          Runner.this.results[n] = new Stats(results);
          if (++n <= max) {
            next();
          } else {
            finish();
          }
        }
      });
    }

    void run() {
      next();
    }
  }

  public void onModuleLoad() {
    // Don't run this in hosted mode.
    if (GWT.isScript()) {
      new Runner(0, 10, 10000, 20).run();
    }
  }
}
