# Copyright 2014 Google Inc.
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
#   The standard GWT libraries.

# Google owns the copyright
licenses(["unencumbered"])

package(
    default_visibility = [
        "//third_party/java/gwt:__subpackages__",
        "//third_party/java_src/gwt:__subpackages__",
    ],
)

subinclude("//third_party/java_src/gwt:build-macros")

# The classes in gwt-user.jar that GWT applications can depend on.
# We want the final output of this NOT to depend on gwt-dev,
# though it is built with that dependency, because user code can and should
# depend on this without also touching the compiler and shell internals.
AugmentedJar(
    name = "gwt-user-classes-src",
    srcs = [],
    added_jars = [
        ":libgwt-user-bare.jar",
        "//third_party/java_src/gwt/svn/tools:user_jars",
    ],
    added_roots = [
        "src",
        "super",
    ],
    constraints = [],
    dojarjar = 1,
    exclude_glob = [
        "**/package.html",
        "**/package-info.java",
        "src/com/google/gwt/junit/**",
        "super/com/google/gwt/junit/**",
    ],
    java_library_name = "gwt-user-classes",
    output_name = "gwt-user-classes-src.jar",
    wrap_javalibrary = 0,
)

# The gwt-user classes for GWT internal use only.
# They must be jarjar-ed and dependencies stripped before exposing to google3.
java_library(
    name = "gwt-user-bare",
    srcs = glob(
        [
            "src/**/*.java",
            "super/com/google/gwt/emul/javaemul/internal/**/*.java",
        ],
        exclude = [
            "src/com/google/gwt/junit/**/*.java",  # see gwt-testing
            "**/EmulatedCharset.java",
            "**/RunStyleSelenium.java",
        ],
    ),
    deps = [
        "//third_party/java_src/gwt/svn/tools:dev_deps",
        "//third_party/java_src/gwt/svn/tools:servlet_api_3",
        "//third_party/java_src/gwt/svn/tools:user_deps",
        "//third_party/java_src/gwt/svn/trunk/dev:compiler.standalone.super",
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-bare",
    ],
)

# This target is used by a java_plugin over in java/gwt/BUILD. It's
# a pretty simple annotation processor, so in order to keep it from being a
# build bottleneck, it's simply built from scratch.
java_library(
    name = "requestfactory-apt",
    srcs = glob([
        "src/com/google/web/bindery/requestfactory/apt/*.java",
    ]) + [
        "src/com/google/web/bindery/requestfactory/shared/JsonRpcProxy.java",
        "src/com/google/web/bindery/requestfactory/shared/JsonRpcService.java",
        "src/com/google/web/bindery/requestfactory/shared/Locator.java",
        "src/com/google/web/bindery/requestfactory/shared/Service.java",
        "src/com/google/web/bindery/requestfactory/shared/ServiceLocator.java",
        "src/com/google/web/bindery/requestfactory/shared/ServiceName.java",
        "src/com/google/web/bindery/requestfactory/shared/SkipInterfaceValidation.java",
        "src/com/google/web/bindery/requestfactory/shared/ProxyFor.java",
        "src/com/google/web/bindery/requestfactory/shared/ProxyForName.java",
        "src/com/google/web/bindery/requestfactory/vm/impl/ClassComparator.java",
        "src/com/google/web/bindery/requestfactory/vm/impl/Deobfuscator.java",
        "src/com/google/web/bindery/requestfactory/vm/impl/OperationData.java",
        "src/com/google/web/bindery/requestfactory/vm/impl/OperationKey.java",
        "src/com/google/gwt/core/shared/GwtIncompatible.java",
        "src/com/google/gwt/user/server/Base64Utils.java",
    ],
    deps = [
        "//third_party/java_src/gwt/svn/trunk/dev:requestfactory-apt-deps",
    ],
)

# Packages just AutoBeans source into a jar with correct java root.
AugmentedJar(
    name = "autobean-classes-src",
    srcs = glob(["src/com/google/web/bindery/autobean/**/*.java"]),
    added_root_globs = {"src": ["src/com/google/web/bindery/autobean/**/*.java"]},
    # We only need the Java classes so don't wait for the rest of :gwt-dev.
    build_deps = [
        "//third_party/java_src/gwt:gwt-user-full",
        "//third_party/java_src/gwt/svn/tools:dev_deps",
        "//third_party/java_src/gwt/svn/trunk/dev:compiler.standalone.super",
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-bare",
    ],
    constraints = [],
    dojarjar = 1,
    exclude_glob = [
        "**/package.html",
        "**/package-info.java",
    ],
    java_library_name = "autobean-classes",
    output_name = "autobean-classes-src.jar",
    wrap_javalibrary = 0,
)

# Repackages the AutoBeans.gwt.xml file with correct java root by placing it in
# "java/".
genrule(
    name = "autobean-gwt-xml",
    srcs = [":src/com/google/web/bindery/autobean/AutoBean.gwt.xml"],
    outs = ["java/com/google/web/bindery/autobean/AutoBean.gwt.xml"],
    cmd = ("cp $(location :src/com/google/web/bindery/autobean/AutoBean.gwt.xml) " +
           "$(location java/com/google/web/bindery/autobean/AutoBean.gwt.xml)"),
)

java_import(
    name = "autobean_import",
    constraints = [
        "gwt",
        "public",
    ],
    jars = [":autobean-classes-src.jar"],
    deps = [
        "//third_party/java_src/gwt:gwt-dev",
        "//third_party/java_src/gwt:gwt-user-full",
    ],
)

java_library(
    name = "autobean",
    constraints = [
        "gwt",
        "public",
    ],
    gwtxml = "java/com/google/web/bindery/autobean/AutoBean.gwt.xml",
    exports = [":autobean_import"],
)

# Packages just IncrementalBuilder test resource into a jar with correct java root.
AugmentedJar(
    name = "incremental-build-system-test-res",
    srcs = [],
    added_root_globs = {
        "test": [
            "test/com/google/gwt/dev/testdata/incrementalbuildsystem/**/*",
        ],
    },
    # We only need the Java classes so don't wait for the rest of :gwt-dev.
    build_deps = [
        "//third_party/java_src/gwt:gwt-user-full",
        "//third_party/java_src/gwt/svn/tools:dev_deps",
        "//third_party/java_src/gwt/svn/trunk/dev:compiler.standalone.super",
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-classes",
    ],
    constraints = [],
    dojarjar = 1,
    exclude_glob = [
        "**/package.html",
        "**/package-info.java",
    ],
    output_name = "incremental-build-system-test-res.jar",
    wrap_javalibrary = 0,
)

# See //third_party/java/gwt:regexp
java_library(
    name = "jvm_regexp",
    srcs = glob(
        ["src/com/google/gwt/regexp/shared/*.java"],
        exclude = ["**/package-info.java"],
    ),
    compatible_with = ["//buildenv/target:android"],
    constraints = ["android"],
)

# See //third_party/java/gwt:hibernate-validation-support
java_library(
    name = "hibernate-validation-support",
    srcs = glob(
        [
            "src/org/hibernate/validator/**/*_CustomFieldSerializer.java",
            "src/org/hibernate/validator/engine/ValidationSupport.java",
        ],
        exclude = ["**/package-info.java"],
    ),
    deps = [
        "//third_party/java/hibernate:validator",
        "//third_party/java/javax_validation",
        "//third_party/java_src/gwt:gwt-servlet",
    ],
)

# The indirection through libgwt-servlet-impl.jar is needed so that Java symbols in the outputs of
# this package are distributed in one form (through the class jars, as AugmentedJar does)
# This prevents them appearing them on classpath as both sources and classes, which confuses javac
# For example see b/3033925
java_import(
    name = "gwt-servlet-internal",
    jars = [":libgwt-servlet-impl.jar"],
)

java_library(
    name = "gwt-servlet-impl",
    srcs = glob(
        [
            "src/com/google/gwt/**/server/**/*.java",
            "src/com/google/gwt/**/shared/**/*.java",
            "src/com/google/web/bindery/**/server/**/*.java",
            "src/com/google/web/bindery/**/shared/**/*.java",
            "src/com/google/web/bindery/**/vm/**/*.java",
            "src/com/google/web/bindery/event/shared/**/*.java",
            "src/javax/validation/**/*_CustomFieldSerializer.java",
            # Despite the "client" designation, these packages are
            # also reached by server RPC code:
            "src/com/google/gwt/canvas/dom/client/*.java",
            "src/com/google/gwt/debug/client/DebugInfo.java",
            "src/com/google/gwt/media/dom/client/*.java",
            "src/com/google/gwt/user/client/rpc/core/**/*CustomFieldSerializer*.java",
            "src/com/google/gwt/user/client/rpc/core/java/util/*.java",
            "src/com/google/gwt/user/client/rpc/SerializedTypeViolationException.java",
            "src/com/google/gwt/user/client/rpc/IncompatibleRemoteServiceException.java",
            "src/com/google/gwt/user/client/rpc/IsSerializable.java",
            "src/com/google/gwt/user/client/rpc/GwtTransient.java",
            "src/com/google/gwt/user/client/rpc/RemoteService.java",
            "src/com/google/gwt/user/client/rpc/AsyncCallback.java",
            "src/com/google/gwt/user/client/rpc/RemoteServiceRelativePath.java",
            "src/com/google/gwt/user/client/rpc/RpcToken.java",
            "src/com/google/gwt/user/client/rpc/RpcTokenException.java",
            "src/com/google/gwt/user/client/rpc/RpcTokenExceptionHandler.java",
            "src/com/google/gwt/user/client/rpc/impl/AbstractSerialization*.java",
            "src/com/google/gwt/user/client/rpc/RpcRequestBuilder.java",
            "src/com/google/gwt/user/client/rpc/ServiceDefTarget.java",
            "src/com/google/gwt/user/client/rpc/InvocationException.java",
            "src/com/google/gwt/user/client/rpc/CustomFieldSerializer.java",
            "src/com/google/gwt/user/client/rpc/XsrfProtectedService.java",
            "src/com/google/gwt/user/client/rpc/XsrfToken.java",
            "src/com/google/gwt/user/client/rpc/XsrfTokenService.java",
            "src/com/google/gwt/http/client/RequestBuilder.java",
            "src/com/google/gwt/http/client/Request.java",
            "src/com/google/gwt/http/client/Header.java",
            "src/com/google/gwt/http/client/Response.java",
            "src/com/google/gwt/http/client/ResponseImpl.java",
            "src/com/google/gwt/http/client/RequestException.java",
            "src/com/google/gwt/http/client/RequestPermissionException.java",
            "src/com/google/gwt/http/client/RequestTimeoutException.java",
            "src/com/google/gwt/http/client/RequestCallback.java",
            "src/com/google/gwt/http/client/StringValidator.java",
            "src/com/google/gwt/http/client/URL.java",
            "src/com/google/gwt/http/client/UrlBuilder.java",
            "src/com/google/gwt/json/client/JSONArray.java",
            "src/com/google/gwt/json/client/JSONBoolean.java",
            "src/com/google/gwt/json/client/JSONException.java",
            "src/com/google/gwt/json/client/JSONNull.java",
            "src/com/google/gwt/json/client/JSONNumber.java",
            "src/com/google/gwt/json/client/JSONObject.java",
            "src/com/google/gwt/json/client/JSONString.java",
            "src/com/google/gwt/json/client/JSONValue.java",
            "src/com/google/gwt/core/client/JsonUtils.java",
            "src/com/google/gwt/core/client/impl/WeakMapping.java",
            "src/com/google/gwt/xhr/client/ReadyStateChangeHandler.java",
            "src/com/google/gwt/xhr/client/XMLHttpRequest.java",
            "src/com/google/gwt/user/client/Timer.java",
            "src/com/google/gwt/user/client/rpc/SerializationException.java",
            "src/com/google/gwt/user/client/rpc/SerializationStreamReader.java",
            "src/com/google/gwt/user/client/rpc/SerializationStreamWriter.java",
            "src/com/google/gwt/user/client/rpc/impl/ClientSerializationStreamWriter.java",
            "src/com/google/gwt/user/client/rpc/impl/Serializer.java",
            # ...which need:
            "src/com/google/gwt/core/client/GWT.java",
            "src/com/google/gwt/core/client/JavaScriptException.java",
            "src/com/google/gwt/core/client/JavaScriptObject.java",
            "src/com/google/gwt/core/client/JsArray.java",
            "src/com/google/gwt/core/client/JsDate.java",
            "src/com/google/gwt/core/client/Duration.java",
            "src/com/google/gwt/core/client/Scheduler.java",
            "src/com/google/gwt/user/client/Window.java",
            "src/com/google/gwt/user/client/Window*.java",
            "src/com/google/gwt/user/client/BaseListenerWrapper.java",
            "src/com/google/gwt/user/client/Element.java",
            "src/com/google/gwt/user/client/Event.java",
            "src/com/google/gwt/user/client/EventListener.java",
            "src/com/google/gwt/user/client/EventPreview.java",
            "src/com/google/gwt/user/client/NativePreviewEvent.java",
            "src/com/google/gwt/user/client/History.java",
            "src/com/google/gwt/user/client/impl/HistoryImpl.java",
            "src/com/google/gwt/user/client/impl/DomImpl.java",
            "src/com/google/gwt/user/client/HistoryListener.java",
            "src/com/google/gwt/user/client/Cookies.java",
            "src/com/google/gwt/core/client/impl/JavaScriptExceptionBase.java",
            "src/com/google/gwt/user/client/ui/AcceptsOneWidget.java",
            "src/com/google/gwt/user/client/ui/Widget.java",
            "src/com/google/gwt/user/client/ui/IsWidget.java",
            "src/com/google/gwt/user/client/ui/HasOneWidget.java",
            "src/com/google/gwt/user/client/ui/HasVisibility.java",
            "src/com/google/gwt/user/client/ui/HasWidgets.java",
            "src/com/google/gwt/user/client/ui/HasText.java",
            "src/com/google/gwt/user/client/ui/AbsolutePanel.java",
            "src/com/google/gwt/user/client/ui/ComplexPanel.java",
            "src/com/google/gwt/user/client/ui/IndexedPanel.java",
            "src/com/google/gwt/user/client/ui/InsertPanel.java",
            "src/com/google/gwt/user/client/ui/Panel.java",
            "src/com/google/gwt/user/client/ui/AttachDetachException.java",
            "src/com/google/gwt/user/client/ui/UIObject.java",
            "src/com/google/gwt/user/client/ui/PotentialElement.java",
            "src/com/google/gwt/user/client/ui/RootPanel.java",
            "src/com/google/gwt/user/client/ui/WidgetCollection.java",
            "src/com/google/gwt/event/dom/client/HasNativeEvent.java",
            "src/com/google/gwt/event/dom/client/PrivateMap.java",
            "src/com/google/gwt/event/dom/client/DomEvent.java",
            "src/com/google/gwt/event/dom/client/KeyEvent.java",
            "src/com/google/gwt/event/dom/client/KeyCodeEvent.java",
            "src/com/google/gwt/event/dom/client/KeyCodes.java",
            "src/com/google/gwt/event/dom/client/KeyUpEvent.java",
            "src/com/google/gwt/event/dom/client/HasKeyUpHandlers.java",
            "src/com/google/gwt/event/dom/client/KeyUpHandler.java",
            "src/com/google/gwt/user/client/DOM.java",
            "src/com/google/gwt/user/client/impl/DOMImpl.java",
            "src/com/google/gwt/user/client/impl/WindowImpl.java",
            "src/com/google/gwt/dom/client/*.java",
            "src/com/google/gwt/core/client/impl/Impl.java",
            "src/com/google/gwt/core/client/impl/StackTraceCreator.java",
            "src/com/google/gwt/core/client/impl/SchedulerImpl.java",
            "src/com/google/gwt/core/client/impl/AsyncFragmentLoader.java",
            "src/com/google/gwt/core/client/impl/OnSuccessExecutor.java",
            "src/com/google/gwt/core/client/impl/SynchronousOnSuccessExecutor.java",
            "src/com/google/gwt/core/client/JsArrayInteger.java",
            "src/com/google/gwt/core/client/JsArrayString.java",
            "src/com/google/gwt/core/client/RunAsyncCallback.java",
            "src/com/google/gwt/i18n/client/constants/*.java",
            "src/com/google/gwt/i18n/client/impl/cldr/*.java",
            "src/com/google/gwt/i18n/client/impl/plurals/*.java",
            "src/com/google/gwt/i18n/client/impl/*.java",
            "src/com/google/gwt/i18n/client/*.java",
            "src/com/google/gwt/i18n/rebind/keygen/*.java",
            "super/com/google/gwt/emul/javaemul/internal/ArrayHelper.java",
            "super/com/google/gwt/emul/javaemul/internal/ArrayStamper.java",
            # And it's also sensible to want for serialization:
            "src/com/google/gwt/user/client/ui/SuggestOracle.java",
        ],
        exclude = [
            "**/junit/**",
            "**/package-info.java",
            "src/com/google/web/bindery/requestfactory/server/RequestFactoryJarExtractor.java",
        ],
    ),
    deps = [
        "//third_party/java/servlet/servlet_api",
        "//third_party/java_src/gwt/svn/tools:servlet_deps",
        "//third_party/java_src/gwt/svn/trunk/dev:compiler.standalone.super",
        "//third_party/java_src/gwt/svn/trunk/dev:servlet-deps",
    ],
)

# The minimum requirements for GWT-RPC serializable object definition.
# Includes support for data transfer objects (with default or custom field serialization)
# but not server interfaces, services, client code, etc.
#
# CAUTION: most of google ends up depending on this, via common/base:base-gwt
# and common/collect:collect-gwt.  You really don't want it to be big!
java_library(
    name = "gwt-dto-serializers",
    srcs = [
        "src/com/google/gwt/user/client/rpc/CustomFieldSerializer.java",
        "src/com/google/gwt/user/client/rpc/GwtTransient.java",
        "src/com/google/gwt/user/client/rpc/IsSerializable.java",
        "src/com/google/gwt/user/client/rpc/SerializationException.java",
        "src/com/google/gwt/user/client/rpc/SerializationStreamReader.java",
        "src/com/google/gwt/user/client/rpc/SerializationStreamWriter.java",
        "src/com/google/gwt/user/client/rpc/core/java/util/Collections.java",
    ] + glob([
        "src/com/google/gwt/user/client/rpc/core/java/util/*_CustomFieldSerializer*.java",
    ]),
    compatible_with = ["//buildenv/target:android"],
    constraints = [
        "android",
        "gwt",
        "public",
    ],
    gwtxml = "Gwt-dto-serializers.gwt.xml",
)

# GWT JsInterop annotations in a separate library used by hybrid app projects
java_library(
    name = "gwt-jsinterop-annotations",
    srcs = glob(["src/jsinterop/**/*.java"]),
    compatible_with = ["//buildenv/target:android"],
    constraints = [
        "android",
    ],
)

# Creates gwt-testing.jar, which contains GWTTestCase and its infrastructure.
# (The jar depends on gwt-user.jar but the dependency isn't declared.
# Instead we assume it will be added by a wrapper rule.)
AugmentedJar(
    name = "gwt-testing",
    srcs = [],
    added_jars = [
        ":libgwt-testing-bare.jar",
    ],
    added_root_globs = {
        "src": ["src/com/google/gwt/junit/**"],
        "super": ["super/com/google/gwt/junit/**"],
    },
    dojarjar = 1,
    exclude_glob = [
        "**/package.html",
        "**/package-info.java",
        "**/RunStyleSelenium.java",
    ],
)

# The gwt-testing classes for GWT internal use only.
# They must be jarjar-ed and dependencies stripped before exposing to google3.
java_library(
    name = "gwt-testing-bare",
    testonly = 1,
    srcs = glob(
        ["src/com/google/gwt/junit/**/*.java"],
        exclude = [
            "**/super/**",
            "**/RunStyleSelenium.java",
        ],
    ),
    deps = [
        ":gwt-user-bare",
        "//third_party/java/junit",
        "//third_party/java_src/gwt/svn/tools:dev_deps",
        "//third_party/java_src/gwt/svn/tools:servlet_api_3",
        "//third_party/java_src/gwt/svn/trunk/dev:compiler.standalone.super",
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-bare",
    ],
)

# A minimal library for annotations that might be used by client code.
java_library(
    name = "gwt-user-interop-annotations",
    srcs = [
        "src/com/google/gwt/core/shared/GwtIncompatible.java",
    ],
    compatible_with = ["//buildenv/target:android"],
    constraints = [
        "android",
    ],
)

# === TESTS ===

AugmentedJar(
    name = "user-test-code",
    testonly = 1,
    srcs = glob(
        [
            "test/**/*.java",
            "test_i18n_bar/**/*.java",
        ],
        exclude = [
            "test/**/super/**",
            "test/**/tck/**",
            "test/**/java8/**",
            "test/**/*Java8Suite.java",
            "**/RunStyleSeleniumTest.java",
        ],
    ),
    added_roots = [
        "test",
        "test-super",
        "test_i18n_bar",
    ],
    # For build_deps we should use pre-jarjar dependencies (-bare)
    build_deps = [
        ":gwt-user-bare",
        ":gwt-testing-bare",
        "//third_party/java_src/gwt/svn/tools:dev_deps",
        "//third_party/java_src/gwt/svn/tools:user_deps",
        "//third_party/java_src/gwt/svn/tools:servlet_api_3",
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-bare",
        "//third_party/java_src/gwt/svn/trunk/dev:dev-test-code-bare",
        # for @GwtScriptOnly
        "//third_party/java_src/gwt/svn/trunk/dev:compiler.standalone.super",
    ],
    constraints = [
        "gwt",
        "public",
    ],
    dojarjar = 1,
    export_deps = [
        "//third_party/java_src/gwt:gwt-dev",  # for the post-jarjar names
        "//third_party/java_src/gwt:gwt-testing",
        "//third_party/java_src/gwt/svn/trunk/dev:dev-test-code",
    ],
    deps = [
        "//third_party/java/jsr305_annotations",
        "//third_party/java/junit",
        "//third_party/java_src/gwt/svn/tools:testtool_deps",
    ],
)

# This target should only be dependent from platform tests that care about tck.
# This target is separated from user-test-code as its deps pulls in an old version of Guice which
# breaks the Webdriver for Android.
AugmentedJar(
    name = "tck-test-code",
    testonly = 1,
    srcs = glob(["test/**/tck/**/*.java"]),
    # For build_deps we should use pre-jarjar dependencies (-bare)
    build_deps = [
        ":gwt-user-bare",
        ":gwt-testing-bare",
        "//third_party/java_src/gwt/svn/tools:user_deps",
        "//third_party/java_src/gwt/svn/trunk/dev:gwt-dev-bare",
        "//third_party/java_src/gwt/svn/trunk/dev:dev-test-code-bare",
    ],
    constraints = [
        "gwt",
        "public",
    ],
    dojarjar = 1,
    deps = [
        "//third_party/java/junit",
        "//third_party/java_src/gwt/svn/tools:validator_deps",
        "//third_party/java_src/gwt/svn/tools:validator_tck_deps",
    ],
)

filegroup(
    name = "browser_suite_files",
    srcs = glob(
        ["test/**/*Suite.java"],
        exclude = [
            "test/**/ConstraintDefinitionsGwtSuite.java",
            "test/**/*JreSuite.java",
            "test/**/*Java8Suite.java",
        ],
    ),
)

filegroup(
    name = "jsinterop_suite_files",
    srcs = glob(["test/**/*JsInteropSuite.java"]),
)

filegroup(
    name = "nobrowser_suite_files",
    srcs = glob(["test/**/*JreSuite.java"]),
)

filegroup(
    name = "dev_suite_files",
    srcs = glob(
        ["test/**/*Suite.java"],
        exclude = [
            "test/**/ConstraintDefinitionsGwtSuite.java",
            "test/**/*CompilerSuite.java",
            "test/**/RunAsyncSuite.java",
            "test/**/*JreSuite.java",
            "test/**/*JsInteropSuite.java",
            "test/**/*Java8Suite.java",
        ],
    ),
)

# === ant and checkstyle ===

# Files to be excluded from ant tests.
ANT_EXCLUDES = [
    "**/.git/**",
    "**/.svn/**",
    # in case eclipse output is mixed into source
    "**/war/WEB-INF/classes/**",
    "**/webapp/WEB-INF/classes/**",
    "**/bin/**",
    # emacs backup and autosave files
    "**/*~",
    "**/#*",
    "test_i18n_dollar/**",
]

filegroup(
    name = "checkstyle_files",
    srcs = glob(
        ["*/com/google/gwt/**/*.java"],
        exclude = ANT_EXCLUDES,
        exclude_directories = 1,
    ),
)

filegroup(
    name = "checkapi_files",
    srcs = glob(
        [
            "**/*.java",
            "**/gwt*userApi.conf",
        ],
        exclude = ANT_EXCLUDES,
        exclude_directories = 1,
    ),
)

filegroup(
    name = "ant_files",
    srcs = glob(
        [
            "build.xml",
            "javadoc/**/*",
            "src/**/*",
            "style/**/*",
            "super/**/*",
            "test/**/*",
            "test_i18n_bar/**/*",
            "test-super/**/*",
        ],
        exclude = ANT_EXCLUDES,
    ),
)

filegroup(
    name = "java_emul",
    srcs = glob(["super/com/google/gwt/emul/java/**/*.java"]),
    visibility = ["//third_party/java_src/j2cl:__subpackages__"],
)

filegroup(
    name = "java_emul_internal",
    srcs = glob(["super/com/google/gwt/emul/javaemul/**/*.java"]),
    visibility = ["//third_party/java_src/j2cl:__subpackages__"],
)

filegroup(
    name = "java_emul_test",
    srcs = glob(
        [
            "test/com/google/gwt/emultest/**/*.java",
            "test/com/google/gwt/testing/TestUtils.java",
            "test/org/apache/commons/**/*.java",
        ],
        exclude = [
            "test/**/java8/**",
            "test/**/*Java8Suite.java",
            "**/package-info.java",
        ],
    ),
    visibility = ["//third_party/java_src/j2cl:__subpackages__"],
)
