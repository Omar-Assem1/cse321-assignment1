import csv, math, os, subprocess
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

JDK = os.environ.get("BENCH_JDK", "21.0.11-ea")

def sh(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True).stdout.strip()

rows = list(csv.DictReader(open("report/results.csv")))
n    = [int(r["n"]) for r in rows]
rand = [float(r["randomized_ms"]) for r in rows]
mom  = [float(r["median_of_medians_ms"]) for r in rows]
srt  = [float(r["sort_ms"]) for r in rows]
ns   = lambda ms, size: ms * 1e6 / size
lab  = lambda size: "10^%d" % round(math.log10(size))

series = [("Randomized select", rand, "o"), ("Median of medians", mom, "s"), ("Sort (Arrays.sort)", srt, "^")]
plt.figure(figsize=(7, 5))
for name, ys, m in series:
    plt.loglog(n, ys, m + "-", label=name)
plt.xlabel("n"); plt.ylabel("average time (ms)")
plt.title("Selection time vs input size (JDK %s)" % JDK)
plt.legend(); plt.grid(True, which="both", alpha=0.3)
plt.savefig("report/plot_time.png", dpi=150, bbox_inches="tight")

plt.figure(figsize=(7, 5))
for name, ys, m in series:
    plt.semilogx(n, [ns(t, x) for t, x in zip(ys, n)], m + "-", label=name)
plt.xlabel("n"); plt.ylabel("time per element (ns)")
plt.title("Time per element vs input size (JDK %s)" % JDK)
plt.legend(); plt.grid(True, which="both", alpha=0.3)
plt.savefig("report/plot_per_element.png", dpi=150, bbox_inches="tight")

t1 = "| n | randomized (ms) | median of medians (ms) | sort (ms) |\n|---|---|---|---|\n"
t2 = "| n | randomized | median of medians | sort |\n|---|---|---|---|\n"
for i, x in enumerate(n):
    t1 += "| %s | %.3f | %.3f | %.3f |\n" % (lab(x), rand[i], mom[i], srt[i])
    t2 += "| %s | %.1f | %.1f | %.1f |\n" % (lab(x), ns(rand[i], x), ns(mom[i], x), ns(srt[i], x))

section = """### 1.3 Results
Main run: JDK %(jdk)s. Average time in ms:

%(t1)s
Time per element (ns):

%(t2)s
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

**Ranking (JDK %(jdk)s).** Randomized select is fastest at every size. Median of medians
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

**Conclusion.** On JDK %(jdk)s, selection wins clearly and the gap grows with n, as the
theory predicts, and randomized select is the fastest overall. On JDK 25 the library
sort is competitive up to about 10^6 elements. Both runs agree that selection runs in
linear time and that median of medians pays a large constant factor for its worst-case
guarantee.

""" % {"jdk": JDK, "t1": t1, "t2": t2}

cpu   = sh("lscpu | grep 'Model name' | sed 's/.*: *//'")
cores = sh("nproc")
ram   = sh("free -h | awk '/Mem:/ {print $2}'")
os_   = sh("grep PRETTY_NAME /etc/os-release | cut -d= -f2 | tr -d '\"'")
select_code = "```java\n" + open("problem1/src/selection/Select.java").read() + "\n```"

t = open("report/report_template.md").read()
a = t.index("### 1.3 Results")
b = t.index("## Problem 2")
t = t[:a] + section + t[b:]
t = (t.replace("@@CPU@@", "%s (%s threads)" % (cpu, cores)).replace("@@RAM@@", ram)
      .replace("@@OS@@", os_).replace("@@JDK@@", JDK).replace("@@SELECT@@", select_code))
open("report/report.md", "w").write(t)
print("CPU:", cpu, "| RAM:", ram, "| OS:", os_, "| JDK:", JDK)
print("leftover @@ markers:", t.count("@@"))
