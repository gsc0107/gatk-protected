package org.broadinstitute.hellbender.tools.exome.gcbias;

import org.broadinstitute.hellbender.cmdline.*;
import org.broadinstitute.hellbender.cmdline.programgroups.CopyNumberProgramGroup;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.tools.exome.*;
import org.broadinstitute.hellbender.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Correct coverage for sample-specific covariate bias effects as described in {@link TargetCovariateCorrector}.
 *
 * Inputs are
 * 1. read counts file (format described in {@link ReadCountCollectionUtils}).  Since this tool corrects
 *    for multiplicative covariate bias effects, these counts should not be in log space i.e. they should represent
 *    raw coverage or proportional coverage upstream of tangent normalization.
 * 2. targets file containing GC content annotation as produced by {@link AnnotateTargets}.  Every target in the input
 *    read counts must be present in the targets file but the reverse need not be true.
 * 3. TODO
 *
 * Output is a read counts file with the same targets (rows) and samples (columns) as the input, corrected for GC bias.
 * Coverage is represented by doubles in {@link ReadCountCollection}.
 *
 * @author David Benjamin &lt;davidben@broadinstitute.org&gt;
 */
@CommandLineProgramProperties(
        oneLineSummary = "Correct for per-sample GC bias effects",
        summary = "Correct coverage in a read counts files by estimating per-sample bias as a function of target GC" +
                " content and dividing input coverage by these derived bias curves.",
        programGroup = CopyNumberProgramGroup.class
)
public class CorrectCovariateBias extends CommandLineProgram {
    public static final String INPUT_READ_COUNTS_FILE_LONG_NAME = StandardArgumentDefinitions.INPUT_LONG_NAME;
    public static final String INPUT_READ_COUNTS_FILE_SHORT_NAME = StandardArgumentDefinitions.INPUT_SHORT_NAME;

    public static final String OUTPUT_READ_COUNTS_FILE_LONG_NAME = StandardArgumentDefinitions.OUTPUT_LONG_NAME;
    public static final String OUTPUT_READ_COUNTS_FILE_SHORT_NAME = StandardArgumentDefinitions.OUTPUT_SHORT_NAME;

    public static final String PERFORM_GC_CORRECTION_LONG_NAME = "correctGC";
    public static final String PERFORM_GC_CORRECTION_SHORT_NAME = PERFORM_GC_CORRECTION_LONG_NAME;

    public static final String PERFORM_MAPPABILITY_CORRECTION_LONG_NAME = "correctMappability";
    public static final String PERFORM_MAPPABILITY_CORRECTION_SHORT_NAME = "correctMap";

    @Argument(
            doc = "Read counts input file.",
            shortName = INPUT_READ_COUNTS_FILE_SHORT_NAME,
            fullName = INPUT_READ_COUNTS_FILE_LONG_NAME,
            optional = false
    )
    protected File inputReadCountsFile;

    @Argument(
            doc = "Perform the GC correction.",
            shortName = PERFORM_GC_CORRECTION_SHORT_NAME,
            fullName = PERFORM_GC_CORRECTION_LONG_NAME,
            optional = true
    )
    protected boolean correctGC = false;

    @Argument(
            doc = "Perform the mappability correction",
            shortName = PERFORM_MAPPABILITY_CORRECTION_SHORT_NAME,
            fullName = PERFORM_MAPPABILITY_CORRECTION_LONG_NAME,
            optional = true
    )
    protected boolean correctMappability = false;

    @Argument(
            doc = "Output file of GC-corrected read counts.",
            shortName = OUTPUT_READ_COUNTS_FILE_SHORT_NAME,
            fullName = OUTPUT_READ_COUNTS_FILE_LONG_NAME,
            optional = false
    )
    protected File outputReadCountsFile;

    // arguments for covariate-annotated targets
    @ArgumentCollection
    protected TargetArgumentCollection targetArguments = new TargetArgumentCollection();

    @Override
    protected Object doWork() {
        validateArguments();
        final TargetCollection<Target> targets = targetArguments.readTargetCollection(false);
        final ReadCountCollection inputCounts = readInputCounts(targets);
        final ReadCountCollection gcCorrectedCounts = (correctGC) ? TargetCovariateCorrector.correctCoverage(inputCounts, covariateContentsOfTargets(inputCounts, targets, TargetAnnotation.GC_CONTENT)) : inputCounts;
        final ReadCountCollection mappabilityCorrectedCounts = (correctMappability) ? TargetCovariateCorrector.correctCoverage(gcCorrectedCounts, covariateContentsOfTargets(inputCounts, targets, TargetAnnotation.MAPPABILITY_CONTENT)) : gcCorrectedCounts;

        writeOutputCounts(mappabilityCorrectedCounts);
        return "SUCCESS";
    }

    private ReadCountCollection readInputCounts(final TargetCollection<Target> targets) {
        final ReadCountCollection inputCounts;
        try {
            inputCounts = ReadCountCollectionUtils.parse(inputReadCountsFile, targets, false);
        } catch (final IOException ex) {
            throw new UserException.CouldNotReadInputFile(inputReadCountsFile, ex.getMessage(), ex);
        }
        return inputCounts;
    }

    private void writeOutputCounts(final ReadCountCollection outputCounts) {
        try {
            ReadCountCollectionUtils.write(outputReadCountsFile, outputCounts);
        } catch (final IOException ex) {
            throw new UserException.CouldNotCreateOutputFile(outputReadCountsFile, ex);
        }
    }

    private double[] covariateContentsOfTargets(final ReadCountCollection inputCounts, final TargetCollection<Target> targets, TargetAnnotation covariateName) {
        final List<Target> annotatedTargets = inputCounts.targets().stream().map(t -> targets.target(t.getName())).collect(Collectors.toList());
        if (!annotatedTargets.stream().allMatch(t -> t.getAnnotations().hasAnnotation(covariateName))) {
            throw new UserException.BadInput("At least one target lacks a covariate annotation.");
        }
        return annotatedTargets.stream().mapToDouble(t -> t.getAnnotations().getDouble(covariateName)).toArray();
    }

    private void validateArguments(){
        Utils.validateArg(correctGC || correctMappability, "At least one of " + PERFORM_GC_CORRECTION_LONG_NAME + " or " + PERFORM_GC_CORRECTION_LONG_NAME + " parameters must be true");
    }

}
