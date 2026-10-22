# CSE321 Assignment 1: Divide and Conquer
**Name:** Omar Assem  **ID:** 23010610

## Problem 1: Selection algorithms

### 1.1 Implementation
All three methods find the k-th smallest element (k is 1-based) and work in place.

- **Randomized select (CLRS 9.2).** Pick a uniformly random pivot from the current
  range, partition, and continue only in the part that contains rank k.
  Expected time O(n), worst case O(n^2).
- **Deterministic select, median of medians (CLRS 9.3).** Split the range into groups
  of 5, sort each group with insertion sort, take each group's median, and find the
  median of these medians recursively. Use it as the pivot, partition, and continue
  on one side. The pivot guarantees that at least about 30% of the elements fall on
  each side, so T(n) <= T(n/5) + T(7n/10 + 6) + O(n) = O(n) in the worst case.
  The group medians are swapped to the front of the range, so the recursive call
  works on that prefix without an extra array.
- **Naive method.** `Arrays.sort`, then return element k-1. O(n log n).

**Deviation from the textbook.** CLRS uses the Lomuto partition, which degrades to
O(n^2) when many elements are equal. I use a three-way partition (less than / equal to /
greater than the pivot), so duplicates cannot cause quadratic behavior. The recursion
structure is unchanged.

**Correctness.** `CorrectnessCheck` compares both algorithms with the sorted answer on
20,000 random arrays of size 1 to 50 with many duplicates (every rank k tested), on
all-equal, ascending and descending arrays, and on random arrays up to about 100,000
elements. Result: `PASSED: 1032882 checks, 0 failures`.

```java
package selection;

import java.util.Arrays;
import java.util.SplittableRandom;

public final class Select {

    private static final SplittableRandom RNG = new SplittableRandom(42); // fixed seed = reproducible
    private static final int GROUP = 5;                                   // group size for median of medians

    private Select() { }

    public static int randomizedSelect(int[] a, int k) {
        checkArgs(a, k);
        int target = k - 1;                 // 0-based index of the answer in sorted order
        int lo = 0, hi = a.length - 1;      // invariant: lo <= target <= hi
        while (lo < hi) {
            // pick a uniformly random pivot value from the current range
            int pivot = a[lo + RNG.nextInt(hi - lo + 1)];
            int[] b = partition3(a, lo, hi, pivot);   // b[0]..b[1] = block equal to pivot
            if (target < b[0]) {
                hi = b[0] - 1;              // answer is among the smaller elements
            } else if (target > b[1]) {
                lo = b[1] + 1;              // answer is among the larger elements
            } else {
                return pivot;               // target falls in the "equal" block: done
            }
        }
        return a[lo];                       // one element left, it must be the answer
    }

    public static int deterministicSelect(int[] a, int k) {
        checkArgs(a, k);
        return mmSelect(a, 0, a.length - 1, k - 1);
    }

    private static int mmSelect(int[] a, int lo, int hi, int target) {
        while (true) {
            if (hi - lo + 1 <= GROUP) {              // tiny range: just sort it
                insertionSort(a, lo, hi);
                return a[target];
            }
            int pivot = medianOfMedians(a, lo, hi);  // a pivot guaranteed to be "good"
            int[] b = partition3(a, lo, hi, pivot);
            if (target < b[0]) {
                hi = b[0] - 1;
            } else if (target > b[1]) {
                lo = b[1] + 1;
            } else {
                return pivot;
            }
        }
    }

    private static int medianOfMedians(int[] a, int lo, int hi) {
        int n = hi - lo + 1;
        int groups = (n + GROUP - 1) / GROUP;        // the last group may have fewer than 5 elements
        for (int g = 0; g < groups; g++) {
            int s = lo + g * GROUP;                  // group start
            int e = Math.min(s + GROUP - 1, hi);     // group end
            insertionSort(a, s, e);
            // The median is the middle of the sorted group. Move it to position lo+g.
            // lo+g <= s, so this only touches groups that are already processed.
            swap(a, lo + g, s + (e - s) / 2);
        }
        // median of the 'groups' medians stored in a[lo .. lo+groups-1]
        return mmSelect(a, lo, lo + groups - 1, lo + (groups - 1) / 2);
    }

    public static int sortSelect(int[] a, int k) {
        checkArgs(a, k);
        Arrays.sort(a);                              // native dual-pivot quicksort, O(n log n)
        return a[k - 1];
    }

    private static int[] partition3(int[] a, int lo, int hi, int pivot) {
        int lt = lo;      // next slot for a "smaller" element
        int i = lo;       // scanning index
        int gt = hi;      // next slot (from the right) for a "larger" element
        while (i <= gt) {
            if (a[i] < pivot) {
                swap(a, lt++, i++);
            } else if (a[i] > pivot) {
                swap(a, i, gt--);                    // do not advance i: the swapped-in element is unseen
            } else {
                i++;
            }
        }
        return new int[] { lt, gt };
    }

    private static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int v = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > v) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = v;
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private static void checkArgs(int[] a, int k) {
        if (a == null || k < 1 || k > a.length) {
            throw new IllegalArgumentException("need 1 <= k <= a.length");
        }
    }
}
```

### 1.2 Experimental setup
- Machine: 12th Gen Intel(R) Core(TM) i5-12450HX (12 threads), 11Gi RAM, Kali GNU/Linux Rolling. Benchmarks run on JDK 21.0.11-ea (classes compiled with JDK 21.0.11-ea).
- Random int arrays (full int range), k = the (lower) median, n = 10^3 to 10^7.
- JIT warm-up: 50 rounds of all three methods on 100,000-element arrays before measuring.
- Each method receives its own clone of the array, and cloning is outside the timed region.
- Time is measured with `System.nanoTime()` and averaged over 500 / 200 / 50 / 15 / 5 runs
  for n = 10^3 ... 10^7. The run order of the three methods rotates each run, and the
  program checks that all three return the same answer.

### 1.3 Results
Main run: JDK 21.0.11-ea. Average time in ms:

| n | randomized (ms) | median of medians (ms) | sort (ms) |
|---|---|---|---|
| 10^3 | 0.011 | 0.027 | 0.030 |
| 10^4 | 0.097 | 0.277 | 0.380 |
| 10^5 | 0.965 | 2.749 | 4.843 |
| 10^6 | 8.338 | 26.269 | 58.376 |
| 10^7 | 94.748 | 255.685 | 716.448 |

Time per element (ns):

| n | randomized | median of medians | sort |
|---|---|---|---|
| 10^3 | 11.0 | 27.0 | 30.0 |
| 10^4 | 9.7 | 27.7 | 38.0 |
| 10^5 | 9.7 | 27.5 | 48.4 |
| 10^6 | 8.3 | 26.3 | 58.4 |
| 10^7 | 9.5 | 25.6 | 71.6 |

![Selection time vs n (log-log)](plot_time.png)

![Time per element vs n](plot_per_element.png)

An earlier run of the same program on JDK 25.0.3-ea (same machine) gave:

| n | randomized (ms) | median of medians (ms) | sort (ms) |
|---|---|---|---|
| 10^3 | 0.011 | 0.026 | 0.006 |
| 10^4 | 0.100 | 0.265 | 0.060 |
| 10^5 | 0.976 | 2.721 | 0.716 |
| 10^6 | 8.798 | 26.407 | 8.115 |
| 10^7 | 97.025 | 255.357 | 127.136 |

### 1.4 Analysis
**Growth.** Both selection algorithms are linear: their time per element stays roughly
constant across four orders of magnitude (about 9-11 ns for randomized select, about
26-30 ns for median of medians), so each 10x increase in n gives about 10x the time.
Sorting is superlinear: its time per element grows from 30 ns to 72 ns, which matches
O(n log n).

**Ranking (JDK 21.0.11-ea).** Randomized select is fastest at every size. Median of medians
is second from 10^4 upward (about equal to sort at 10^3), and sort is slowest. At 10^7:
randomized 94.7 ms, median of medians 255.7 ms (2.7x slower than randomized), sort
716.4 ms (7.6x slower than randomized). The gap between sort and randomized select
widens with n, as expected when comparing O(n log n) with O(n).

**Deviations from theory.**

1. Median of medians has a better worst case than randomized select, yet it is about
   2.7x slower on random data. It makes extra passes (grouping, insertion sorts, median
   swaps) and a recursive call per level. Random inputs never trigger the worst case it
   protects against, so its guarantee costs time without a benefit here.
2. The measured advantage of selection over sorting at 10^7 is 7.6x, not the factor of
   about log2(n) = 23 that the asymptotics suggest. Asymptotic analysis hides constants:
   each partition pass costs several operations per element, and my partition loop has
   unpredictable branches on random data (a plausible explanation that I did not isolate
   experimentally).
3. The results depend on the JDK. On JDK 25.0.3-ea, the same sort took 127 ms at 10^7,
   and it beat randomized select up to 10^6, while the two selection algorithms were
   almost unchanged. The selection code is my own and is identical in both runs, so the
   difference comes from the library sort or the JIT. I did not investigate the cause,
   so I only report it as an observation.
4. Time per element for the selection algorithms is not perfectly flat. Cache and memory
   effects (a 10^7-element int array is 40 MB), JIT compilation and garbage collection
   may add noise, especially with only 5 runs at 10^7. I did not isolate these.
5. The three-way partition does more work per element than a plain partition, which
   raises the constant of both selection algorithms. I chose it for robustness against
   duplicates.

**Conclusion.** On JDK 21.0.11-ea, selection wins clearly and the gap grows with n, as the
theory predicts, and randomized select is the fastest overall. On JDK 25 the library
sort is competitive up to about 10^6 elements. Both runs agree that selection runs in
linear time and that median of medians pays a large constant factor for its worst-case
guarantee.

## Problem 2: Minimum perimeter triangle

### 2.1 Approach
**Key fact.** In a triangle with perimeter P, every side is at most P/2, because
a <= b + c implies 2a <= a + b + c. This also holds for collinear triples, whose
perimeter is twice the distance between the outer points. So if the best perimeter found
so far is d, a better triangle has all three pairwise distances below d/2.

**Algorithm.**

1. Sort the points by x (ties by y) once.
2. `divide(lo, hi)`: if at most 5 points remain, try all triples (at most 10). Otherwise
   split at the middle index, solve both halves recursively, and set d to the smaller
   result.
3. A better triangle must use points from both halves, otherwise it was already counted.
   All its points are within d/2 of the dividing line, so only the points in the strip
   |x - midX| <= d/2 matter.
4. Merge the two y-sorted halves (as in merge sort), collect the strip points in y order,
   and for each strip point i test triples (i, j, k) with j and k after i in y order and
   y_j - y_i <= d/2, y_k - y_i <= d/2. Whenever a smaller perimeter is found, d is
   updated, which shrinks the window.
5. Return d. Collinear triples need no special case, because the sum of the three
   distances already equals twice the outer distance.

### 2.2 Why only a constant number of points need checking
The points of the left half are sparse: d is the smaller of the two halves' best
perimeters, so any three points of the same half form a triangle with perimeter at
least d (the same holds for the right half). Take a window of width d/2 on each side of
the dividing line and height d/2, and divide it into cells of side d/4. Three points of
one half in the same cell would form a triangle with perimeter at most
(2 + sqrt(2)) * d/4 = 0.854d < d (the largest triangle inside a square is the right
triangle on three of its corners), a contradiction. So each cell holds at most 2 points
of each half. The window has 2 x 2 = 4 cells per side, so 8 cells in total and at most
8 * 2 = 16 points. The inner loops therefore do a constant amount of work per strip point.

### 2.3 Complexity
- The initial sort costs O(n log n).
- At each recursion level, the merge costs O(n), building the strip costs O(n), and the
  strip scan costs O(m) <= O(n) because of the constant window. So
  T(n) = 2T(n/2) + O(n), which gives O(n log n).
- Total time O(n log n), space O(n) (temporary arrays for the merge and the strip).
- A brute-force solution checks all triples, which is O(n^3), about 10^15 operations
  for n = 10^5.

### 2.4 Testing
- The four sample cases pass.
- 5000 random cases compared with the O(n^3) brute force agree (including fully collinear
  sets, two-line sets and tiny coordinate ranges with many ties).
- Timings for n = 10^5: random points 28 ms, collinear points 6 ms, dense grid about 20 ms.
- Edge cases: maximum coordinates (n = 3), Windows line endings with no trailing
  newline, 100,000 tiny test cases, and 10^5 points on a vertical line, all correct.
- The course's automatic checker passed all tests (about 0.2 s per performance test,
  including JVM startup).

### 2.5 Notes
- The strip check handles triples, not pairs as in the classic closest-pair problem,
  so its constant is larger, but it is still constant.
- A tolerance of 1e-9 in the strip comparisons protects against floating-point rounding.
- Output uses `Locale.US` so the decimal separator is always a dot.