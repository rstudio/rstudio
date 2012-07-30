/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;

import java.util.concurrent.TimeUnit;

/**
 * Logs progress of an operation, attempting to calculate an estimated time remaining (ETR) for
 * the entire operation for each log entry, assuming that progress is approximately linear.
 */
class ProgressLogger {

  /**
   * An interface for retrieving nanosecond times, useable ONLY for deltas.
   */
  interface NanoSource {

    /**
     * Returns a number of nanoseconds. Subtracting the results of two invocations of this method
     * yields the approximate number of nanoseconds between the invocations.
     */
    long getNanos();
  }

  // Constant for dividing nanosecond time intervals by to get second time intervals. It is a double
  // in order to allow fractional seconds.
  private static final long NANOSECONDS_IN_SECOND = TimeUnit.SECONDS.toNanos(1);
  // The minimum amount of progress that must be made before an ETR is calculated (where 1.0 is
  // 100%). If this is too low, then early, inaccurate estimates will be shown.
  private static final double ETR_ACCURACY_THRESHOLD = 0.05;
  // The number of seconds before which no log is shown. This reduces log spam for things with
  // fast progress.
  private static final int LOG_BLACKOUT_PERIOD = 5;
  // The number of nanoseconds before which no log is shown.
  private static final long LOG_BLACKOUT_PERIOD_NANOS = LOG_BLACKOUT_PERIOD * NANOSECONDS_IN_SECOND;

  private final TreeLogger logger;
  private final TreeLogger.Type logLevel;
  private final int maxProgressUnits;
  private final int percentageIncrement;
  private final NanoSource nanoSource;

  private long startNanos = 0;
  private boolean timerStarted = false;
  private long nextPercentage = 0;

  /**
   * Creates a ProgressLogger that logs messages with log level {@code logLevel} to {@code logger}.
   * It will log only every {@code percentageIncrement} percent of progress, where progress is
   * measured from 0 to {@code maxProgressUnits}. Gets nano time offsets for ETR calculations from
   * {@link System#nanoTime()}.
   */
  ProgressLogger(TreeLogger logger, TreeLogger.Type logLevel, int maxProgressUnits,
      int percentageIncrement) {
    this.logger = logger;
    this.logLevel = logLevel;
    this.maxProgressUnits = maxProgressUnits;
    this.percentageIncrement = percentageIncrement;
    this.nanoSource = new NanoSource() {
      @Override
      public long getNanos() {
        return System.nanoTime();
      }
    };
  }

  /**
   * Returns true if {@link #startTimer()} has been called, and false otherwise.
   */
  boolean isTimerStarted() {
    return timerStarted;
  }

  /**
   * Sets the start time against which ETR calculations are made.
   */
  void startTimer() {
    startNanos = nanoSource.getNanos();
    timerStarted = true;
  }

  /**
   * Notifies the ProgressLogger that progress has reached {@code progressUnits}. This may cause
   * (depending on the ProgressLogger's settings) the emission of a log message in the form
   * "[PERCENT]% complete (ETR: [ESTIMATED TIME REMAINING] seconds)" in most cases, or
   * "[PERCENT]% complete (ETR: ?)" if not enough progress has been made to make a good estimate of
   * remaining time.
   *
   * Successive calls to this method should provide monotonically increasing values of
   * {@code progressUnits}. It does not support backwards progress.
   */
  void updateProgress(int progressUnits) {
    // Only do the percentage calculation if the result would be logged.
    if (!logger.isLoggable(logLevel)) {
      return;
    }

    if (!timerStarted) {
      throw new IllegalStateException("#updateProgress() called before #startTimer().");
    }

    double progress = progressUnits / (double) maxProgressUnits;
    double currentPercentage = 100 * progress;
    long elapsedNanos = nanoSource.getNanos() - startNanos;

    // Display the percent complete if progress has reached the
    // next percentage increment, or if progress is 100%.
    if (currentPercentage < nextPercentage && progressUnits != maxProgressUnits) {
      return;
    }

    // Don't log anything if < LOG_BLACKOUT_PERIOD_NANOS nanoseconds have elapsed.
    if (elapsedNanos < LOG_BLACKOUT_PERIOD_NANOS) {
      return;
    }

    // Show the largest integer multiple of the percentage increment that is less than the
    // actual percentage.
    long displayedPercentage =
        percentageIncrement * Math.round(Math.floor(currentPercentage / percentageIncrement));
    // If progress is 100%, just show 100%.
    if (progressUnits == maxProgressUnits) {
      displayedPercentage = 100;
    }
    
    // Only attempt to estimate a time remaining if we have a reasonable amount of data to go
    // on. Otherwise the estimates are wildly misleading.
    if (progress >= ETR_ACCURACY_THRESHOLD) {
      // Do linear extrapolation to estimate the amount of time remaining.
      double estimatedTotalNanos = elapsedNanos / progress;
      double estimatedNanosRemaining = estimatedTotalNanos - elapsedNanos;
      // Convert nanos to seconds.
      double estimatedSecondsRemaining = estimatedNanosRemaining / (double) NANOSECONDS_IN_SECOND;

      logger.log(logLevel, String.format("%d%% complete (ETR: %d seconds)",
          displayedPercentage, Math.round(estimatedSecondsRemaining)));
    } else {
      // Not enough information to estimate time remaining.
      logger.log(logLevel, String.format("%d%% complete (ETR: ?)", displayedPercentage));
    }
    nextPercentage += percentageIncrement;
  }
}
