package com.github.edg_thexu.better_experience.module.lootpity;

/** Converts accumulated scores into valid probabilities without changing group exclusivity. */
public final class PityRolls {
    private PityRolls() {}

    public static double[] probabilities(double[] scores, boolean exclusive) {
        double[] result = scores.clone();
        double sum = 0;
        double largest = 0;
        boolean overdue = false;
        for (int i = 0; i < result.length; i++) {
            double score = result[i];
            if (!Double.isFinite(score) || score < 0) throw new IllegalArgumentException("Invalid pity score");
            sum += score;
            if (score >= 1 - 1.0e-7 && score > largest) {
                largest = score;
                overdue = true;
            }
        }
        if (exclusive && overdue) {
            int tied = 0;
            for (double score : scores) if (score == largest) tied++;
            for (int i = 0; i < result.length; i++) result[i] = scores[i] == largest ? 1.0 / tied : 0;
        } else {
            for (int i = 0; i < result.length; i++) {
                if (exclusive && sum > 1) result[i] /= sum;
                else if (result[i] >= 1 - 1.0e-7) result[i] = 1;
            }
        }
        return result;
    }
}
