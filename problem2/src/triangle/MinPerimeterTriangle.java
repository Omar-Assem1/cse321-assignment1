package triangle;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

public final class MinPerimeterTriangle {

    // coordinates are in [-10^6, 10^6]; they are packed into one long for fast sorting
    private static final long OFFSET = 1_000_000L;
    private static final long BASE = 2_000_001L;
    private static final double EPS = 1e-9;     // tolerance against floating-point rounding
    private static final int BRUTE_LIMIT = 5;   // ranges this small are solved by brute force

    private final int[] px, py;   // points, sorted by x at start; each range becomes y-sorted after its recursion
    private final int[] tx, ty;   // temp buffers for merging
    private final int[] sx, sy;   // buffer for the strip

    private MinPerimeterTriangle(int[] xs, int[] ys) {
        int n = xs.length;
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) {
            keys[i] = (xs[i] + OFFSET) * BASE + (ys[i] + OFFSET);   // orders by x, then y
        }
        Arrays.sort(keys);
        px = new int[n];
        py = new int[n];
        for (int i = 0; i < n; i++) {
            px[i] = (int) (keys[i] / BASE - OFFSET);
            py[i] = (int) (keys[i] % BASE - OFFSET);
        }
        tx = new int[n];
        ty = new int[n];
        sx = new int[n];
        sy = new int[n];
    }

    public static double solve(int[] xs, int[] ys) {
        if (xs.length < 3) throw new IllegalArgumentException("need at least 3 points");
        return new MinPerimeterTriangle(xs, ys).divide(0, xs.length);
    }

    private double divide(int lo, int hi) {
        if (hi - lo <= BRUTE_LIMIT) {                   // base case: at most C(5,3) = 10 triples
            double best = bruteForce(px, py, lo, hi);
            sortByY(lo, hi);
            return best;
        }
        // for n >= 6 both halves have >= 3 points, so d is always finite
        int mid = (lo + hi) >>> 1;
        int midX = px[mid];             // read BEFORE recursing: recursion reorders the range by y
        double d = Math.min(divide(lo, mid), divide(mid, hi));

        mergeByY(lo, mid, hi);          // now [lo,hi) is sorted by y

        int m = 0;
        double reach = d / 2 + EPS;
        for (int i = lo; i < hi; i++) {
            if (Math.abs(px[i] - midX) <= reach) {
                sx[m] = px[i];
                sy[m] = py[i];
                m++;
            }
        }

        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m && sy[j] - sy[i] <= d / 2 + EPS; j++) {
                double dij = dist(sx[i], sy[i], sx[j], sy[j]);
                // perimeter >= 2 * (any side), so a better triangle needs every side < d/2
                if (2 * dij >= d) continue;
                for (int k = j + 1; k < m && sy[k] - sy[i] <= d / 2 + EPS; k++) {
                    double p = dij + dist(sx[i], sy[i], sx[k], sy[k]) + dist(sx[j], sy[j], sx[k], sy[k]);
                    if (p < d) d = p;
                }
            }
        }
        return d;
    }

    private void mergeByY(int lo, int mid, int hi) {
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) {
            if (py[i] <= py[j]) { tx[k] = px[i]; ty[k] = py[i]; i++; }
            else                { tx[k] = px[j]; ty[k] = py[j]; j++; }
            k++;
        }
        while (i < mid) { tx[k] = px[i]; ty[k] = py[i]; i++; k++; }
        while (j < hi)  { tx[k] = px[j]; ty[k] = py[j]; j++; k++; }
        System.arraycopy(tx, lo, px, lo, hi - lo);
        System.arraycopy(ty, lo, py, lo, hi - lo);
    }

    private void sortByY(int lo, int hi) {
        for (int i = lo + 1; i < hi; i++) {
            int cx = px[i], cy = py[i];
            int j = i - 1;
            while (j >= lo && py[j] > cy) {
                px[j + 1] = px[j];
                py[j + 1] = py[j];
                j--;
            }
            px[j + 1] = cx;
            py[j + 1] = cy;
        }
    }

    private static double dist(int x1, int y1, int x2, int y2) {
        double dx = x1 - x2;    // int difference (at most 2*10^6), then widened to double
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double perimeter(int x1, int y1, int x2, int y2, int x3, int y3) {
        return dist(x1, y1, x2, y2) + dist(x1, y1, x3, y3) + dist(x2, y2, x3, y3);
    }

    private static double bruteForce(int[] xs, int[] ys, int lo, int hi) {
        double best = Double.POSITIVE_INFINITY;
        for (int a = lo; a < hi; a++)
            for (int b = a + 1; b < hi; b++)
                for (int c = b + 1; c < hi; c++)
                    best = Math.min(best, perimeter(xs[a], ys[a], xs[b], ys[b], xs[c], ys[c]));
        return best;
    }

    public static double bruteForce(int[] xs, int[] ys) {
        return bruteForce(xs, ys, 0, xs.length);
    }

    private static final class FastReader {
        private final InputStream in;
        private final byte[] buf = new byte[1 << 16];
        private int len = 0, ptr = 0;
        boolean eof = false;            // set when nextInt() ran out of input

        FastReader(InputStream in) { this.in = in; }

        private int read() throws IOException {
            if (ptr == len) {
                len = in.read(buf, 0, buf.length);
                ptr = 0;
                if (len <= 0) { len = 0; return -1; }
            }
            return buf[ptr++];
        }

        int nextInt() throws IOException {
            int c = read();
            while (c != -1 && c != '-' && (c < '0' || c > '9')) c = read();   // skip whitespace
            if (c == -1) { eof = true; return 0; }
            boolean neg = false;
            if (c == '-') { neg = true; c = read(); }
            int r = 0;
            while (c >= '0' && c <= '9') { r = r * 10 + (c - '0'); c = read(); }
            return neg ? -r : r;
        }
    }

    public static void main(String[] args) throws IOException {
        InputStream in = args.length >= 1 ? new FileInputStream(args[0]) : System.in;
        FastReader fr = new FastReader(in);
        StringBuilder out = new StringBuilder();

        while (true) {
            int n = fr.nextInt();
            if (fr.eof) break;                          // end of the input file
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = fr.nextInt();
                ys[i] = fr.nextInt();
            }
            double ans = solve(xs, ys);
            out.append(String.format(Locale.US, "%.3f", ans)).append('\n');   // Locale.US => '.' as decimal point
        }

        if (args.length >= 2) Files.writeString(Paths.get(args[1]), out.toString());
        else System.out.print(out);
        System.out.flush();
    }
}