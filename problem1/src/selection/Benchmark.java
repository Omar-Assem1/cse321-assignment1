package selection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

public final class Benchmark {

    private static final int[] SIZES = { 1_000, 10_000, 100_000, 1_000_000, 10_000_000 };
    private static final int[] RUNS  = {   500,    200,      50,        15,          5 };

    private static final int WARMUP_ROUNDS = 50;
    private static final int WARMUP_SIZE = 100_000;

    private static long sink;   // consumes results so the JIT cannot discard the work

    private interface Method {
        int run(int[] a, int k);
    }

    public static void main(String[] args) throws IOException {
        Path csv = Paths.get(args.length > 0 ? args[0] : "results.csv");

        Method[] methods = { Select::randomizedSelect, Select::deterministicSelect, Select::sortSelect };
        String[] names = { "randomized", "median-of-medians", "sort" };

        System.out.println("Warming up the JIT ...");
        warmUp(methods);

        StringBuilder out = new StringBuilder("n,runs,randomized_ms,median_of_medians_ms,sort_ms\n");
        System.out.printf(Locale.US, "%-12s %-6s %-14s %-20s %-12s%n",
                "n", "runs", "randomized_ms", "median_of_medians_ms", "sort_ms");

        for (int s = 0; s < SIZES.length; s++) {
            int n = SIZES[s];
            int runs = RUNS[s];
            int k = (n + 1) / 2;                       // the (lower) median
            Random rnd = new Random(1000L + n);
            double[] totalNs = new double[methods.length];

            for (int r = 0; r < runs; r++) {
                int[] data = randomArray(n, rnd);      // generation is not timed
                int expected = 0;
                for (int step = 0; step < methods.length; step++) {
                    int m = (step + r) % methods.length;   // rotate the order
                    int[] copy = data.clone();             // clone is not timed
                    long t0 = System.nanoTime();
                    int result = methods[m].run(copy, k);
                    long t1 = System.nanoTime();
                    totalNs[m] += (t1 - t0);
                    sink += result;
                    if (step == 0) {
                        expected = result;
                    } else if (result != expected) {
                        throw new IllegalStateException("methods disagree for n=" + n
                                + " (" + names[m] + " gave " + result + ", expected " + expected + ")");
                    }
                }
            }

            double rand = totalNs[0] / runs / 1e6;     // ns -> ms, averaged
            double mom  = totalNs[1] / runs / 1e6;
            double sort = totalNs[2] / runs / 1e6;
            System.out.printf(Locale.US, "%-12d %-6d %-14.3f %-20.3f %-12.3f%n", n, runs, rand, mom, sort);
            out.append(String.format(Locale.US, "%d,%d,%.3f,%.3f,%.3f%n", n, runs, rand, mom, sort));
        }

        if (csv.getParent() != null) Files.createDirectories(csv.getParent());
        Files.writeString(csv, out.toString());
        System.out.println("Wrote " + csv + "   (checksum " + sink + ")");
    }

    private static void warmUp(Method[] methods) {
        Random rnd = new Random(1);
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            int[] data = randomArray(WARMUP_SIZE, rnd);
            for (Method m : methods) sink += m.run(data.clone(), (WARMUP_SIZE + 1) / 2);
        }
    }

    private static int[] randomArray(int n, Random rnd) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = rnd.nextInt();
        return a;
    }
}