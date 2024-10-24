package jarinker.core;

import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class JarinkerTest {

    @Test
    @SneakyThrows
    void testShrinkJars() {
        //        var serverJar = new JarFile(new File(
        //                Jarinker.class.getClassLoader().getResource("moego-server.jar").toURI()));
        //        assertThat(serverJar).isNotNull();
        //        var libJar = new JarFile(new File(Jarinker.class
        //                .getClassLoader()
        //                .getResource("moego-api-java.jar")
        //                .toURI()));
        //        assertThat(libJar).isNotNull();
        //
        //        var shrunkJar = Jarinker.shrinkJars(List.of(serverJar), List.of(libJar));
        //
        //        System.out.println(shrunkJar);
    }

    //    @Test
    void shrinkJars2() {
        long start = System.currentTimeMillis();
        Jarinker.shrinkJars(
                List.of(Path.of("src/test/resources/moego-server.jar")),
                List.of(Path.of("src/test/resources/moego-api-java.jar")));
        System.out.println("sync Time: " + (System.currentTimeMillis() - start));
    }

    @Test
    void shrinkJars() {
        long start = System.currentTimeMillis();
        Jarinker.shrinkJars(
                List.of(Path.of("src/test/resources/moego-server.jar")),
                List.of(Path.of("src/test/resources/moego-api-java.jar")));
        System.out.println("async Time: " + (System.currentTimeMillis() - start));
    }
}
