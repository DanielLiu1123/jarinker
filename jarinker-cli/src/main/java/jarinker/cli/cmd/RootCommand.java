package jarinker.cli.cmd;

import picocli.CommandLine;

/**
 * Root command for Jarinker CLI.
 *
 * @author Freeman
 */
@CommandLine.Command(
        name = "jarinker",
        description = "A JAR shrinker that analyzes dependencies and removes unused classes",
        version = "0.1.0",
        mixinStandardHelpOptions = true)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        // Show help when no subcommand is specified
        spec.commandLine().usage(System.out);
    }
}
