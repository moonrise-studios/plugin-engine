package gg.moonrise.engine.paper.loader;

import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.library.ClassPathLibrary;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperPluginLoaderTest {

    @Test
    void registersStableCloudPaperRuntime() throws ReflectiveOperationException {
        CapturingClasspathBuilder builder = new CapturingClasspathBuilder();
        PaperPluginLoader loader = new PaperPluginLoader() {
        };

        loader.classloader(builder);

        List<String> coordinates = dependencies(builder.resolver()).stream()
                .map(dependency -> dependency.getArtifact().toString())
                .toList();

        assertTrue(coordinates.contains("org.incendo:cloud-paper:jar:2.0.1"));
        assertFalse(coordinates.contains("org.incendo:cloud-paper:jar:2.0.0"));
        assertFalse(coordinates.contains("org.incendo:cloud-paper:jar:2.0.0-beta.10"));
    }

    @Test
    void libraryReplacesTheEngineDefaultWithTheSameArtifact() throws ReflectiveOperationException {
        List<String> coordinates = resolvedCoordinates(loader(List.of("org.incendo:cloud-paper:2.0.2")));

        assertEquals(1, coordinates.stream().filter(value -> value.startsWith("org.incendo:cloud-paper:")).count());
        assertTrue(coordinates.contains("org.incendo:cloud-paper:jar:2.0.2"));
        assertTrue(coordinates.contains("org.incendo:cloud-annotations:jar:2.0.0"));
    }

    @Test
    void libraryWithANewArtifactIsAddedAfterTheDefaults() throws ReflectiveOperationException {
        List<String> coordinates = resolvedCoordinates(loader(List.of("com.github.ben-manes.caffeine:caffeine:3.2.3")));

        assertEquals("com.github.ben-manes.caffeine:caffeine:jar:3.2.3", coordinates.getLast());
        assertTrue(coordinates.contains("org.incendo:cloud-paper:jar:2.0.1"));
    }

    @Test
    void addLibrariesStillRunsAfterTheDefaults() throws ReflectiveOperationException {
        PaperPluginLoader loader = new PaperPluginLoader() {
            @Override
            public void addLibraries(MavenLibraryResolver resolver) {
                resolver.addDependency(dependency("com.github.ben-manes.caffeine:caffeine:3.2.3"));
            }
        };

        assertEquals("com.github.ben-manes.caffeine:caffeine:jar:3.2.3", resolvedCoordinates(loader).getLast());
    }

    @Test
    void libraryWithoutAVersionIsRejected() {
        PaperPluginLoader loader = loader(List.of("org.example:library"));

        assertThrows(IllegalArgumentException.class, () -> loader.classloader(new CapturingClasspathBuilder()));
    }

    private static PaperPluginLoader loader(List<String> libraries) {
        return new PaperPluginLoader() {
            @Override
            public List<String> libraries() {
                return libraries;
            }
        };
    }

    private static List<String> resolvedCoordinates(PaperPluginLoader loader) throws ReflectiveOperationException {
        CapturingClasspathBuilder builder = new CapturingClasspathBuilder();
        loader.classloader(builder);
        return dependencies(builder.resolver()).stream()
                .map(dependency -> dependency.getArtifact().toString())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Dependency> dependencies(MavenLibraryResolver resolver)
            throws ReflectiveOperationException {
        Field field = MavenLibraryResolver.class.getDeclaredField("dependencies");
        field.setAccessible(true);
        return List.copyOf((List<Dependency>) field.get(resolver));
    }

    private static final class CapturingClasspathBuilder implements PluginClasspathBuilder {

        private final List<ClassPathLibrary> libraries = new ArrayList<>();

        @Override
        public PluginClasspathBuilder addLibrary(ClassPathLibrary library) {
            libraries.add(library);
            return this;
        }

        @Override
        public PluginProviderContext getContext() {
            return null;
        }

        private MavenLibraryResolver resolver() {
            assertEquals(1, libraries.size());
            assertTrue(libraries.getFirst() instanceof MavenLibraryResolver);
            return (MavenLibraryResolver) libraries.getFirst();
        }
    }
}
