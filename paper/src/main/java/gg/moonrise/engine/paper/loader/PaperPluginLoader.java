package gg.moonrise.engine.paper.loader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the SkylandsPluginLoader class.
 */
public abstract class PaperPluginLoader implements PluginLoader {

    private static final List<String> DEFAULT_LIBRARIES = List.of(
            "org.springframework:spring-context:6.2.13",
            "jakarta.annotation:jakarta.annotation-api:3.0.0",
            "de.exlll:configlib-yaml:4.8.1",
            "gg.moonrise.moss:moss-paper:1.2.2",
            "org.incendo:cloud-paper:2.0.1",
            "org.incendo:cloud-annotations:2.0.0"
    );

    /**
     * Executes classloader.
     * @param builder the builder
     */
    @Override
    public void classloader(PluginClasspathBuilder builder) {
        onClassLoaderInit(builder);

        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("central", "default", getDefaultMavenCentralMirror()).build());
        resolver.addRepository(new RemoteRepository.Builder("moonrise-releases", "default", "https://repo.moonrise.gg/repository/maven-releases/").build());
        resolver.addRepository(new RemoteRepository.Builder("moonrise-snapshots", "default", "https://repo.moonrise.gg/repository/maven-snapshots/").build());

        for (String coordinates : runtimeLibraries()) {
            resolver.addDependency(dependency(coordinates));
        }

        addLibraries(resolver);

        builder.addLibrary(resolver);
    }

    /**
     * Executes onClassLoaderInit.
     * @param builder the builder
     */

    public void onClassLoaderInit(PluginClasspathBuilder builder) {
        // Override to add functionality
    }

    /**
     * Executes addLibraries.
     * @param resolver the resolver
     */

    public void addLibraries(MavenLibraryResolver resolver) {
        // Override to add functionality
    }

    /**
     * Returns extra runtime libraries as Maven coordinates, such as {@code groupId:artifactId:version}.
     * A library with the same {@code groupId:artifactId} as an engine default replaces that default,
     * so a plugin can pin another version without both versions being resolved.
     * @return the library coordinates
     */
    public List<String> libraries() {
        return List.of();
    }

    /**
     * Executes dependency.
     * @param coords the coords
     * @return the result
     */

    public Dependency dependency(String coords) {
        return new Dependency(new DefaultArtifact(coords), null);
    }

    private List<String> runtimeLibraries() {
        Map<String, String> libraries = new LinkedHashMap<>();
        for (String coordinates : DEFAULT_LIBRARIES) {
            libraries.put(libraryKey(coordinates), coordinates);
        }
        for (String coordinates : Objects.requireNonNull(libraries(), "libraries")) {
            Objects.requireNonNull(coordinates, "library coordinates");
            libraries.put(libraryKey(coordinates), coordinates);
        }
        return List.copyOf(libraries.values());
    }

    private static String libraryKey(String coordinates) {
        String[] parts = coordinates.split(":");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid Maven coordinates " + coordinates);
        }
        return parts[0] + ":" + parts[1];
    }

    private static String getDefaultMavenCentralMirror() {
        String central = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
        if (central == null) {
            central = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
        }
        if (central == null) {
            central = "https://maven-central.storage-download.googleapis.com/maven2";
        }
        return central;
    }
}
