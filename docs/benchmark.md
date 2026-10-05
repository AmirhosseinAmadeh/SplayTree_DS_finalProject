keys=200000, queries per run=2000000, JVM=17.0.12

## contains() throughput (million lookups / second)
| access pattern | SplayTree | TreeSet | tree height after run |
|---|---:|---:|---:|
| uniform random | 1.1 | 3.9 | 44 |
| skewed (Zipf, hot keys) | 2.0 | 5.8 | 45 |
| hot set (20 keys) | 12.1 | 28.0 | 46 |
| sequential sweep | 10.4 | 19.2 | 200000 |

## Range sum over 5% of the keys (queries / second)
| structure | queries/s |
|---|---:|
| SplayTree.sumRange (augmented, O(log n)) | 593,912 |
| TreeSet.subSet + loop (O(k)) | 9,986 |

speed-up on range sums: 59x
