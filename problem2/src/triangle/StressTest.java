package triangle;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class StressTest {

    public static void main(String[] args) {
        boolean ok = samples();
        ok &= randomVsBrute();
        timeLarge();
        System.out.println(ok ? "ALL PASSED" : "SOME TESTS FAILED");
        System.exit(ok ? 0 : 1);
    }

    private static boolean samples() {
        boolean ok = true;
        ok &= expect(new int[]{0, 1, 2, 1}, new int[]{0, 0, 0, 3}, 4.000);
        ok &= expect(new int[]{0, 3, 0, 10, 11, 10}, new int[]{0, 0, 4, 10, 10, 11}, 3.414);
        ok &= expect(new int[]{0, 1, 3}, new int[]{0, 0, 0}, 6.000);
        ok &= expect(new int[]{0, 6, 0}, new int[]{0, 0, 8}, 24.000);
        System.out.println(ok ? "samples: OK" : "samples: FAILED");
        return ok;
    }

    private static boolean expect(int[] xs, int[] ys, double expected) {
        double got = MinPerimeterTriangle.solve(xs, ys);
        boolean ok = Math.abs(got - expected) < 5e-4;   // expected values are rounded to 3 decimals
        if (!ok) System.out.println("  expected " + expected + " got " + got);
        return ok;
    }

    private static boolean randomVsBrute() {
        Random rnd = new Random(2026);
        int bad = 0;
        final int trials = 5000;
        for (int t = 0; t < trials; t++) {
            int[][] c = randomCase(rnd);
            double fast = MinPerimeterTriangle.solve(c[0], c[1]);
            double slow = MinPerimeterTriangle.bruteForce(c[0], c[1]);
            if (Math.abs(fast - slow) > 1e-9 * Math.max(1.0, slow)) {
                bad++;
                if (bad <= 5) System.out.println("MISMATCH n=" + c[0].length + " fast=" + fast + " brute=" + slow);
            }
        }
        System.out.println("random vs brute force: " + (trials - bad) + "/" + trials + " agree");
        return bad == 0;
    }

    private static int[][] randomCase(Random rnd) {
        int[] ranges = {3, 5, 10, 50, 1000, 1_000_000};
        int r = ranges[rnd.nextInt(ranges.length)];
        int mode = rnd.nextInt(3);
        long cap = mode == 0 ? (2L * r + 1) * (2L * r + 1) : mode == 1 ? 2L * r + 1 : 2L * (2L * r + 1);
        int n = (int) Math.min(3 + rnd.nextInt(40), cap);   // cap = how many distinct points exist
        return generate(rnd, n, r, mode);
    }

    private static int[][] generate(Random rnd, int n, int r, int mode) {
        Set<Long> seen = new HashSet<>();
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; ) {
            int x = rnd.nextInt(2 * r + 1) - r;
            int y = mode == 0 ? rnd.nextInt(2 * r + 1) - r : mode == 1 ? x : rnd.nextInt(2);
            if (seen.add((long) x * 4_000_003L + y)) {     // keep points distinct
                xs[i] = x;
                ys[i] = y;
                i++;
            }
        }
        return new int[][]{xs, ys};
    }

    private static void timeLarge() {
        Random rnd = new Random(7);
        int n = 100_000;

        int[][] random = generate(rnd, n, 1_000_000, 0);

        int[] lx = new int[n], ly = new int[n];             // collinear, evenly spaced
        for (int i = 0; i < n; i++) { lx[i] = i - n / 2; ly[i] = 0; }

        int side = 316;                                      // 316 * 316 = 99856 grid points
        int[] gx = new int[side * side], gy = new int[side * side];
        for (int i = 0; i < gx.length; i++) { gx[i] = i % side; gy[i] = i / side; }

        timeOne("random   n=" + n, random[0], random[1]);
        timeOne("line     n=" + n, lx, ly);
        timeOne("grid     n=" + gx.length, gx, gy);
    }

    private static void timeOne(String label, int[] xs, int[] ys) {
        long t0 = System.nanoTime();
        double ans = MinPerimeterTriangle.solve(xs, ys);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf(Locale.US, "%s -> %.3f  (%d ms)%n", label, ans, ms);
    }
}