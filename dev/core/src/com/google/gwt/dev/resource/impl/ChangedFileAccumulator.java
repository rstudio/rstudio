/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.resource.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Listens for and accumulates file changes in a recursive directory tree even when new files are
 * changed in new directories.
 * <p>
 * Since it contains an internal thread it can't be garbage collected until it has been explicitly
 * shutdown.
 * <p>
 * Multiple instances can listen to the same directories at the same time.
 */
public class ChangedFileAccumulator {

  /**
   * A runnable that polls for watch events (in a blocking manner). File changes are recorded
   * and listeners are attached to new directories.
   */
  private class WatchEventPoller implements Runnable {
    @Override
    public void run() {
      try {
        while (true) {
          WatchKey watchKey;
          try {
            watchKey = watchService.take();
          } catch (InterruptedException e) {
            // Shutdown has been requested.
            return;
          } catch (ClosedWatchServiceException e) {
            // Shutdown has been requested.
            return;
          }

          synchronized (changedFiles) {
            Path containingDirectory = pathsByWatchKey.get(watchKey);

            for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
              WatchEvent.Kind<?> eventKind = watchEvent.kind();

              if (eventKind == StandardWatchEventKinds.OVERFLOW) {
                setFailed(
                    new RuntimeException("Changes occurred faster than they could be recorded."));
                return;
              }

              if (!(watchEvent.context() instanceof Path)) {
                continue;
              }

              Path changedFileName = (Path) watchEvent.context();
              Path changedPath = containingDirectory.resolve(changedFileName);

              // Maybe listen to newly created directories.
              if (eventKind == StandardWatchEventKinds.ENTRY_CREATE
                  && Files.isDirectory(changedPath)) {
                // A new directory and some contained files can be created faster than watches can
                // be attached to new directories. So it is necessary to look for files in what are
                // believed to be "newly created" directories and consider those files changed.
                try {
                  recursivelyRegisterListeners(
                      changedPath, true /* considerPreexistingFilesChanged */);
                } catch (IOException e) {
                  setFailed(e);
                  return;
                }
              }

              // Record changed files.
              changedFiles.add(changedPath.toFile().getAbsoluteFile());
            }

            // Ensures that future change events will be seen.
            if (!watchKey.reset()) {
              pathsByWatchKey.remove(watchKey);
            }
          }
        }
      } catch (RuntimeException e) {
        setFailed(e);
      }
    }
  }

  private final Set<File> changedFiles = Collections.synchronizedSet(Sets.<File> newHashSet());
  private Thread changePollerThread;
  private Exception changePollerException;
  private final Map<WatchKey, Path> pathsByWatchKey = Maps.newHashMap();
  private final WatchService watchService;

  public ChangedFileAccumulator(Path rootDirectory) throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
    recursivelyRegisterListeners(rootDirectory, false /* considerPreexistingFilesChanged */);
    startChangePoller();
  }

  /**
   * Returns a sorted copy of the list of files that have changed since the last time changed files
   * were requested.
   */
  public List<File> getAndClearChangedFiles() throws ExecutionException {
    if (isFailed()) {
      throw new ExecutionException(changePollerException);
    }

    synchronized (changedFiles) {
      List<File> sortedChangedFiles = Lists.newArrayList(changedFiles);
      changedFiles.clear();
      Collections.sort(sortedChangedFiles);
      return sortedChangedFiles;
    }
  }

  public boolean isFailed() {
    return changePollerException != null;
  }

  public void shutdown() {
    changePollerThread.interrupt();
    try {
      watchService.close();
    } catch (IOException e) {
      changePollerException = e;
      // Am already trying to stop listening, there's nothing to be done about this failure.
    }
    changedFiles.clear();
    pathsByWatchKey.clear();
  }

  private void recursivelyRegisterListeners(final Path directory,
      final boolean considerPreexistingFilesChanged) throws IOException {
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path currentDirectory, BasicFileAttributes attrs)
          throws IOException {
        WatchKey watchKey = currentDirectory.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY);

        // If the recursive directory scan has gone in a loop because of symlinks.
        if (pathsByWatchKey.containsKey(watchKey)) {
          return FileVisitResult.SKIP_SUBTREE;
        }

        pathsByWatchKey.put(watchKey, currentDirectory);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (attrs.isSymbolicLink()) {
          recursivelyRegisterListeners(Files.readSymbolicLink(file),
              considerPreexistingFilesChanged);
          return FileVisitResult.CONTINUE;
        }
        if (considerPreexistingFilesChanged) {
          changedFiles.add(file.toFile());
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private void setFailed(Exception caughtException) {
    this.changePollerException = caughtException;
    shutdown();
  }

  private void startChangePoller() {
    changePollerThread = new Thread(new WatchEventPoller());
    // Don't prevent JVM shutdown.
    changePollerThread.setDaemon(true);
    changePollerThread.start();
  }
}
