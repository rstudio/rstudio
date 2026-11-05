/*
 * Copyright (C) 2026 by Posit Software, PBC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

const BOUNDED_LIMITS = Object.freeze({
   smallLines: 32,
   ambiguousLines: 128,
   characters: 4096,
   groupReads: 200000,
   totalReads: 2000000,
   totalCharacters: 65536,
   highlightLines: 128,
   highlightCharacters: 8192,
   highlightGroupReads: 100000,
   highlightTotalReads: 300000,
   highlightTotalCharacters: 16384
});

// Evaluated inside Ace's VM. Work limits are deterministic character-sequence
// reads, not elapsed time: review decorations cannot alter hunk selection by
// spending more of a shared deadline. Size caps also bound allocation and
// heuristic work outside getElement(). Line alignment retains its time limit.
function installBoundedRefinement(provider, experiment) {
   const Computer = provider.experimentComputer;
   const Sequence = provider.experimentCharSequence;
   const refine = Computer.prototype.refineDiff;
   const getElement = Sequence.prototype.getElement;
   const exhausted = {};
   let active = null;
   Sequence.prototype.getElement = function(offset) {
      if (active) {
         if (active.remaining === 0 || active.shared.remaining === 0)
            throw exhausted;
         active.remaining--;
         active.shared.remaining--;
      }
      return getElement.call(this, offset);
   };

   function characterCount(lines, range, cap) {
      let count = range.endLineNumber - range.startLineNumber;
      for (let row = range.startLineNumber; row <= range.endLineNumber; row++) {
         const start = row === range.startLineNumber ? range.startColumn - 1 : 0;
         const end = row === range.endLineNumber ? range.endColumn - 1 : lines[row - 1].length;
         count += end - start;
         if (count > cap)
            break;
      }
      return count;
   }

   function hasSharedLine(base, lines, diff) {
      const seen = new Set();
      for (let row = diff.seq1Range.start; row < diff.seq1Range.endExclusive; row++)
         seen.add(base[row].trim());
      for (let row = diff.seq2Range.start; row < diff.seq2Range.endExclusive; row++) {
         if (seen.has(lines[row].trim()))
            return true;
      }
      return false;
   }

   Computer.prototype.refineDiff = function(base, lines, diff, timeout, whitespace, options) {
      const limits = options.boundedRefinement;
      if (!limits)
         return refine.call(this, base, lines, diff, timeout, whitespace, options);

      const coarse = refine.call(this, base, lines, diff, timeout, whitespace, {
         ...options, skipCharacterRefinement: true
      });
      const mapping = coarse.mappings[0];
      const lineCount = diff.seq1Range.length + diff.seq2Range.length;
      const size = cap => characterCount(base, mapping.originalRange, cap) +
         characterCount(lines, mapping.modifiedRange, cap);
      const small = lineCount <= limits.smallLines;
      const ambiguous = !small && lineCount <= limits.ambiguousLines && hasSharedLine(base, lines, diff);
      const characters = small || ambiguous ? size(limits.characters) : Infinity;
      const eligible = characters <= limits.characters;
      const budgets = options.boundedBudgets || (options.boundedBudgets = {
         canonical: { remaining: limits.totalReads, characters: limits.totalCharacters },
         highlight: { remaining: limits.highlightTotalReads, characters: limits.highlightTotalCharacters }
      });
      const record = {
         old: [diff.seq1Range.start, diff.seq1Range.endExclusive],
         new: [diff.seq2Range.start, diff.seq2Range.endExclusive],
         reason: eligible ? (small ? 'small' : 'ambiguous') : 'size',
         refined: false, reads: 0, characters: 0,
         highlighted: false, highlightReads: 0, highlightCharacters: 0
      };
      experiment.boundedGroups.push(record);
      const run = (shared, allowance, field, characterField, count) => {
         if (shared.remaining === 0 || allowance === 0 || count > shared.characters)
            return null;
         shared.characters -= count;
         record[characterField] = count;
         const budget = { shared, remaining: allowance };
         const previous = active;
         active = budget;
         try {
            // Work exhaustion is caught here and produces a coarse mapping;
            // it must not be mistaken for a failed line alignment.
            return refine.call(this, base, lines, diff, { isValid: () => true }, whitespace, options);
         } catch (error) {
            if (error !== exhausted)
               throw error;
            return null;
         } finally {
            record[field] = allowance - budget.remaining;
            active = previous;
         }
      };
      if (eligible) {
         const result = run(
            budgets.canonical,
            limits.groupReads,
            'reads',
            'characters',
            characters
         );
         if (result) {
            record.refined = true;
            return result;
         }
         record.reason = 'work';
      }

      // The coarse mapping chooses the outer hunk in both modes. Review may
      // decorate it with more precise characters, but cannot split or move it.
      mapping.boundedHighlights = [];
      if (options.includeBoundedHighlights && lineCount <= limits.highlightLines) {
         const highlightSize = size(limits.highlightCharacters);
         if (highlightSize <= limits.highlightCharacters) {
            const result = run(
               budgets.highlight,
               limits.highlightGroupReads,
               'highlightReads',
               'highlightCharacters',
               highlightSize
            );
            if (result) {
               mapping.boundedHighlights = result.mappings;
               record.highlighted = true;
            }
         }
      }
      return coarse;
   };
}

module.exports = { BOUNDED_LIMITS, installBoundedRefinement };
