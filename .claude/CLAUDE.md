# RStudio

These instructions apply to the RStudio open-source IDE repository.


## Building RStudio

These sections describe how to build RStudio from the command line.


### Frontend

To build the front-end, you can use:

    cd src/gwt && ant javac

This will be necessary if you've modified any scripts in the `src/gwt` directory. Note that if you modify any JavaScript components, e.g. in `src/gwt/acesupport`, you will also need to run:

    cd src/gwt && ant acesupport


### Backend

To build the backend components, you can use:

    cd build && cmake --build . --target all

If the build directory does not yet exist, you can create and configure the project with:

    mkdir build
    cd build
    cmake -DCMAKE_EXPORT_COMPILE_COMMANDS=1 ..


### Desktop Integration

To build the Electron application / desktop components, you can use:

    cd src/node/desktop && npm run package


## Testing

### C++ Tests

C++ tests use Google Test. Run them with:

    ./rstudio-tests --scope <scope> --filter <pattern>

where `<scope>` is one of `core`, `rserver`, `rsession`, or `r`.


### GWT Tests

    cd src/gwt && ant unittest


### Desktop (Electron) Tests

    cd src/node/desktop && npm test


## Code Style

### C++

C++ code is formatted with clang-format. The configuration is in `src/cpp/.clang-format`. Key settings:

- 3-space indentation
- Allman brace style
- No tabs

### TypeScript (Desktop)

TypeScript code uses ESLint and Prettier. From `src/node/desktop`:

    npm run lint      # Check for issues
    npm run format    # Auto-format code


## Code Structure

Broadly speaking, RStudio is split into three different components -- the front-end / user interface, the desktop integration, and the backend / server components.

- The user interface uses a mixture of Google GWT and JavaScript, in `src/gwt`.
- The desktop integration lives in `src/node/desktop`, as an Electron application.
- The backend and server components live in `src/cpp`, and are primarily authored in C++ and R.


## Schema-Driven Code Generation

Several parts of the codebase are generated from JSON schema files.


### User Preferences and State

User preferences and UI state are declaratively defined in:

    src/cpp/session/resources/schema/user-prefs-schema.json
    src/cpp/session/resources/schema/user-state-schema.json

After modifying either file, regenerate with:

    Rscript scripts/generate-prefs.R


### Server and Session Options

Command-line options for the server and session binaries are defined in:

    src/cpp/server/server-options.json
    src/cpp/session/session-options.json

After modifying either file, regenerate with:

    Rscript scripts/generate-options.R


## Commands

RStudio defines its commands as part of a file at:

    src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml

When a command is added here, a stub will also need to be added to the file at:

    src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.java


### Command Handlers

Handlers for commands can be implemented almost anywhere in the code base. A command handler has the format:

    @Handler
    void on<Command>() { ... }

where `<Command>` is the name of the command, but with the first letter capitalized.


Any class which implements handlers in this form will also need to create a CommandBinder; e.g.

    interface Binder extends CommandBinder<Commands, ThisClass> {}

And then the constructor should have the form:

    @Inject
    class ThisClass(Commands commands, Binder binder)
    {
        binder.bind(commands, this);
    }

possibly with other injected parameters.


## Pull Requests

When generating a pull request that fixes a known issue, please ensure the pull request body includes:

    ## Intent

    Addresses <issue>.

