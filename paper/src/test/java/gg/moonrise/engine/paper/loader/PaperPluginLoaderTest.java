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

        assertTrue(coordinates.contains("org.incendo:cloud-paper:jar:2.0.0"));
        assertFalse(coordinates.contains("org.incendo:cloud-paper:jar:2.0.0-beta.10"));
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
