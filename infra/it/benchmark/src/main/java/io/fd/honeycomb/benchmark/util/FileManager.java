/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.benchmark.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface FileManager {

    FileManager INSTANCE = new FileManager() {};

    default Path createTempFile(String config) throws IOException {
        return Files.createTempFile("hcbenchmark", config);
    }

    default void deleteFile(final Path toDelete) {
        if (toDelete != null) {
            try {
                Files.delete(toDelete);
            } catch (IOException e) {
                // NOOP, dont care about temp files too much
            }
        }
    }

}
