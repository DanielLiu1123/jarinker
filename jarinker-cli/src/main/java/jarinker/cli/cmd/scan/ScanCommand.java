package jarinker.cli.cmd.scan;

import static jarinker.cli.util.Printer.print;

import jarinker.core.Jarinker;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * @author Freeman
 */
@Command(name = "scan", mixinStandardHelpOptions = true, description = "Scan class files in paths.")
public class ScanCommand implements Runnable {

    @Option(
            names = {"-s", "--sources"},
            type = Path.class,
            description = "The paths of the sources.")
    private List<Path> sources = new ArrayList<>();

    @Override
    public void run() {
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
    }
}
