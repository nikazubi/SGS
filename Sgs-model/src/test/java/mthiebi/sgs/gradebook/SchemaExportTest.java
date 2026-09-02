package mthiebi.sgs.gradebook;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;

import javax.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds Hibernate metadata for the gradebook entities and writes the DDL to
 * db/001_schema.sql.
 * <p>
 * Two jobs. It validates the mappings - javac happily compiles an entity whose
 * annotations Hibernate will reject - and it produces the script to review and
 * run against production, since ddl-auto stays out of prod. Regenerate whenever
 * the entities change and diff the result.
 */
class SchemaExportTest {

    private static final String PACKAGE_DIR =
            "src/main/java/mthiebi/sgs/gradebook/model";

    @Test
    void exportsGradebookSchema() throws IOException {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.SQLServer2012Dialect")
                // The database collation has no code page for Georgian, so a
                // varchar column would store ?????????? instead of მათემატიკა.
                .applySetting("hibernate.use_nationalized_character_data", "true")
                .build();

        try {
            MetadataSources sources = new MetadataSources(registry);
            for (Class<?> entity : entityClasses()) {
                sources.addAnnotatedClass(entity);
            }
            Metadata metadata = sources.buildMetadata();

            Path out = Paths.get("..", "db", "001_schema.sql").normalize();
            Files.createDirectories(out.getParent());
            Files.deleteIfExists(out);

            SchemaExport export = new SchemaExport();
            export.setDelimiter(";");
            export.setFormat(true);
            export.setOutputFile(out.toString());
            export.execute(EnumSet.of(TargetType.SCRIPT), SchemaExport.Action.CREATE, metadata);

            assertTrue(Files.exists(out), "schema script was not written");
            assertTrue(Files.size(out) > 0, "schema script is empty");
            System.out.println("schema written to " + out.toAbsolutePath());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private List<Class<?>> entityClasses() throws IOException {
        File dir = new File(PACKAGE_DIR);
        assertTrue(dir.isDirectory(), "entity package not found at " + dir.getAbsolutePath());
        try (Stream<Path> files = Files.list(dir.toPath())) {
            return files
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".java"))
                    .map(n -> "mthiebi.sgs.gradebook.model." + n.substring(0, n.length() - 5))
                    .map(SchemaExportTest::load)
                    .filter(c -> c.isAnnotationPresent(Entity.class))
                    .collect(Collectors.toList());
        }
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
