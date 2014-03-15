# Copyright 2012 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

# Description:
#   Sample apps for GWT. (Mirrored to svn.)

# Google owns the copyright
licenses(["unencumbered"])

package(
    default_visibility = ["//third_party/java_src/gwt:__pkg__"],
)

subinclude("//third_party/java_src/gwt:build-macros")

package_group(
    name = "showcase_friends",
    packages = ["//javatests/com/google/gwt/tourguide/..."],
)

# Files in this directory for running ant
# Note: ant_test.sh replaces this build.xml file with its own stub.
filegroup(
    name = "ant",
    srcs = glob([
        "build.xml",
        "**/*.java",
    ]),
)

Zip(
    name = "hello-src.jar",
    srcs = glob([
        "hello/src/**/*.java",
        "hello/src/**/*.xml",
    ]),
    root = "hello/src",
    visibility = ["//third_party/java_src/gwt/svn/trunk/dev:__pkg__"],
)

# Allow friends to import the showcase app as a GWT module
gwt_module(
    name = "showcase_module",
    extra_inherits = ["com.google.gwt.sample.showcase.Showcase"],
    rename_to = "showcase",
    visibility = [":showcase_friends"],
    deps = [":showcase_lib"],
)

gwt_application(
    name = "showcase_app",
    module_target = ":showcase_module",
)

java_library(
    name = "showcase_lib",
    srcs = [":showcase.jar"],
)

AugmentedJar(
    name = "showcase",
    srcs = glob(
        ["showcase/src/**/*.java"],
        exclude_directories = 1,
    ),
    added_roots = ["showcase/src"],
    deps = [
        "//third_party/java_src/gwt",
        "//third_party/java_src/gwt:gwt-dev",
    ],
)

# Provides the contents of the showcase war directory as a handy zip file
# (Doesn't include appengine-web.xml so you can replace it.)
Zip(
    name = "showcase-wardir.zip",
    srcs = glob(
        ["showcase/war/**"],
        exclude = glob(["showcase/war/**/*~"]) + [
            "showcase/war/WEB-INF/appengine-web.xml",
        ],
    ),
    root = "showcase/war",
    visibility = [":showcase_friends"],
)
