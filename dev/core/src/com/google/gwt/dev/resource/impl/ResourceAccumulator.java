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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

/**
 * Listens for and accumulates resources for a given root and PathPrefixSet.
 */
class ResourceAccumulator {

  private static final boolean WATCH_FILE_CHANGES_DEFAULT = Boolean.parseBoolean(
      System.getProperty("gwt.watchFileChanges", "true"));

  private Map<AbstractResource, ResourceResolution> resolutionsByResource;
  private Multimap<Path, Path> childPathsByParentPath;
  private Path rootDirectory;
  private WeakReference<PathPrefixSet> pathPrefixSetRef;
  private WatchService watchService;
  private boolean watchFileChanges = WATCH_FILE_CHANGES_DEFAULT;

  public ResourceAccumulator(Path rootDirectory, PathPrefixSet pathPrefixSet) {
    this.rootDirectory = rootDirectory;
    this.pathPrefixSetRef = new WeakReference<PathPrefixSet>(pathPrefixSet);
  }

  public boolean isWatchServiceActive() {
    return watchService != null;
  }

  /**
   * Make sure the resources associated with this directory and pathPrefixSet are up-to-date.
   */
  public void refreshResources() throws IOException {
    if (isWatchServiceActive()) {
      refresh();
    } else {
      fullRefresh();
    }
  }

  public Map<AbstractResource, ResourceResolution> getResources() {
    return resolutionsByResource;
  }

  public void shutdown() throws IOException {
    // watchService field is not cleared so any attempt to use this class after shutdown will fail.
    stopWatchService();
  }

  /**
   * Full refresh clears existing resources and watchers and does a clean refresh.
   */
  private void fullRefresh() throws IOException {
    resolutionsByResource = Maps.newIdentityHashMap();
    childPathsByParentPath = ArrayListMultimap.create();

    maybeInitializeWatchService();

    onNewDirectory(rootDirectory);
  }

  private void maybeInitializeWatchService() throws IOException {
    if (watchFileChanges) {
      stopWatchService();
      try {
        watchService = FileSystems.getDefault().newWatchService();
      } catch (IOException e) {
        watchFileChanges = false;
      }
    }
  }

  private void stopWatchService() throws IOException {
    if (watchService != null) {
      watchService.close();
    }
  }

  private void refresh() throws IOException {
    while (true) {
      WatchKey watchKey = watchService.poll();
      if (watchKey == null) {
        return;
      }

      Path parentDir = (Path) watchKey.watchable();

      for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
        WatchEvent.Kind<?> eventKind = watchEvent.kind();
        if (eventKind == OVERFLOW) {
          fullRefresh();
          return;
        }

        Path child = parentDir.resolve((Path) watchEvent.context());
        if (eventKind == ENTRY_CREATE) {
          onNewPath(child);
        } else if (eventKind == ENTRY_DELETE) {
          onRemovedPath(child);
        }
      }

      watchKey.reset();
    }
  }

  private void onNewPath(Path path) throws IOException {
    try {
      if (Files.isHidden(path)) {
        return;
      }

      if (Files.isRegularFile(path)) {
        onNewFile(path);
      } else {
        onNewDirectory(path);
      }
    } catch (NoSuchFileException | FileNotFoundException e) {
      // ignore: might happen, e.g., for temporary files used for "safe writes" by IDEs/editors
    }
  }

  private void onNewDirectory(Path directory) throws IOException {
    String relativePath = getRelativePath(directory);
    if (!relativePath.isEmpty() && !getPathPrefixSet().includesDirectory(relativePath)) {
      return;
    }

    if (watchService != null) {
      // Start watching the directory.
      directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path child : stream) {
        childPathsByParentPath.put(directory, child);
        onNewPath(child);
      }
    }
  }

  private void onNewFile(Path file) {
    FileResource resource = toFileResource(file);
    ResourceResolution resourceResolution = getPathPrefixSet().includesResource(resource.getPath());
    if (resourceResolution != null) {
      resolutionsByResource.put(resource, resourceResolution);
    }
  }

  private void onRemovedPath(Path path) {
    resolutionsByResource.remove(toFileResource(path));
    for (Path child : childPathsByParentPath.get(path)) {
      onRemovedPath(child);
    }
  }

  private FileResource toFileResource(Path path) {
    String relativePath = getRelativePath(path);
    return FileResource.of(relativePath, path.toFile());
  }

  private String getRelativePath(Path directory) {
    // Make sure that paths are exposed "Unix" style to PathPrefixSet.
    return rootDirectory.relativize(directory).toString().replace(File.separator, "/");
  }

  private PathPrefixSet getPathPrefixSet() {
    PathPrefixSet pathPrefixSet = pathPrefixSetRef.get();
    // pathPrefixSet can never be null as the life span of this class is bound by it.
    assert pathPrefixSet != null;
    return pathPrefixSet;
  }
}
