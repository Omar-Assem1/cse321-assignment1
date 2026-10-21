# CSE321 Assignment 1: Divide and Conquer

## Problem 1: Selection benchmark
Randomized select (CLRS 9.2), deterministic median-of-medians select (CLRS 9.3)
and the naive sort-based method, compared on random arrays up to 10^7 elements.

## Problem 2: Minimum perimeter triangle
Divide and conquer in O(n log n): split by x, recurse, then check a strip around
the dividing line, sorted by y.

## Build
```
mkdir -p problem1/out problem2/out dist
javac -d problem1/out problem1/src/selection/*.java
javac -d problem2/out problem2/src/triangle/*.java
jar cfm dist/problem1.jar problem1/manifest.txt -C problem1/out . -C problem1/src .
jar cfm dist/problem2.jar problem2/manifest.txt -C problem2/out . -C problem2/src .
```

## Run
```
java -cp problem1/out selection.CorrectnessCheck
java -Xms2g -Xmx2g -cp problem1/out selection.Benchmark report/results.csv
java -jar dist/problem2.jar problem2/samples/sample.in      # or: < input > output
java -cp problem2/out triangle.StressTest
```