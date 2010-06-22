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

package com.google.gwt.logging.client;

import java.util.logging.Level;

/**
 * An interface for a set of classes which are used to choose the default
 * logging level. This allows the user to configure the default level in the
 * gwt.xml file.
 */
public interface DefaultLevel {
  /**
   * Returns Level.ALL as the default level.
   */
  public class All implements DefaultLevel {
    public Level getLevel() {
      return Level.ALL;
    }
  }

  /**
   * Returns Level.CONFIG as the default level.
   */
  public class Config implements DefaultLevel {
    public Level getLevel() {
      return Level.CONFIG;
    }
  }

  /**
   * Returns Level.FINE as the default level.
   */
  public class Fine implements DefaultLevel {
    public Level getLevel() {
      return Level.FINE;
    }
  }

  /**
   * Returns Level.FINER as the default level.
   */
  public class Finer implements DefaultLevel {
    public Level getLevel() {
      return Level.FINER;
    }
  }

  /**
   * Returns Level.FINEST as the default level.
   */
  public class Finest implements DefaultLevel {
    public Level getLevel() {
      return Level.FINEST;
    }
  }

  /**
   * Returns Level.INFO as the default level.
   */
  public class Info implements DefaultLevel {
    public Level getLevel() {
      return Level.INFO;
    }
  }

  /**
   * Returns Level.SEVERE as the default level.
   */
  public class Severe implements DefaultLevel {
    public Level getLevel() {
      return Level.SEVERE;
    }
  }

  /**
   * Returns Level.WARNING as the default level.
   */
  public class Warning implements DefaultLevel {
    public Level getLevel() {
      return Level.WARNING;
    }
  }

  Level getLevel();

}
