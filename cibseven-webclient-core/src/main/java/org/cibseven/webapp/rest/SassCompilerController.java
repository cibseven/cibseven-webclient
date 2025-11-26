/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.rest;

import de.larsgrefer.sass.embedded.CompileSuccess;
import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassCompiler;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import de.larsgrefer.sass.embedded.importer.ClasspathImporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Tag(name = "SASS Compiler", description = "Endpoints for compiling and serving CSS from SASS/SCSS files")
@ApiResponses({
    @ApiResponse(responseCode = "500", description = "An unexpected system error occurred")
})
@RestController
public class SassCompilerController {

	private static final Map<String, String> cachedCss = new ConcurrentHashMap<>();
	private static final String DEFAULT_THEME_NAME = "generic";
	private static final String DEFAULT_BOOTSTRAP_VERSION = "bs5"; // default to v5
	private static final String PATH_THEMES_FOLDER_NAME = "/themes";
	private static final String VARIABLE_FILE_NAME = "variables.scss"; // for Bootstrap 5.x
	private static final String VARIABLE_FILE_NAME_BS4 = "variables-bootstrap4.scss"; // for Bootstrap 4.x

    @PostConstruct
    public void generateCssOnStartup() throws IOException, SassCompilationFailedException {
        if (cachedCss.isEmpty()) {
            cachedCss.putAll(generateBootstrapCss()); // pre-generate bs5 by default
        }
    }

    @Operation(
        summary = "Get compiled CSS for a theme",
        description = "Returns compiled CSS for the specified theme, optionally minified"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully compiled and returned CSS"),
        @ApiResponse(responseCode = "404", description = "Theme not found"),
        @ApiResponse(responseCode = "500", description = "Error generating CSS")
    })
    @GetMapping("/css")
    public ResponseEntity<String> getCss(
            @Parameter(description = "Theme name to compile CSS for", example = "generic")
            @RequestParam(defaultValue = DEFAULT_THEME_NAME) String theme,

            @Parameter(description = "Bootstrap version to target. Use '5' (default) or '4' / '4.5.0' for Bootstrap 4.", example = "5")
            @RequestParam(name = "bootstrap", defaultValue = DEFAULT_BOOTSTRAP_VERSION) String bootstrapVersion,

            @Parameter(description = "Whether to minify the CSS output", example = "true")
            @RequestParam(defaultValue = "true") boolean minify) {
        try {
            String versionKey = isBootstrap4(bootstrapVersion) ? "bs4" : "bs5";
            String key = cacheKey(theme, versionKey);

            String css = cachedCss.computeIfAbsent(key, k -> {
                try {
                    return generateSingleCss(theme, bootstrapVersion);
                } catch (IOException | SassCompilationFailedException e) {
                    return null;
                }
            });

            if (css == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("[Error][Sass compiler] Theme not found");
            }

            return ResponseEntity.ok().body(minify ? minifyCss(css) : css);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("[Error][Sass compiler] Error generating CSS");
        }
    }

    private Map<String, String> generateBootstrapCss() throws IOException, SassCompilationFailedException {
        Map<String, String> cssMap = new ConcurrentHashMap<>();

        try (SassCompiler sassCompiler = SassCompilerFactory.bundled()) {
            sassCompiler.registerImporter(new ClasspathImporter().autoCanonicalize());
            sassCompiler.setSilent(true);

            // Access the themes folder from the classpath
            URL resource = getClass().getResource(PATH_THEMES_FOLDER_NAME);
            if (resource == null) {
                throw new IOException("[Error][Sass compiler] Themes folder not found.");
            }

            File themesFolder = new File(resource.toURI());
            File[] themeDirs = themesFolder.listFiles(File::isDirectory);

            if (themeDirs != null) {
                for (File themeDir : themeDirs) {
                    String themeName = themeDir.getName();
                    String css = compileThemeCss(sassCompiler, themeDir);
                    if (css != null) {
                        cssMap.put(cacheKey(themeName, "bs5"), css);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException("[Error][Sass compiler] Invalid URI for themes folder.", e);
        }

        return cssMap;
    }

    private String generateSingleCss(String theme, String bootstrapVersion) throws IOException, SassCompilationFailedException {
        URL themeUrl = getClass().getResource(PATH_THEMES_FOLDER_NAME + "/" + theme);
        if (themeUrl == null) {
            return null;
        }

        try (SassCompiler sassCompiler = SassCompilerFactory.bundled()) {
            sassCompiler.registerImporter(new ClasspathImporter().autoCanonicalize());
            sassCompiler.setSilent(true);

            File themeDir = new File(themeUrl.toURI());
            if (isBootstrap4(bootstrapVersion)) {
                return compileThemeCssBs4(sassCompiler, themeDir);
            }
            return compileThemeCss(sassCompiler, themeDir);
        } catch (URISyntaxException e) {
            throw new IOException("[Error][Sass compiler] Invalid URI for theme folder.", e);
        }
    }

    private String compileThemeCss(SassCompiler sassCompiler, File themeDir) throws IOException, SassCompilationFailedException {
        File variablesFile = new File(themeDir, VARIABLE_FILE_NAME);
        if (!variablesFile.exists()) {
            return null;
        }

        CompileSuccess compileSuccess = sassCompiler.compileFile(variablesFile);
        return compileSuccess.getCss();
    }

    private String compileThemeCssBs4(SassCompiler sassCompiler, File themeDir) throws IOException, SassCompilationFailedException {
        File variablesFile = new File(themeDir, VARIABLE_FILE_NAME_BS4);
        if (!variablesFile.exists()) {
            return null;
        }
        CompileSuccess compileSuccess = sassCompiler.compileFile(variablesFile);
        return compileSuccess.getCss();
    }

    private static boolean isBootstrap4(String bootstrapVersion) {
        if (bootstrapVersion == null) return false;
        String v = bootstrapVersion.trim();
        return v.startsWith("bs4") || v.startsWith("4");
    }

    private static String cacheKey(String theme, String versionKey) {
        return theme + "|" + versionKey;
    }

    private static String minifyCss(String css) {
        // Remove CSS comments
        css = css.replaceAll("/\\*[^*]*\\*+([^/][^*]*\\*+)*/", "");

        // Remove unnecessary white spaces, newlines, and tabs
        css = css.replaceAll("\\s+", " ");  // Replace sequences of white spaces with a single space
        css = css.replaceAll("(?<=\\S)\\s*\\{\\s*", "{");  // Remove spaces before {
        css = css.replaceAll("\\s*\\}", "}");  // Remove spaces after }
        css = css.replaceAll("\\s*;\\s*", ";");  // Remove spaces around ;

        // Trim the CSS to remove leading and trailing spaces
        css = css.trim();

        return css;
    }
}
