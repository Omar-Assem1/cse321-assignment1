package selection;

import java.util.Arrays;
import java.util.Random;

public final class CorrectnessCheck {

    private static long checks = 0;
    private static long failures = 0;

    public static void main(String[] args) {
        Random rnd = new Random(2026);

        for (int trial = 0; trial < 20_000; trial++) {
            int n = 1 + rnd.nextInt(50);
            int range = 1 + rnd.nextInt(2 * n);          // between 1 and 2n distinct values
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(range) - range / 2;
            checkAllRanks(a, "random-small");
        }

        for (int n = 1; n <= 50; n++) {
            int[] equal = new int[n];
            Arrays.fill(equal, 7);
            int[] asc = new int[n];
            int[] desc = new int[n];
            for (int i = 0; i < n; i++) { asc[i] = i; desc[i] = n - i; }
            checkAllRanks(equal, "all-equal");
            checkAllRanks(asc, "ascending");
            checkAllRanks(desc, "descending");
        }

        // Part 3: bigger arrays (up to ~100k), a few random ranks each, with and without duplicates.
        for (int t = 0; t < 20; t++) {
            int n = 1000 + rnd.nextInt(100_000);
            int bound = (t % 2 == 0) ? Integer.MAX_VALUE : 1000;   // odd t => many duplicates
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(bound);
            int[] sorted = a.clone();
            Arrays.sort(sorted);
            for (int j = 0; j < 5; j++) {
                checkRank(a, sorted, 1 + rnd.nextInt(n), "random-large");
            }
            checkRank(a, sorted, (n + 1) / 2, "random-large-median");
        }

        System.out.println(failures == 0
                ? "PASSED: " + checks + " checks, 0 failures"
                : "FAILED: " + failures + " failures out of " + checks + " checks");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void checkAllRanks(int[] a, String label) {
        int[] sorted = a.clone();
        Arrays.sort(sorted);
        for (int k = 1; k <= a.length; k++) checkRank(a, sorted, k, label);
    }

    private static void checkRank(int[] a, int[] sorted, int k, String label) {
        int expected = sorted[k - 1];
        compare("randomized", Select.randomizedSelect(a.clone(), k), expected, a, k, label);
        compare("median-of-medians", Select.deterministicSelect(a.clone(), k), expected, a, k, label);
    }

    private static void compare(String name, int got, int expected, int[] a, int k, String label) {
        checks++;
        if (got != expected) {
            failures++;
            if (failures <= 10) {   // do not flood the console
                System.out.println("MISMATCH [" + label + "] " + name + ": k=" + k
                        + " expected=" + expected + " got=" + got
                        + (a.length <= 50 ? " array=" + Arrays.toString(a) : " n=" + a.length));
            }
        }
    }
}