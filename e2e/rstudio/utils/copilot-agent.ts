import { spawn, type ChildProcessWithoutNullStreams } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';

/**
 * A minimal stdio LSP client for the GitHub copilot-language-server, just
 * enough to drive its sign-in flow the same way RStudio's backend does
 * (src/cpp/session/modules/SessionAssistant.cpp): spawn `node
 * language-server.js --stdio`, send `initialize` + `initialized`, then
 * `signInInitiate` and poll `checkStatus`.
 *
 * The agent owns the whole OAuth exchange (device-code fetch, token poll) and
 * writes its own credential store (auth.db under the HOME we give it -- see
 * copilotConfigDir in utils/auth.ts), so nothing here touches SQLite or
 * tokens directly.
 */

// Values mirrored from the SessionAssistant.cpp initialize request. The agent
// only uses these to label the editor in telemetry; any plausible pair works.
const EDITOR_INFO = { name: 'RStudio', version: '2026.04.0' };

// Copilot sign-in status strings (AssistantConstants.java).
export const STATUS_OK = 'OK';
export const STATUS_ALREADY_SIGNED_IN = 'AlreadySignedIn';
export const STATUS_NOT_AUTHORIZED = 'NotAuthorized';
export const STATUS_PROMPT_DEVICE_FLOW = 'PromptUserDeviceFlow';

export interface SignInInitiateResult {
  status: string;
  userCode?: string;
  verificationUri?: string;
  user?: string;
  expiresIn?: number;
  interval?: number;
}

export interface CheckStatusResult {
  status: string;
  user?: string;
}

interface JsonRpcMessage {
  jsonrpc: string;
  id?: number | string;
  method?: string;
  params?: unknown;
  result?: unknown;
  error?: { code: number; message: string; data?: unknown };
}

function log(msg: string): void {
  console.log(`[copilot-agent] ${msg}`);
}

// Where an installed RStudio keeps the bundled agent, per platform. Only the
// macOS location has been verified against a real install; the Windows and
// Linux guesses follow the Electron resources/app layout and should be
// checked (or overridden via RSTUDIO_COPILOT_JS_FOLDER) the first time the
// sign-in flow runs there.
function installedAgentFolder(): string {
  switch (process.platform) {
    case 'win32':
      return 'C:\\Program Files\\RStudio\\resources\\app\\copilot-language-server-js';
    case 'darwin':
      return '/Applications/RStudio.app/Contents/Resources/app/copilot-language-server-js';
    default:
      return '/usr/lib/rstudio/resources/app/copilot-language-server-js';
  }
}

/**
 * Resolve the language-server.js to run: RSTUDIO_COPILOT_JS_FOLDER when set
 * (the same override the IDE honors), otherwise the agent bundled with the
 * installed RStudio. Fails loud so a wrong path never turns into a silent
 * spawn of nothing.
 */
export function resolveAgentScript(): string {
  const folder = process.env.RSTUDIO_COPILOT_JS_FOLDER || installedAgentFolder();
  const script = path.join(folder, 'language-server.js');
  if (!fs.existsSync(script)) {
    throw new Error(
      `copilot-language-server not found at ${script}. `
      + 'Install RStudio Desktop or point RSTUDIO_COPILOT_JS_FOLDER at a folder containing language-server.js.',
    );
  }
  return script;
}

export class CopilotAgent {
  private child: ChildProcessWithoutNullStreams;
  private nextId = 1;
  private pending = new Map<number | string, { resolve: (v: unknown) => void; reject: (e: Error) => void }>();
  private buffer = Buffer.alloc(0);
  private exited = false;

  /**
   * Spawn the agent with HOME redirected to `homeDir`, so its credential store
   * lands in copilotConfigDir(homeDir) and the real user profile is never
   * touched. Runs on the same node executable as the test process.
   */
  constructor(homeDir: string) {
    const script = resolveAgentScript();
    const env: NodeJS.ProcessEnv = {
      ...process.env,
      HOME: homeDir,
      USERPROFILE: homeDir,
      // Plaintext store, matching what the harness sets for the IDE
      // (desktop.fixture.ts, #18205): no keychain prompts, and the sandbox
      // copy stays readable/scrubbable.
      GITHUB_COPILOT_AUTH_TOKEN_ENCRYPTION: 'false',
    };
    // XDG_CONFIG_HOME would win over HOME for the config path; drop it so the
    // redirect cannot be bypassed on hosts that set it.
    delete env.XDG_CONFIG_HOME;
    // On Windows the agent resolves its config dir from LOCALAPPDATA, which
    // still points at the real profile after the HOME/USERPROFILE redirect;
    // pin it inside homeDir to match copilotConfigDir's AppData/Local layout.
    if (process.platform === 'win32') {
      env.LOCALAPPDATA = path.join(homeDir, 'AppData', 'Local');
    }

    log(`spawning ${process.execPath} ${script} --stdio (HOME=${homeDir})`);
    this.child = spawn(process.execPath, [script, '--stdio'], {
      cwd: path.dirname(script),
      env,
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    this.child.stdout.on('data', (chunk: Buffer) => this.onData(chunk));
    this.child.stderr.on('data', (chunk: Buffer) => {
      const text = chunk.toString().trim();
      if (text) log(`[stderr] ${text}`);
    });
    this.child.on('exit', (code, signal) => {
      this.exited = true;
      log(`agent exited (code=${code}, signal=${signal})`);
      for (const [, p] of this.pending) p.reject(new Error('agent exited before responding'));
      this.pending.clear();
    });
  }

  // --- LSP framing ---------------------------------------------------------

  private send(msg: JsonRpcMessage): void {
    const body = Buffer.from(JSON.stringify(msg), 'utf8');
    const frame = Buffer.concat([Buffer.from(`Content-Length: ${body.length}\r\n\r\n`, 'ascii'), body]);
    this.child.stdin.write(frame);
  }

  private onData(chunk: Buffer): void {
    this.buffer = Buffer.concat([this.buffer, chunk]);
    // Parse as many complete Content-Length frames as the buffer holds.
    for (;;) {
      const headerEnd = this.buffer.indexOf('\r\n\r\n');
      if (headerEnd === -1) return;
      const header = this.buffer.subarray(0, headerEnd).toString('ascii');
      const match = /Content-Length:\s*(\d+)/i.exec(header);
      if (!match) {
        throw new Error(`malformed LSP header from agent: ${header}`);
      }
      const length = parseInt(match[1], 10);
      const bodyStart = headerEnd + 4;
      if (this.buffer.length < bodyStart + length) return;
      const body = this.buffer.subarray(bodyStart, bodyStart + length).toString('utf8');
      this.buffer = this.buffer.subarray(bodyStart + length);
      this.onMessage(JSON.parse(body) as JsonRpcMessage);
    }
  }

  private onMessage(msg: JsonRpcMessage): void {
    if (msg.id !== undefined && msg.method === undefined) {
      // Response to one of our requests.
      const pending = this.pending.get(msg.id);
      if (!pending) {
        log(`response for unknown request id ${msg.id}`);
        return;
      }
      this.pending.delete(msg.id);
      if (msg.error) {
        pending.reject(new Error(`agent error for id ${msg.id}: ${msg.error.code} ${msg.error.message}`));
      } else {
        pending.resolve(msg.result);
      }
    } else if (msg.id !== undefined && msg.method !== undefined) {
      // Server-initiated request. Answer with a null result so the agent never
      // stalls waiting on us (e.g. workspace/configuration or window requests);
      // for this short-lived sign-in flow the content of the answer is
      // irrelevant, but an unanswered request could deadlock the flow.
      log(`server request "${msg.method}" (id ${msg.id}); replying with null stub`);
      this.send({ jsonrpc: '2.0', id: msg.id, result: null });
    } else if (msg.method !== undefined) {
      // Notification; observe and move on. didChangeStatus in particular is a
      // useful trace of where the sign-in stands.
      log(`notification "${msg.method}": ${JSON.stringify(msg.params ?? {}).slice(0, 200)}`);
    }
  }

  // --- JSON-RPC surface -----------------------------------------------------

  request<T>(method: string, params: unknown, timeoutMs = 30_000): Promise<T> {
    if (this.exited) return Promise.reject(new Error(`agent already exited; cannot send "${method}"`));
    const id = this.nextId++;
    return new Promise<T>((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error(`request "${method}" (id ${id}) timed out after ${timeoutMs}ms`));
      }, timeoutMs);
      this.pending.set(id, {
        resolve: (v) => {
          clearTimeout(timer);
          resolve(v as T);
        },
        reject: (e) => {
          clearTimeout(timer);
          reject(e);
        },
      });
      this.send({ jsonrpc: '2.0', id, method, params });
    });
  }

  notify(method: string, params: unknown): void {
    this.send({ jsonrpc: '2.0', method, params });
  }

  // --- Copilot sign-in flow -------------------------------------------------

  /** The initialize handshake, mirroring SessionAssistant.cpp (no workspace folders). */
  async initialize(): Promise<void> {
    const result = await this.request<{ serverInfo?: { name?: string; version?: string } }>('initialize', {
      processId: process.pid,
      locale: 'en',
      initializationOptions: {
        editorInfo: EDITOR_INFO,
        editorPluginInfo: EDITOR_INFO,
      },
      capabilities: {
        workspace: { workspaceFolders: false },
      },
    });
    log(`initialized against ${result?.serverInfo?.name ?? 'agent'} ${result?.serverInfo?.version ?? ''}`);
    this.notify('initialized', {});
  }

  async signInInitiate(): Promise<SignInInitiateResult> {
    return await this.request<SignInInitiateResult>('signInInitiate', {});
  }

  async checkStatus(): Promise<CheckStatusResult> {
    return await this.request<CheckStatusResult>('checkStatus', {});
  }

  /**
   * Poll checkStatus until it reports a terminal state. OK/AlreadySignedIn is
   * success; NotAuthorized is terminal too (the account has no Copilot access,
   * which the caller should surface as an entitlement problem, not a bug).
   */
  async waitForSignIn(timeoutMs = 90_000): Promise<CheckStatusResult> {
    const deadline = Date.now() + timeoutMs;
    let last: CheckStatusResult = { status: 'unknown' };
    while (Date.now() < deadline) {
      last = await this.checkStatus();
      log(`checkStatus: ${JSON.stringify(last)}`);
      if (last.status === STATUS_OK || last.status === STATUS_ALREADY_SIGNED_IN) return last;
      if (last.status === STATUS_NOT_AUTHORIZED) return last;
      await new Promise((r) => setTimeout(r, 2000));
    }
    throw new Error(`sign-in did not complete within ${timeoutMs}ms (last status: ${JSON.stringify(last)})`);
  }

  /**
   * Orderly LSP shutdown so the agent flushes and closes its SQLite store
   * (auth.db uses WAL; killing the process mid-write could leave the row in
   * the -wal file only). Falls back to SIGKILL if the agent lingers.
   */
  async shutdown(): Promise<void> {
    if (this.exited) return;
    try {
      await this.request('shutdown', null, 5_000);
    } catch (err) {
      log(`shutdown request failed (${(err as Error).message}); exiting anyway`);
    }
    try {
      this.notify('exit', null);
    } catch {
      // stdin may already be closed; the kill below covers it
    }
    const deadline = Date.now() + 10_000;
    while (!this.exited && Date.now() < deadline) {
      await new Promise((r) => setTimeout(r, 200));
    }
    if (!this.exited) {
      log('agent did not exit after shutdown/exit; killing');
      this.child.kill('SIGKILL');
    }
  }
}
