package jarinker.cli;

import static io.goodforgod.graalvm.hint.annotation.ReflectionHint.AccessType.ALL_DECLARED;
import static io.goodforgod.graalvm.hint.annotation.ReflectionHint.AccessType.ALL_PUBLIC;

import io.goodforgod.graalvm.hint.annotation.ReflectionHint;
import jarinker.cli.cmd.AnalyzeCommand;
import jarinker.cli.cmd.RootCommand;
import jarinker.cli.cmd.ShrinkCommand;
import picocli.AutoComplete;
import picocli.CommandLine;

/**
 * Main entry point for Jarinker CLI.
 * Delegates to RootCommand for actual command processing.
 *
 * @author Freeman
 */
@ReflectionHint(
        types = AutoComplete.GenerateCompletion.class,
        value = {ALL_DECLARED, ALL_PUBLIC
        }) // for GenerateCompletion, picocli only generate graalvm reflection config for "your own" commands
public class Cli {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RootCommand())
                .addSubcommand("completion", new AutoComplete.GenerateCompletion())
                .addSubcommand("analyze", new AnalyzeCommand())
                .addSubcommand("shrink", new ShrinkCommand())
                .execute(args);

        System.exit(exitCode);
    }
}
