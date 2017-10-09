/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

import groovy.text.SimpleTemplateEngine

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Check/generate and repair Readme.adoc for a honeycomb module.
 */
class ReadmeGenerator {

    static final def DEFAULT_README = ReadmeGenerator.getResource("/readme/readmeDefaultContent").text

    static final def ADOC_FOLDER = "asciidoc"
    static final def README = "Readme"
    static final def README_FILE = "${README}.adoc"
    static final def README_HTML = "${README}.html"
    static final def SITE_FOLDER = "site"
    static final def INDEX_HTML = "index.html"

    public static void checkReadme(project, properties, log) {
        log.info "Checking ${ADOC_FOLDER}/${README_FILE}"
        def asciidoc = Paths.get(project.getBasedir().toString(), ADOC_FOLDER)
        def readme = Paths.get(asciidoc.toString(), README_FILE)
        if (!Files.exists(readme)) {
            log.info "Generating ${readme}"
            Files.createDirectories(asciidoc)
            Files.createFile(readme)
            readme.toFile().text = new SimpleTemplateEngine().createTemplate(DEFAULT_README)
                    .make(["artifactId": project.artifactId])
                    .toString()
        }
    }

    public static void fixSite(project, properties, log) {
        def index = Paths.get(project.build.directory.toString(), SITE_FOLDER, INDEX_HTML)
        if (Files.exists(index)) {
            log.info "Fixing links in generated site"
            def html = index.toFile().text
            log.info "Fixing ${ADOC_FOLDER} ${README_HTML} link"
            index.toFile().text = html.replaceAll("[./]*${README}\\.html", README_HTML)
        }
    }
}
