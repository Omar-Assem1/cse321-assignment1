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