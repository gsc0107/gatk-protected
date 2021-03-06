package org.broadinstitute.hellbender.tools.exome;

import htsjdk.samtools.util.Locatable;
import htsjdk.tribble.Feature;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.Utils;

import java.io.Serializable;

/**
 * Exome analysis target.
 *
 * <p>A name entity with genomic interval optionally attached to it</p>
 *
 * @author Valentin Ruano-Rubio &lt;valentin@broadinstitute.org&gt;
 */
public class Target implements Locatable, Feature, Serializable {

    static final long serialVersionUID = 11337337337L;

    // always specified, never null
    private final String name;

    // interval can be left unspecified (null)
    private final SimpleInterval interval;

    private final TargetAnnotationCollection annotations;

    /**
     * Creates a new target given its name.
     * <p>
     * The interval is left undefined.
     * </p>
     *
     * @param name the new target name.
     * @throws IllegalArgumentException if {@code name} is {@code null}.
     */
    public Target(final String name) {
        this(name, null);
    }

    public Target(final String name, final SimpleInterval interval) {
        this(name, interval, null);
    }

    public Target(final SimpleInterval interval) {
        this(createDummyTargetName(interval), interval);
    }

    /**
     * Creates a string for a locatable that can be used when creating dummy target names
     * @param locatable The genome region to create a unique dummy target name. Never {@code null}
     * @return never {@code null}
     */
    private static String createDummyTargetName(final Locatable locatable){
        Utils.nonNull(locatable);
        return "target_" + locatable.getContig() + "_" + String.valueOf(locatable.getStart()) + "_" + String.valueOf(locatable.getEnd());
    }

    public TargetAnnotationCollection getAnnotations() {
        return annotations;
    }

    /**
     * Construct a new target given all its properties except coverage.
     * <p>
     * The name cannot be a {@code null} but the interval can be, thus leaving it unspecified.
     * </p>
     *
     * @param name the name of the interval.
     * @param interval the interval.
     * @param annotations annotations on the target.
     *
     * @throws IllegalArgumentException if {@code name} is {@code null}.
     */
    public Target(final String name, final SimpleInterval interval, final TargetAnnotationCollection annotations) {
        this.name = Utils.nonNull(name, "the name cannot be null");
        this.interval = interval;
        this.annotations = annotations;
    }

    /**
     * Returns the interval.
     * @return may be {@code null}.
     */
    public SimpleInterval getInterval() {
        return interval;
    }

    /**
     * Returns the name.
     * @return never{@code null}.
     */
    public String getName() {
        return name;
    }

     // Three methods to implement Locatable
    @Override
    public String getContig() {
        if (interval == null) {
            throw new IllegalStateException("the target does not have an interval assigned");
        }
        return interval.getContig();
    }

    @Override
    public int getStart() {
        if (interval == null) {
            throw new IllegalStateException("the target does not have an interval assigned");
        }
        return interval.getStart();
    }

    @Override
    public int getEnd() {
        if (interval == null) {
            throw new IllegalStateException("the target does not have an interval assigned");
        }
        return interval.getEnd();
    }

    public int length() { return getEnd() - getStart() + 1; }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Target ? equals((Target)other) : false;
    }

    /**
     * Compare this with another target.
     *
     * <p>
     *     Only the name is compared; the interval is not considered par of the identification of the target.
     * </p>
     * @param other the other target to compare to.
     * @return {@code true} iff they have the same interval and name.
     */
    public boolean equals(final Target other) {
        if (other == null) {
            return false;
        } else {
            return name.equals(other.name);
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    @Deprecated
    public String getChr() {
        return interval.getContig();
    }
}
