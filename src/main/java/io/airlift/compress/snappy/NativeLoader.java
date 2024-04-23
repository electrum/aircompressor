package io.airlift.compress.snappy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class NativeLoader
{
    private NativeLoader() {}

    public static SymbolLookup loadLibrary(String name)
    {
        String libraryPath = getLibraryPath(name);
        URL url = NativeLoader.class.getResource(libraryPath);
        if (url == null) {
            throw new LinkageError("Library not found: " + libraryPath);
        }
        Path path = temporaryFile(name, url);
        return SymbolLookup.libraryLookup(path, Arena.ofAuto());
    }

    private static Path temporaryFile(String name, URL url)
    {
        try {
            File file = File.createTempFile(name, null);
            file.deleteOnExit();
            try (InputStream in = url.openStream()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return file.toPath();
        }
        catch (IOException e) {
            throw new LinkageError("Failed to create temporary file: " + e.getMessage(), e);
        }
    }

    private static String getLibraryPath(String name)
    {
        return "/aircompressor/" + getPlatform() + "/" + System.mapLibraryName(name);
    }

    private static String getPlatform()
    {
        String name = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        return (name + "-" + arch).replace(' ', '_');
    }
}
