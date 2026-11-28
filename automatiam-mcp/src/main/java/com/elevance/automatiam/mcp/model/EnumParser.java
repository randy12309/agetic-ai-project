package com.elevance.automatiam.mcp.model;

import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Turns a tool argument into an enum constant, or rejects it.
 *
 * <p>The rejection names the permitted values. That is safe to disclose - the v […]
 * of the published tool schema - and it is what lets a workflow correct itself […]
 * the same bad call.
 */
public final class EnumParser {

    private EnumParser() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String argument, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidToolArgumentException(argument, "must not be blank");
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equalsIgnoreCase(value.strip()))
                .findFirst()
                .orElseThrow(() -> new InvalidToolArgumentException(argument, "must be one of " + permitted(type)));
    }

    /** Parses an argument the caller may omit. Blank and null are absence, not […] */
    public static <E extends Enum<E>> Optional<E> parseOptional(
            Class<E> type, String argument, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(parse(type, argument, value));
    }

    /**
     * Maps a value received from an upstream OneIAM response.
     *
     * <p>Unlike {@link #parse}, an unrecognised value yields {@code fallback) i […]
     * The asymmetry is intended: a caller who mistypes a filter should be told, […]
     * OneIAM started emitting last week should not fail a read that is otherwis […]
     *
     * <p>Adapters mapping upstream responses use this. Tool arguments use {@lin […]
     */
    public static <E extends Enum<E>> E parseUpstream(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.name().equalsIgnoreCase(value.strip()))
                .findFirst()
                .orElse(fallback);
    }

    private static <E extends Enum<E>> String permitted(Class<E> type) {
        return Arrays.stream(type.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
