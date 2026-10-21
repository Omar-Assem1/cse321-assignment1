# CSE321 Assignment 1: Divide and Conquer
Name / ID: ...

## Problem 1: Selection algorithms

### 1.1 Implementation
- Paste the code of Select.java (the three methods), plus a short description of each.
- Mention: three-way partition used instead of Lomuto (reason: duplicates).
- Mention: correctness checked with CorrectnessCheck (N checks, 0 failures).

### 1.2 Experimental setup
- Machine (CPU, RAM), JDK version, JVM flags (-Xms2g -Xmx2g).
- Random int arrays, k = median, size n = 10^3 ... 10^7.
- Warm-up, per-method clones, System.nanoTime, runs per size (500/200/50/15/5),
  rotated method order, averages reported.

### 1.3 Results
- Table from results.csv (n, randomized, median-of-medians, sort in ms).
- Plot 1: time vs n, log-log, 3 series.
- Plot 2 (optional): time / n (ns per element) vs n, log x-axis.

### 1.4 Analysis
- Theory: randomized O(n) expected, O(n^2) worst; median of medians O(n) worst;
  sort O(n log n).
- Which is fastest, and by what factor at 10^7?
- Does the measured growth match? (time x10 per size step for O(n); slightly more for n log n.)
- Deviations from theory (see section 4 of the chat answer for ideas): large constant of
  median of medians, optimized native sort, cache and memory effects at 10^6-10^7, JIT, GC,
  variance of randomized, the effect of the partition scheme.

## Problem 2: Minimum perimeter triangle

### 2.1 Approach
- Key fact: every side <= P/2, so a better triangle has all pairwise distances < d/2.
- Sort by x, split at the median, recurse, d = min(left, right).
- Strip |x - midX| <= d/2, sorted by y (via merge), window y_j - y_i <= d/2.
- Base case: <= 5 points, brute force. Collinear triples need no special handling
  (the sum of the three sides equals twice the outer distance).

### 2.2 Why only a constant number of points need checking
- Points of one half are pairwise "sparse": any three of them have perimeter >= d.
- Cut the window (width d/2 per side, height d/2) into cells of side d/5; three points
  in one cell would give perimeter <= 3 * sqrt(2) * d/5 < d, so each cell holds <= 2 points.
- At most 3x3 cells per side => <= 36 points in the window. Constant => strip work is O(m).

### 2.3 Complexity
- Initial sort O(n log n); recurrence T(n) = 2T(n/2) + O(n) => O(n log n) total.
- Space O(n).
- Compare with brute force O(n^3).

### 2.4 Testing
- Samples, 5000 random cases vs brute force (collinear, ties, tiny ranges), n = 10^5 timings.

### 2.5 Notes / deviations
- Constant factor in the strip is larger than in the classic closest-pair algorithm
  (triples, not pairs), but still constant.
- EPS = 1e-9 in the strip tests protects against floating-point rounding.