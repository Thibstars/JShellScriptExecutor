package be.thibaulthelsmoortel.executor.util;

import java.net.URL;

/**
 * @author Thibault Helsmoortel
 */
public final class ResourceUtil {

    private ResourceUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static URL getResource(String name) {
        return ResourceUtil.class.getClassLoader().getResource(name);
    }
}
