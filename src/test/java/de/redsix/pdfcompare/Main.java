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

public class Main {

    public static void main(String[] args) throws IOException {

        String file1="expected.pdf";
        String file2="actual.pdf";

        Instant start = Instant.now();
        final CompareResult result = new PdfComparator().compare(file1, file2, "ignore.conf");
        if (result.isNotEqual()) {
            System.out.println("Differences found!");
        }
        final boolean isEquals = result.writeTo("test");
        Instant end = Instant.now();
        System.out.println("Duration: " + Duration.between(start, end).toMillis() + "ms");
    }
}
