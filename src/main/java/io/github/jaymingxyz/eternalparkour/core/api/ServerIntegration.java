package io.github.jaymingxyz.eternalparkour.core.api;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;

/**
 * Optional integration extension point.
 *
 * <p>Providers are discovered through {@link java.util.ServiceLoader}.</p>
 */
public interface ServerIntegration {

    String id();

    void enable(EternalParkour plugin) throws Exception;

    void disable() throws Exception;
}
