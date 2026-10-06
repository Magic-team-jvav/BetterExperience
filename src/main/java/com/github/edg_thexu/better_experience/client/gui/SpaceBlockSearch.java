package com.github.edg_thexu.better_experience.client.gui;



import java.util.List;
import java.util.Locale;

/** Compile once per text edit, then match the cached names and registry IDs. */
public record SpaceBlockSearch(List<String> terms) {
    public SpaceBlockSearch {
        terms = List.copyOf(terms);
    }

    public static SpaceBlockSearch parse(String query) {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        return new SpaceBlockSearch(normalized.isEmpty() ? List.of() : List.of(normalized.split("\\s+")));
    }

    public boolean matches(String name, String id) {
        return terms.stream().allMatch(term -> term.startsWith("@")
                ? id.substring(0, id.indexOf(':')).contains(term.substring(1))
                : name.contains(term) || id.contains(term));
    }
}
