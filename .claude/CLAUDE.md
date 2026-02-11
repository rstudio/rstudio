# RStudio

These instructions apply to the RStudio open-source IDE repository.


## Code Structure

Broadly speaking, RStudio is split into three different components -- the front-end / user interface, the desktop integration, and the backend / server components.

- The user interface uses a mixture of Google GWT and JavaScript, in `src/gwt`.
- The desktop integration lives in `src/node/desktop`, as an Electron application.
- The backend and server components live in `src/cpp`, and are primarily authored in C++ and R.


### Backend Layout (`src/cpp`)

    src/cpp/
    ├── core/              # Foundation libraries (http, json, system, text, etc.)
    ├── shared_core/       # Shared between server and session
    ├── server_core/       # Server-specific core utilities
    ├── r/                 # R integration layer
    │   └── R/             # Core R functions (Api.R, Tools.R, etc.)
    ├── session/           # Session management (main IDE backend)
    │   ├── modules/       # Feature modules (one per feature)
    │   ├── include/       # Public headers
    │   └── resources/     # Schemas, templates, static resources
    ├── server/            # RStudio Server (auth, db, launcher)
    └── tests/             # C++ and automation tests


### Frontend Layout (`src/gwt`)

    src/gwt/src/org/rstudio/
    ├── core/client/               # Core GWT utilities (widgets, dom, events, js interop)
    └── studio/client/
        ├── application/           # Application lifecycle
        ├── common/                # Shared components
        ├── server/                # Server communication interfaces
        └── workbench/
            ├── commands/          # Command infrastructure (Commands.cmd.xml)
            ├── events/            # Event definitions
            ├── model/             # Data models
            ├── ui/                # UI components
            └── views/             # Feature views (one per pane)
                ├── source/        # Source editor (largest subsystem)
                ├── console/       # R console
                ├── environment/   # Environment pane
                ├── files/         # Files pane
                ├── plots/         # Plots pane
                ├── packages/      # Packages pane
                ├── help/          # Help pane
                ├── terminal/      # Terminal pane
                ├── chat/          # AI chat pane
                ├── vcs/           # Git/SVN integration
                ├── connections/   # Database connections
                ├── jobs/          # Background jobs
                └── ...            # buildtools, viewer, tutorial, presentation, etc.


### R Code Locations

R code lives in several places and serves different purposes:

- `src/cpp/r/R/` -- core R functions shipped with RStudio (Api.R, Tools.R, etc.)
- `src/cpp/session/modules/*.R` -- R-side logic for session modules (e.g. SessionChat.R)
- `src/cpp/tests/automation/testthat/` -- BRAT automation tests
- `scripts/` -- build and code generation scripts


## Session Module System

Each backend feature is implemented as a session module. Modules follow a consistent pattern:

### File naming convention

    src/cpp/session/modules/SessionXxx.cpp   # C++ implementation
    src/cpp/session/modules/SessionXxx.hpp   # C++ header (if needed)
    src/cpp/session/modules/SessionXxx.R     # R-side logic (if needed)

### Module structure

Each module lives in its own namespace and has an `initialize()` function that registers RPC methods and event handlers:

```cpp
namespace rstudio {
namespace session {
namespace modules {
namespace xxx {

namespace {
// Private helper functions and RPC handlers
} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "xxx_do_something", xxxDoSomething))
      (bind(sourceModuleRFile, "SessionXxx.R"));

   return initBlock.execute();
}

} // namespace xxx
} // namespace modules
} // namespace session
} // namespace rstudio
```

### Key module infrastructure

- `src/cpp/session/include/session/SessionModuleContext.hpp` -- module registration API (`registerRpcMethod`, `registerAsyncRpcMethod`, `sourceModuleRFile`, `enqueClientEvent`, etc.)
- `src/cpp/session/include/session/SessionClientEvent.hpp` -- all client event type constants


## Client-Server Communication

The frontend and backend communicate via JSON-RPC over HTTP. Understanding this flow is essential for most feature work.

### RPC (Frontend → Backend)

1. **Backend** registers an RPC handler in a session module's `initialize()`:

       registerRpcMethod("chat_start_backend", chatStartBackend)

   The handler function receives a `JsonRpcRequest` and populates a `JsonRpcResponse`.

2. **Frontend** defines the method in a `*ServerOperations` interface:

   ```java
   // e.g. ChatServerOperations.java
   public interface ChatServerOperations
   {
       void chatStartBackend(ServerRequestCallback<JsObject> callback);
   }
   ```

   The method name is auto-converted from snake_case to camelCase (e.g. `chat_start_backend` → `chatStartBackend`).

3. **Frontend** calls it from a presenter or view via the injected server operations interface.

### Client Events (Backend → Frontend)

1. **Backend** fires an event using `module_context::enqueClientEvent()`:

   ```cpp
   ClientEvent event(client_events::kChatOutput, data);
   module_context::enqueClientEvent(event);
   ```

   Event type constants are defined in `SessionClientEvent.hpp`.

2. **Frontend** receives the event through the `EventBus` and dispatches it to registered handlers.


## GWT Frontend Patterns

### Dependency Injection (GIN)

The GWT frontend uses GIN (GWT INjection, based on Guice) for dependency injection.

- `RStudioGinModule.java` -- binds interfaces to implementations
- `RStudioGinjector.java` -- provides access to injected singletons
- Use `@Inject` on constructors to wire dependencies

### MVP Pattern

Views follow Model-View-Presenter separation:

- **Presenter** (e.g. `ChatPresenter.java`) -- manages logic, handles events, calls server operations
- **View interface** (e.g. `ChatPresenter.Display`) -- defines the UI contract
- **View implementation** (e.g. `ChatPane.java`) -- implements the widget and UI

### EventBus

Components communicate through a central `EventBus` using typed events. Fire events with `eventBus_.fireEvent()` and handle them by implementing the corresponding `Handler` interface.

### Server Operations

Each feature area defines a `*ServerOperations` interface for its RPC calls. These are aggregated into `Server.java`, which is the main server interface.


## Adding a New Feature (Checklist)

When implementing a new feature end-to-end:

1. **Backend module**: Create `src/cpp/session/modules/SessionXxx.cpp` with `initialize()`, RPC handlers, and optionally a companion `.R` file.
2. **Wire the module**: Register the module's `initialize()` in the session startup sequence.
3. **Client events** (if needed): Add event type constants to `SessionClientEvent.hpp`.
4. **Server operations**: Create a `*ServerOperations.java` interface in the GWT frontend and add it to `Server.java`.
5. **Frontend view**: Add UI under `src/gwt/.../workbench/views/`.
6. **Commands** (if needed): Add to `Commands.cmd.xml`, add stubs to `Commands.java`, and update the MD5 checksum.
7. **DI wiring**: Bind new classes in `RStudioGinModule.java`.


## Key Files Reference

Backend:
- `src/cpp/session/include/session/SessionModuleContext.hpp` -- module registration API
- `src/cpp/session/include/session/SessionClientEvent.hpp` -- client event type constants
- `src/cpp/session/resources/schema/user-prefs-schema.json` -- user preferences schema
- `src/cpp/session/resources/schema/user-state-schema.json` -- UI state schema
- `src/cpp/session/session-options.json` -- session CLI options schema
- `src/cpp/server/server-options.json` -- server CLI options schema

Frontend:
- `src/gwt/src/org/rstudio/studio/client/RStudioGinModule.java` -- DI bindings
- `src/gwt/src/org/rstudio/studio/client/RStudioGinjector.java` -- DI accessor
- `src/gwt/src/org/rstudio/studio/client/server/Server.java` -- aggregated server operations
- `src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml` -- command definitions
- `src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.java` -- command stubs


## Common Debugging Patterns

- **Find where an RPC is handled**: grep for the RPC name (e.g. `"chat_start_backend"`) in `src/cpp/session/modules/`.
- **Find a command handler**: grep for `on<CommandName>` with `@Handler` in `src/gwt/`.
- **Find where a preference is used**: search for its key from `user-prefs-schema.json` in both `src/cpp/` and `src/gwt/`.
- **Trace a client event**: find the event constant in `SessionClientEvent.hpp`, then grep for it in both backend (where it's fired) and frontend (where it's handled).


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


## Writing Automated Tests

RStudio uses its own infrastructure for automated testing, nicknamed "BRAT". Essentially, one is able to test and automate a separate instance of RStudio from RStudio, via the Chrome Debugging Protocol (CDP).

Automated tests live in:

    src/cpp/tests/automation/testthat

The tools that help facilitate automation (that is, communication with the automated instance of RStudio) live in

    src/cpp/session/modules/automation

See `src/cpp/tests/automation/CLAUDE.md` for detailed guidance on writing BRAT tests.


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

### R

When documenting R code, use Roxygen style for formatting. For example:

    #' Work
    #'
    #' Perform work until the timeout is reached.
    #'
    #' @param callback The R callback to execute.
    #' @param timeout The maximum amount of time to wait, in seconds.
    work <- function(callback, timeout) { ... }

### C++

C++ code is formatted with clang-format. The configuration is in `src/cpp/.clang-format`. Key settings:

- 3-space indentation
- Allman brace style
- No tabs

### TypeScript (Desktop)

TypeScript code uses ESLint and Prettier. From `src/node/desktop`:

    npm run lint      # Check for issues
    npm run format    # Auto-format code


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

When Commands.cmd.xml is modified in any way, a checksum stored in
src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml.MD5 MUST be
updated to match, and that file included in the commit. The MD5 file is updated when the GWT
code is built using `ant` or `ant draft`.

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


## Git Conventions

For commit messages:

- Use "Addresses #<issue>" instead of "Fixes #<issue>".


For branch naming:

- Use the 'bugfix/' prefix for code changes which fix an existing issue.
- Use the 'feature/' prefix for code changes that add or extend existing functionality.
- Use the 'developer/' prefix for code changes that are primarily for developer ergonomics.
