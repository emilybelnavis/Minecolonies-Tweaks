package dev.minecoloniestweaks.names;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** A name pool has no game dependencies so validation can run in ordinary unit tests. */
public record NamePool(List<String> male, List<String> female, List<String> surnames, boolean surnameFirst) {
    public NamePool {
        male = validate(male, "male");
        female = validate(female, "female");
        surnames = validate(surnames, "surnames");
    }

    private static List<String> validate(List<String> values, String field) {
        if (values == null || values.isEmpty() || values.size() > 10000) {
            throw new IllegalArgumentException(field + " must have between 1 and 10000 entries");
        }
        return values.stream().map(value -> {
            if (value == null || value.isBlank() || value.length() > 64) {
                throw new IllegalArgumentException(field + " contains an empty or excessively long name");
            }
            if (value.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException(field + " contains control characters");
            }
            return value.strip();
        }).distinct().toList();
    }

    public String generate(Random random, boolean isFemale) {
        var firstNames = isFemale ? female : male;
        String first = firstNames.get(random.nextInt(firstNames.size()));
        String surname = surnames.get(random.nextInt(surnames.size()));
        return format(first, surname);
    }

    public String generateUnique(Random random, boolean isFemale, Set<String> occupied, String inheritedSurname) {
        String candidate = "";
        for (int attempt = 0; attempt < 64; attempt++) {
            var firstNames = isFemale ? female : male;
            String first = firstNames.get(random.nextInt(firstNames.size()));
            String surname = inheritedSurname == null ? surnames.get(random.nextInt(surnames.size())) : inheritedSurname;
            candidate = format(first, surname);
            if (!occupied.contains(candidate)) return candidate;
        }
        int suffix = 2;
        while (occupied.contains(candidate + " " + suffix)) suffix++;
        return candidate + " " + suffix;
    }

    public String surnameFrom(String parentName) {
        if (parentName == null || parentName.isBlank()) return null;
        parentName = parentName.replaceFirst(" \\d+$", "");
        for (String surname : surnames.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList()) {
            if (surnameFirst ? parentName.startsWith(surname + " ") : parentName.endsWith(" " + surname)) return surname;
        }
        int split = surnameFirst ? parentName.indexOf(' ') : parentName.lastIndexOf(' ');
        if (split < 0) return null;
        return surnameFirst ? parentName.substring(0, split) : parentName.substring(split + 1);
    }

    private String format(String first, String surname) {
        return surnameFirst ? surname + " " + first : first + " " + surname;
    }
}
