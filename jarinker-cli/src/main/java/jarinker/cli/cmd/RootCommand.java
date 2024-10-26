package jarinker.cli.cmd;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Do not change version property, it will be replaced by gradle.properties:2
 *
 * @author Freeman
 * @since 2024/10/15
 */
@Command(name = "jarinker", mixinStandardHelpOptions = true, version = "0.0.1")
public class RootCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // Show --help if no subcommand/options are provided
        spec.commandLine().usage(System.out);
    }
}
