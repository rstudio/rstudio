## GWT

  GWT is the official open source project for GWT releases 2.5 and onwards.

  In this document you have some quick instructions to build the SDK from
  source code and to run its tests.

  For a more detailed documentation visit our [web site](http://gwtproject.org).
  If you are interested in contributing with the project, please read the
  [Making GWT better](http://gwtproject.gquery.org/makinggwtbetter.html)
  section.

### Building the GWT SDK:

 - In order to build GWT, `java` and `ant` are required in your system.

 - Optional: if you want to compile elemental you need
   `python` and `g++` installed.

 - You need the [gwt-tools](https://google-web-toolkit.googlecode.com/svn/tools/)
   checked out and up-to-date, and it will be placed
   by default at `../tools`. You can override the default
   location using the GWT_TOOLS environment variable or passing `-Dgwt.tools=`
   argument to ant.

   _Note: that you need `svn` to checkout `gwt-tools`_

 - To create the SDK distribution files run:

   `$ ant clean elemental dist-dev`

   or if you don't have `python` and `g++` just run

   `$ ant clean dist-dev`

   Then you will get all `.jar` files in the folder `build/lib` and
   the redistributable file will be: `build/dist/gwt-0.0.0.zip`

   if you want to specify a different version number run:

   `$ ant elemental clean dist-dev -Dgwt.version=x.x.x`

 - To compile everything including examples you have to run

   `$ ant clean elemental dist`

### How to verify GWT code conventions:

 - In GWT we have some conventions so as all code written
   by contributors look similar being easier to review.

 - After you make any modification, run this command to compile
   everything including tests, to check APIs, and to verify code style.
   It shouldn't take longer than 3-4 minutes.

   `$ ant compile.tests apicheck checkstyle -Dprecompile.disable=true`

### How to run GWT tests

 - Previously to run any test you have to set some environment variables
   to guarantee that they are run in the same conditions for all
   developers.

   In a _Unix_ like platform you can use the `export` command:

   `$ export TZ=America/Los_Angeles ANT_OPTS=-Dfile.encoding=UTF-8`

   But in _Windows™_ you have to set the time-zone in your control panel, and
   the environment variables using the command `set`.

 - Finally you can run all test suites with the following command, but be
   prepared because it could take hours, and probably it would fail because
   of timeouts, etc.

   `$ ant test`

 - Thus, you might want to run only certain tests so as you can
   focus on checking the modifications you are working on.

   GWT build scripts use specific ant tasks and a bunch of system
   properties listed in the following table to specify which tests
   to run and how.

   For instance to run the task `test` in the module `user` you
   have to change to the `user` folder and run `ant` with the task
   as argument, adding any other property with the `-D` flag:

   `$ ( cd user && ant test -Dtest.emma.htmlunit.disable=true ; cd .. )`

    Module         | Task                   | Property to skip
    -------------- | ---------------------- | ----------------
    dev            | test                   | test.dev.disable
    codeserver     | test                   | test.codeserver.disable
    user           | test                   | test.user.disable
    user           | test.nongwt            | test.nongwt.disable
    user           | test.dev.htmlunit      | test.dev.htmlunit.disable
    user           | test.web.htmlunit      | test.web.htmlunit.disable
    user           | test.draft.htmlunit    | test.draft.htmlunit.disable
    user           | test.nometa.htmlunit   | test.nometa.htmlunit.disable
    user           | test.emma.htmlunit     | test.emma.htmlunit.disable
    user           | test.coverage.htmlunit | test.coverage.htmlunit.disable
    user           | test.dev.selenium      | test.dev.selenium.disable
    user           | test.web.selenium      | test.web.selenium.disable
    user           | test.draft.selenium    | test.draft.selenium.disable
    user           | test.nometa.selenium   | test.nometa.selenium.disable
    user           | test.emma.selenium     | test.emma.selenium.disable
    requestfactory | test                   |
    elemental      | test                   |
    elemental      | test.nongwt            |
    elemental      | test.dev.htmlunit      |
    elemental      | test.web.htmlunit      |
    tools          | test                   |

   Additionally you can utilize some variables to filter which test to run in each task:

    Module         | Task                                  | Properties                           | Default
    ---------------|---------------------------------------|--------------------------------------|-------------------
    dev/core       | test                                  | gwt.junit.testcase.dev.core.includes | `**/com/google/**/*Test.class`
                   |                                       | gwt.junit.testcase.dev.core.excludes |
    user           | test                                  | gwt.junit.testcase.includes          | `**/*Suite.class`
    user           | test.nongwt                           | gwt.nongwt.testcase.includes         | `**/*JreSuite.class`
                   |                                       | gwt.nongwt.testcase.excludes         |
    user           | test.web.* test.draft.* test.nometa.* | gwt.junit.testcase.web.includes      | `**/*Suite.class`
                   |                                       | gwt.junit.testcase.web.excludes      | `**/*JsInteropSuite.class,**/*JreSuite.class,***/OptimizedOnly*`
    user           | test.dev.* test.emma.*                | gwt.junit.testcase.dev.includes      | `**/*Suite.class`
                   |                                       | gwt.junit.testcase.dev.excludes      | `**/*JsInteropSuite.class,**/*JreSuite.class,***/OptimizedOnly*`

### Examples

 - Run all tests in dev

   `$ ( cd dev && ant test ; cd .. )`

    _Note: that the last `cd ..' is only needed in Windows._

 - There is another option to do the same but without changing to the
   module folder. We have to specify the module as the ant task, and
   the task as a target argument.

   `$ ant dev -Dtarget=test`

 - Run all tests in codeserver

   `$ ( cd dev/codeserver && ant test )`

    or

   `$ ant codeserver -Dtarget=test -Dtest.dev.disable=true`

    _Note: that we disable dev tests because code server depends on dev
    and we don`t want to run its tests._

 - Run all tests in elemental:

   `$ ( cd elemental && ant test.nongwt )`

    or

   `$ ant elemental -Dtarget=test -Dtest.dev.disable=true -Dtest.user.disable=true`

    _Note: that we have to disable dev and user tests because elemental
    depends on both._

 - Run all tests in tools

   `$ ant tools -Dtarget=test -Dtest.dev.disable=true -Dtest.user.disable=true`

 - Run only the JsniRefTest in dev

   ```
   $ ant dev -Dtarget=test \
       -Dgwt.junit.testcase.dev.core.includes="**/JsniRefTest.class"
   ```

 - Run a couple of tests in dev

   ```
   $ ant dev -Dtarget=test \
       -Dgwt.junit.testcase.dev.core.includes="**/JsniRefTest.class,**/JsParserTest.class"
   ```

   _Note: that you have to use regular expressions separated by comma to
   select the test classes to execute._

 - Run all Jre tests in user, they should take not longer than 3min.
   We have two ways to run them. Although the second case is more
   complex it is here to know how disable properties work.

   `$ ( cd user && ant test.nongwt )`

      or

   ```
   $ ant user -Dtarget=test -Dtest.dev.disable=true \
          -Dtest.dev.htmlunit.disable=true \
          -Dtest.web.htmlunit.disable=true \
          -Dtest.coverage.htmlunit.disable=true \
          -Dtest.dev.selenium.disable=true \
          -Dtest.draft.htmlunit.disable=true \
          -Dtest.draft.selenium.disable=true \
          -Dtest.emma.htmlunit.disable=true \
          -Dtest.emma.selenium.disable=true \
          -Dtest.nometa.htmlunit.disable=true \
          -Dtest.nometa.selenium.disable=true \
          -Dtest.web.selenium.disable=true
   ```

    _Note: that we have to set all disable variables but `test.nongwt.disable`_

 - Run certain Jre tests in the user module.

   `$ ( cd user && ant test.nongwt -Dgwt.nongwt.testcase.includes="**/I18NJreSuite.class" )`

 - Run all GWT tests in user using htmlunit in dev mode.

   `$ ( cd user && ant test.dev.htmlunit )`

