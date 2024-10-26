package jarinker.cli.cmd.shrink;

import static jarinker.cli.util.Printer.print;

import jarinker.core.Jarinker;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * @author Freeman
 * @since 2024/10/16
 */
@Command(
        name = "shrink",
        mixinStandardHelpOptions = true,
        description = "Shrink the JAR files by removing unused classes.")
public class ShrinkCommand implements Callable<Integer> {

    @Option(
            names = {"-s", "--sources"},
            type = Path.class,
            description = "Source paths")
    private List<Path> sources = new ArrayList<>();

    //    @Option(
    //            names = {"-d", "--dependencies"},
    //            type = Path.class,
    //            description = "Dependency paths")
    //    private List<Path> dependencies = new ArrayList<>();
    //
    //    @Option(
    //            names = {"-o", "--output"},
    //            type = Path.class,
    //            description = "Output path")
    //    private Path output;

    @Override
    public Integer call() throws Exception {
        var r1 = Jarinker.scan(sources);

        var classFiles = r1.getClassFiles();
        var allClasses = r1.getAllClasses();
        var jarMap = r1.getSourceMap();

        print("Class Files: %d".formatted(classFiles.size()));
        print("");

        print("Found %d JARs:".formatted(jarMap.size()));

        int maxClassSizeLength = jarMap.values().stream()
                .mapToInt(classes -> Integer.toString(classes.size()).length())
                .max()
                .orElse(0);

        for (var en : jarMap.entrySet()) {
            var jar = en.getKey();
            var classes = en.getValue();
            print(String.format(" - %-" + maxClassSizeLength + "d classes in %s", classes.size(), jar.getName()));
        }

        print("");
        print("Total Classes: %d".formatted(allClasses.size()));

        return 0;
    }
}
