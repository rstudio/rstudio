import type { Page, Response } from 'playwright';

/**
 * An agent answer slower than this counts as slow. Suggestions normally land
 * within a couple of seconds of the request; the session gives up on a request
 * at ~10s (see AssistantRequestWatcher), so answers between the two are a
 * service struggling, not a product delay.
 */
export const SLOW_ASSISTANT_ANSWER_MS = 5000;

/** How the session settled one assistant request. */
export interface AssistantRequestOutcome {
  /** True when the session gave up waiting on the agent and cancelled it. */
  cancelled: boolean;
  /** Milliseconds from the request to its outcome. */
  elapsedMs: number;
}

/**
 * Why a suggestion wait that came up empty should count as the assistant
 * service being too slow rather than as a product failure, or null when the
 * failure should stand.
 *
 * A cancelled request alone is not enough: a session that stopped relaying
 * requests to the agent would cancel every one of them too. So this also
 * takes the agent answering something, with every answer slow -- one fast
 * answer means the agent was keeping up and the product lost the rest.
 */
export function slowAssistantReason(outcomes: AssistantRequestOutcome[]): string | null {
  const cancelled = outcomes.filter((o) => o.cancelled);
  const answered = outcomes.filter((o) => !o.cancelled);
  if (cancelled.length === 0 || answered.length === 0) {
    return null;
  }

  const fastestMs = Math.min(...answered.map((o) => o.elapsedMs));
  if (fastestMs < SLOW_ASSISTANT_ANSWER_MS) {
    return null;
  }

  return (
    `too slow: it answered ${answered.length} request(s), the fastest after ${(fastestMs / 1000).toFixed(1)}s, ` +
    `and left ${cancelled.length} unanswered past the session's 10s cutoff`
  );
}

interface AsyncCompletionEvent {
  type?: string;
  data?: { handle?: string; response?: { result?: { cancelled?: unknown } } };
}

/**
 * Records how the session settles the assistant RPCs a page makes. They are
 * async RPCs: the POST answers at once with a handle, and the outcome arrives
 * later as an async_completion event on the get_events long-poll. Requests
 * the session relays to the agent settle with a `cancelled` flag, which is
 * true when the agent left the request pending for ~10s and the session gave
 * up on it (onBackgroundProcessing in SessionAssistant.cpp) -- the only sign,
 * short of the session log, that a slow service rather than a quiet model
 * kept a suggestion from showing (run 40398345386). Requests that settle
 * without the flag are not relayed to the agent and are ignored.
 */
export class AssistantRequestWatcher {
  private readonly sentAt = new Map<string, number>();
  private readonly settledAt = new Map<string, { cancelled: boolean; at: number }>();
  private readonly listener = (response: Response): void => {
    void this.record(response, Date.now());
  };

  constructor(private readonly page: Page) {
    page.on('response', this.listener);
  }

  stop(): void {
    this.page.off('response', this.listener);
  }

  /** The watched requests settled so far. */
  outcomes(): AssistantRequestOutcome[] {
    const outcomes: AssistantRequestOutcome[] = [];
    for (const [handle, settled] of this.settledAt) {
      const sent = this.sentAt.get(handle);
      if (sent !== undefined) {
        outcomes.push({ cancelled: settled.cancelled, elapsedMs: settled.at - sent });
      }
    }
    return outcomes;
  }

  // `at` is taken when the response arrives, before its body is read. For
  // the RPC itself that stands in for the send time: the handle comes back
  // within milliseconds.
  private async record(response: Response, at: number): Promise<void> {
    const path = new URL(response.url()).pathname;
    if (path.includes('/rpc/assistant_')) {
      const body = (await response.json().catch(() => null)) as { asyncHandle?: unknown } | null;
      if (typeof body?.asyncHandle === 'string') {
        this.sentAt.set(body.asyncHandle, at);
      }
      return;
    }

    if (!path.endsWith('/events/get_events')) {
      return;
    }

    const body = (await response.json().catch(() => null)) as { result?: unknown } | null;
    if (!Array.isArray(body?.result)) {
      return;
    }

    for (const event of body.result as AsyncCompletionEvent[]) {
      const handle = event?.data?.handle;
      const cancelled = event?.data?.response?.result?.cancelled;
      if (event?.type === 'async_completion' && typeof handle === 'string' && typeof cancelled === 'boolean') {
        this.settledAt.set(handle, { cancelled, at });
      }
    }
  }
}
