package de.evoid.myhome;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhoneticResolver {
    private static final Soundex SOUNDEX = new Soundex();
    public static final PhoneticResolver instance = new PhoneticResolver();
    public final Set<Option> options = new HashSet<Option>();

    private PhoneticResolver() {
    }

    public static class Option {
        public final String encodedTerm;
        public final Runnable action;

        public Option(String term, Runnable action) {
            encodedTerm = SOUNDEX.encode(term);
            this.action = action;
        }
    }

    public void addOption(String term, Runnable action) {
        options.add(new Option(term, action));
    }

    public Option resolve(String term) {
        return resolve(term, 0.5);
    }

    public Option resolve(String term, double minimumSimilarity) {
        if (options.isEmpty()) return null;
        String encodedTerm = SOUNDEX.encode(term);
        Map<Double, Option> scores = new HashMap<Double, Option>();
        double maxScore = Double.NEGATIVE_INFINITY;
        for (Option option : options) {
            double similarity = StringUtils.getJaroWinklerDistance(encodedTerm, option.encodedTerm);
            scores.put(similarity, option);
            if (similarity > maxScore) maxScore = similarity;
        }
        if (maxScore >= minimumSimilarity) {
            return scores.get(maxScore);
        } else {
            Logger.getLogger(PhoneticResolver.class.getName()).log(Level.SEVERE, "Failed to resolve \"" + term + "\" (max similarity: " + maxScore + ")");
            return null;
        }
    }
}
