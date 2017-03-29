/*
 * Copyright 2016 Malte Finsterwalder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.redsix.pdfcompare;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

//        String file1 = "expected.pdf";
//        String file2 = "actual.pdf";
//        String file1 = "/home/malte/projects/Testcases/HML/DirektRente/8257926_1/8257926_1_004.pdf";
//        String file2 = "/home/malte/projects/Testcases/HML/DirektRente/8257926_1/x.pdf";
        String file1 = "/home/malte/long_chethan.pdf";
        String file2 = "/home/malte/long_chethan_2.pdf";

//        CompareResult result = null;

        final ExecutorService executor = Executors.newSingleThreadExecutor();
//        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < 1; i++) {
            Instant start = Instant.now();
//            final CompareResult result = new DiskUsingCompareResult();
            final CompareResult result = new CompareResultWithPageOverflow(20);
            new PdfComparator(file1, file2, result).withIgnore("ignore.conf")
                    .withExecutor(executor)
                    .compare().writeTo("out");
            Instant end = Instant.now();
            System.out.println("Duration: " + Duration.between(start, end).toMillis() + "ms");
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
//        printMemory("finished");
//        if (result.isNotEqual()) {
//            System.out.println("Differences found!");
//        }
//        result.writeTo("test_with_ignore");
//
//        start = Instant.now();
//        final CompareResult result2 = new PdfComparator(file1, file2).compare();
//        end = Instant.now();
//        System.out.println("Duration: " + Duration.between(start, end).toMillis() + "ms");
//        if (result2.isNotEqual()) {
//            System.out.println("Differences found!");
//        }
//        result2.writeTo("test");
    }


    private static long oldTotal;
    private static long oldFree;

    public static void printMemory(final String s) {
        final long totalMemory = Runtime.getRuntime().totalMemory();
        final long freeMemory = Runtime.getRuntime().freeMemory();
        final long consumed = totalMemory - freeMemory;
        System.out.println("==========================================================================");
        System.out.println("Memory " + s);
        System.out.printf("Total Memory: %6dMB  |  %d\n", toMB(totalMemory), toMB(totalMemory - oldTotal));
        System.out.printf("Free Memory:  %6dMB  |  %d\n", toMB(freeMemory), toMB(freeMemory - oldFree));
        System.out.printf("Consumed:     %6dMB  |  %d\n", toMB(consumed), toMB(consumed - (oldTotal - oldFree)));
        System.out.println("==========================================================================");
        oldTotal = totalMemory;
        oldFree = freeMemory;
    }

    private static long toMB(final long memory) {
        return memory / (1024 * 1024);
    }
}
