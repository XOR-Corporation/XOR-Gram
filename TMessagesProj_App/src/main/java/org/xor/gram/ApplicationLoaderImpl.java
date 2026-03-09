package org.xor.gram;

import org.telegram.messenger.ApplicationLoader;
import org.xor.gram.BuildConfig;

/**
 * Application loader implementation for XORGram.
 * This class is referenced in AndroidManifest.xml as the main application class.
 */
public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
