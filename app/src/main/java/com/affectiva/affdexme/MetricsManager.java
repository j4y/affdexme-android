package com.affectiva.affdexme;

import com.affectiva.android.affdex.sdk.detector.Face;

/**
 * A class containing:
 * -enumerations representing the Emotion and Expressions featured in the Affectiva SDK.
 * -a Metric interface to allow easy iteration through all Expressions and Emotions
 * -utility methods for converting a Metric into several types of strings
 */
public class MetricsManager {

    private static Metrics[] allMetrics;

    static {
        Emotions[] emotions = Emotions.values();
        Expressions[] expressions = Expressions.values();
        Emojis[] emojis = Emojis.values();
        allMetrics = new Metrics[emotions.length + expressions.length + emojis.length];
        System.arraycopy(emotions, 0, allMetrics, 0, emotions.length);
        System.arraycopy(expressions, 0, allMetrics, emotions.length, expressions.length);
        System.arraycopy(emojis, 0, allMetrics, emotions.length + expressions.length, emojis.length);
    }

    static Metrics[] getAllMetrics() {
        return allMetrics;
    }

    //Used for displays
    static String getUpperCaseName(Metrics metric) {
        if (metric == Expressions.LIP_CORNER_DEPRESSOR) {
            return "FROWN";
        } else {
            return metric.toString().replace("_", " ");
        }
    }

    //Used for MetricSelectionFragment
    //This method is optimized for strings of the form SOME_METRIC_NAME, which all metric names currently are
    static String getCapitalizedName(Metrics metric) {
        if (metric == Expressions.LIP_CORNER_DEPRESSOR) {
            return "Frown";
        }
        String original = metric.toString();
        StringBuilder builder = new StringBuilder();
        boolean canBeLowerCase = false;
        for (int n = 0; n < original.length(); n++) {
            char c = original.charAt(n);
            if (c == '_') {
                builder.append(' ');
                canBeLowerCase = false;
            } else {
                if (canBeLowerCase) {
                    builder.append(Character.toLowerCase(c));
                } else {
                    builder.append(c);
                    canBeLowerCase = true;
                }
            }
        }
        return builder.toString();
    }

    //Used to load resource files
    static String getLowerCaseName(Metrics metric) {
        return metric.toString().toLowerCase();
    }

    //Used to construct method names for reflection
    static String getCamelCase(Metrics metric) {
        String metricString = metric.toString();

        StringBuilder builder = new StringBuilder();
        builder.append(Character.toUpperCase(metricString.charAt(0)));

        if (metricString.length() > 1) {
            for (int n = 1; n < metricString.length(); n++) {
                char c = metricString.charAt(n);
                if (c == '_') {
                    n += 1;
                    if (n < metricString.length()) {
                        builder.append(metricString.charAt(n));
                    }
                } else {
                    builder.append(Character.toLowerCase(metricString.charAt(n)));
                }
            }
        }

        return builder.toString();
    }

    public enum MetricType {Emotion, Expression, Emoji}

    public enum Emotions implements Metrics {
        ANGER,
        DISGUST,
        FEAR,
        JOY,
        SADNESS,
        SURPRISE,
        CONTEMPT,
        ENGAGEMENT,
        VALENCE;

        @Override
        public MetricType getType() {
            return MetricType.Emotion;
        }
    }

    public enum Expressions implements Metrics {
        ATTENTION,
        BROW_FURROW,
        BROW_RAISE,
        CHIN_RAISE,
        EYE_CLOSURE,
        INNER_BROW_RAISE,
        LIP_CORNER_DEPRESSOR,
        LIP_PRESS,
        LIP_PUCKER,
        LIP_SUCK,
        MOUTH_OPEN,
        NOSE_WRINKLE,
        SMILE,
        SMIRK,
        UPPER_LIP_RAISE;

        @Override
        public MetricType getType() {
            return MetricType.Expression;
        }
    }

    public enum Emojis implements Metrics {
        RELAXED,
        SMILEY,
        LAUGHING,
        KISSING_CLOSED_EYES,
        KISSING,
        DISAPPOINTED,
        RAGE,
        SMIRK,
        WINK,
        STUCK_OUT_TONGUE_CLOSED_EYES,
        STUCK_OUT_TONGUE_WINKING_EYE,
        STUCK_OUT_TONGUE,
        FLUSHED,
        SCREAM;

        @Override
        public MetricType getType() {
            return MetricType.Emoji;
        }

        public String getUnicodeForEmoji() {
            switch (this) {
                case RELAXED:
                    return Face.EMOJI.RELAXED.getUnicode();
                case SMILEY:
                    return Face.EMOJI.SMILEY.getUnicode();
                case LAUGHING:
                    return Face.EMOJI.LAUGHING.getUnicode();
                case KISSING_CLOSED_EYES:
                    return Face.EMOJI.KISSING_CLOSED_EYES.getUnicode();
                case KISSING:
                    return Face.EMOJI.KISSING.getUnicode();
                case DISAPPOINTED:
                    return Face.EMOJI.DISAPPOINTED.getUnicode();
                case RAGE:
                    return Face.EMOJI.RAGE.getUnicode();
                case SMIRK:
                    return Face.EMOJI.SMIRK.getUnicode();
                case WINK:
                    return Face.EMOJI.WINK.getUnicode();
                case STUCK_OUT_TONGUE_CLOSED_EYES:
                    return Face.EMOJI.STUCK_OUT_TONGUE_CLOSED_EYES.getUnicode();
                case STUCK_OUT_TONGUE_WINKING_EYE:
                    return Face.EMOJI.STUCK_OUT_TONGUE_WINKING_EYE.getUnicode();
                case STUCK_OUT_TONGUE:
                    return Face.EMOJI.STUCK_OUT_TONGUE.getUnicode();
                case FLUSHED:
                    return Face.EMOJI.FLUSHED.getUnicode();
                case SCREAM:
                    return Face.EMOJI.SCREAM.getUnicode();
                default:
                    return "";
            }
        }
    }

    public interface Metrics {
        MetricType getType();
    }
}
