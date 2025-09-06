package jarinker.cli.cmd;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

/**
 * Root command for Jarinker CLI.
 *
 * @author Freeman
 */
@Command(
        name = "jarinker",
        description = "A JAR shrinker that analyzes dependencies and removes unused classes",
        version = "0.1.0",
        mixinStandardHelpOptions = true)
public class RootCommand implements Runnable {

    @Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        // Show help when no subcommand is specified
        spec.commandLine().usage(System.out);
    }
}
