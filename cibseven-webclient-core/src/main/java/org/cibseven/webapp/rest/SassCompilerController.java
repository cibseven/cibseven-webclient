package org.cibseven.webapp.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.web.bind.annotation.RestController;

import de.larsgrefer.sass.embedded.CompileSuccess;
import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassCompiler;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import de.larsgrefer.sass.embedded.importer.ClasspathImporter;
import jakarta.annotation.PostConstruct;

@RestController
public class SassCompilerController {

    private static final String THEME_NAME = "cib";
    private static final String PATH_THEMES_FOLDER = "/themes";
    private static final String VARIABLE_FILE_NAME = "variables.scss";
    private static final String EXTENSION_FILE = ".css";
    private static final String OUTPUT_PATH_FILES = "bootstrap/";
    
    @PostConstruct
    public void generateCssOnStartup() throws IOException, SassCompilationFailedException, URISyntaxException {
        String css = generateSingleCss(THEME_NAME);
        if (css != null) {
            saveCssToFile(css);
        } else {
            System.err.println("[Error][Sass compiler] Failed to generate CSS for theme: " + THEME_NAME);
        }
    }
      
    private String generateSingleCss(String theme) throws IOException, SassCompilationFailedException {
    	
    	Path variablesFilePath = Paths.get(PATH_THEMES_FOLDER, theme);
        URL resource = getClass().getResource(variablesFilePath.toString().replace("\\", "/"));

        if (resource == null) {
            System.err.println("[Error][Sass compiler] Theme file not found: " + variablesFilePath);
            return null;
        }

        try (SassCompiler sassCompiler = SassCompilerFactory.bundled()) {
            sassCompiler.registerImporter(new ClasspathImporter().autoCanonicalize());
            return compileThemeCss(new File(resource.toURI()), sassCompiler);
        } catch (URISyntaxException e) {
            throw new IOException("[Error][Sass compiler] Invalid URI for theme folder.", e);
        }
    }

    private void saveCssToFile(String css) throws IOException, URISyntaxException, SassCompilationFailedException {
        URL resourceUrl = getClass().getClassLoader().getResource(PATH_THEMES_FOLDER);
        if (resourceUrl == null) {
            throw new IllegalStateException("Resource folder not found: " + PATH_THEMES_FOLDER);
        }

        Path assetsPath = Paths.get(resourceUrl.toURI());
        File themesFolder = assetsPath.toFile();
        try (SassCompiler sassCompiler = SassCompilerFactory.bundled()) {
                sassCompiler.registerImporter(new ClasspathImporter().autoCanonicalize());
        
            File[] themeDirs = themesFolder.listFiles(File::isDirectory);

	        if (themeDirs != null) {
	            for (File themeDir : themeDirs) {
	                String themeName = themeDir.getName();
	
	                String themeCss = compileThemeCss(themeDir, sassCompiler);
	
	                if (themeCss != null) {
	                    saveThemeCssToFile(themeName, themeCss);
	                }
	            }
	        }
	    } catch (SassCompilationFailedException e) {
	        System.err.println("[Error][Sass compiler] saveCssToFile " + e.getMessage());
	    }
    }

    private void saveThemeCssToFile(String themeName, String css) throws IOException, URISyntaxException {
        URL resourceUrl = getClass().getClassLoader().getResource(OUTPUT_PATH_FILES);
        if (resourceUrl == null) {
            throw new IllegalStateException("Resource folder not found: " + OUTPUT_PATH_FILES);
        }

        Path assetsPath = Paths.get(resourceUrl.toURI());
        File outputDir = assetsPath.toFile();

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            System.out.println("⚠️ Error creating folder: " + outputDir.getAbsolutePath());
            return;
        }

        File outputFile = new File(outputDir, themeName + EXTENSION_FILE);

        try (FileWriter writer = new FileWriter(outputFile, false)) { // `false` to overwrite the file
            writer.write(css);
            System.out.println("✅ CSS saved for theme '" + themeName + "' in: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Error writing the CSS for theme '" + themeName + "' in the file.");
            e.printStackTrace();
        }
    }
    
    public static String readFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }

        return new String(Files.readAllBytes(file.toPath()));
    }
    
	private String compileThemeCss(File themeDir, SassCompiler sassCompiler) throws IOException, SassCompilationFailedException {
        File variablesFile = new File(themeDir, VARIABLE_FILE_NAME);

        if (!variablesFile.exists()) {
            System.out.println("[Error] Theme file not found: " + variablesFile.getAbsolutePath());
            return null;
        }

        try {
            CompileSuccess compileSuccess = sassCompiler.compileFile(variablesFile);
            return compileSuccess.getCss();
        } catch (SassCompilationFailedException e) {
            System.err.println("[Error][Sass compiler] Compilation failed for theme: " + themeDir.getName() + e.getMessage());
            return null;
        }
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
