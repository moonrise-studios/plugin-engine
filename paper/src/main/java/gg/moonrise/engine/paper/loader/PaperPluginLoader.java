package gg.moonrise.engine.paper.loader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Represents the SkylandsPluginLoader class.
 */
public abstract class PaperPluginLoader implements PluginLoader {

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

        resolver.addDependency(dependency("org.springframework:spring-context:6.2.13"));

        resolver.addDependency(dependency("jakarta.annotation:jakarta.annotation-api:3.0.0"));

        resolver.addDependency(dependency("de.exlll:configlib-yaml:4.8.1"));

        resolver.addDependency(dependency("gg.moonrise.moss:moss-paper:1.2.2"));

        resolver.addDependency(dependency("org.incendo:cloud-paper:2.0.0-beta.10"));
        resolver.addDependency(dependency("org.incendo:cloud-annotations:2.0.0"));

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
     * Executes dependency.
     * @param coords the coords
     * @return the result
     */

    public Dependency dependency(String coords) {
        return new Dependency(new DefaultArtifact(coords), null);
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
