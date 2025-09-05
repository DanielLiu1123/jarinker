/**
 * Jarinker core API for JAR dependency analysis and shrinking.
 *
 * <p>This package provides simplified wrappers around jdeps:
 * <ul>
 *   <li>{@link jarinker.core.JdepsAnalyzer} - wrapper around jdeps for dependency analysis</li>
 *   <li>{@link jarinker.core.AnalysisResult} - wrapper for jdeps analysis results</li>
 *   <li>{@link jarinker.core.JarShrinker} - JAR shrinking operations</li>
 * </ul>
 *
 * <p>Example usage for dependency analysis:
 * <pre>{@code
 * var analyzer = JdepsAnalyzer.builder()
 *     .includeJdk(false)
 *     .build();
 *
 * var result = analyzer.analyze(sources, classpath);
 * System.out.println("Found " + result.getNodeCount() + " nodes");
 * }</pre>
 *
 * <p>Example usage for shrinking:
 * <pre>{@code
 * var shrinker = JarShrinker.builder()
 *     .inPlace(false)
 *     .build();
 *
 * var result = shrinker.shrink(jarPaths, reachableClasses, outputDir);
 * System.out.println("Reduced size by " + result.getReductionPercentage() + "%");
 * }</pre>
 *
 * @author Freeman
 */
@NullMarked
package jarinker.core;

import org.jspecify.annotations.NullMarked;
