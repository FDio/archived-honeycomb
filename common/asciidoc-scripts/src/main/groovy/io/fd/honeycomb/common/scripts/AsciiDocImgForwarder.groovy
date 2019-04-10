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

package io.fd.honeycomb.common.scripts

import java.nio.file.Files
import java.nio.file.Paths

class AsciiDocImgForwarder {

    static final def ADOC_FOLDER = "asciidoc"
    static final def SITE_FOLDER = "site"
    static final def PNG_EXTENSION = "png"

    /**
     * Copies generated images to site folder
     * */
    public static void copyGeneratedImages(project, properties, log){

        def sourcePath = Paths.get(project.basedir.toString(), ADOC_FOLDER)
        def destinationPathString = Paths.get(project.build.directory.toString(), SITE_FOLDER).toString()

        log.info "Copying generated asciidoc images from ${sourcePath} to ${destinationPathString}"
        Files.walk(sourcePath)
                .filter({ path -> path.toString().endsWith(PNG_EXTENSION) })
                .forEach({ sourceFilePath ->
                    def targetFilePath = Paths.get(destinationPathString, sourceFilePath.getFileName().toString())
                    log.info "Copying ${sourceFilePath} to ${targetFilePath}"
                    Files.copy(sourceFilePath, targetFilePath) })
    }
}
