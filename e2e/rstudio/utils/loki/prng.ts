/**
 * Seeded pseudo-random source for Agent Loki.
 *
 * A run is reproducible in the weak sense that the same seed picks the same
 * sequence of candidate indices. That is a debugging aid for the tool itself,
 * never a reproduction for a human: a seed tells a reader how to gamble, not
 * what to do. Human reproduction comes from the recorded step list
 * (utils/loki/report.ts).
 */

/** Mulberry32: 32-bit state, uniform enough for choosing among candidates. */
export class Prng {
  private state: number;

  constructor(readonly seed: number) {
    // Force to uint32 so a passed-in negative or fractional seed still gives a
    // stable, documented starting state.
    this.state = seed >>> 0;
  }

  /** Next float in [0, 1). */
  next(): number {
    this.state = (this.state + 0x6d2b79f5) >>> 0;
    let t = this.state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }

  /** Next integer in [0, max). Returns 0 when max <= 0. */
  int(max: number): number {
    if (max <= 0)
      return 0;
    return Math.floor(this.next() * max);
  }

  /** Uniform pick from a non-empty array; undefined for an empty one. */
  pick<T>(items: readonly T[]): T | undefined {
    if (items.length === 0)
      return undefined;
    return items[this.int(items.length)];
  }

  /**
   * Weighted pick over [weight, value] pairs. Weights need not sum to any
   * particular total. Entries with weight <= 0 are never chosen.
   */
  pickWeighted<T>(entries: readonly [number, T][]): T | undefined {
    const usable = entries.filter(([w]) => w > 0);
    if (usable.length === 0)
      return undefined;
    const total = usable.reduce((sum, [w]) => sum + w, 0);
    let roll = this.next() * total;
    for (const [weight, value] of usable) {
      roll -= weight;
      if (roll < 0)
        return value;
    }
    // Floating-point drift only; the last usable entry is the correct answer.
    return usable[usable.length - 1][1];
  }

  /** Fisher-Yates shuffle of a copy. */
  shuffle<T>(items: readonly T[]): T[] {
    const out = items.slice();
    for (let i = out.length - 1; i > 0; i--) {
      const j = this.int(i + 1);
      [out[i], out[j]] = [out[j], out[i]];
    }
    return out;
  }
}

/**
 * Resolve the run seed: PW_LOKI_SEED when set, otherwise a fresh random one.
 * The chosen value is recorded in the report so a run can be repeated while
 * developing the tool.
 */
export function resolveSeed(): number {
  const raw = process.env.PW_LOKI_SEED;
  if (raw !== undefined && raw !== '') {
    const parsed = Number(raw);
    if (!Number.isFinite(parsed))
      throw new Error(`PW_LOKI_SEED="${raw}" is not a number`);
    return parsed >>> 0;
  }
  return Math.floor(Math.random() * 0xffffffff) >>> 0;
}
