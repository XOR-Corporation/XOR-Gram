package org.xorgram.ai;

import android.content.Context;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.xorgram.core.XORConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Translation Engine using ML Kit
 * 
 * Provides real-time translation capabilities using
 * Google's on-device ML Kit translation models.
 */
public class TranslationEngine {

    private final Context context;
    private final XORConfig config;
    private final ExecutorService executor;
    private Translator translator;
    private String currentSourceLang;
    private String currentTargetLang;

    public TranslationEngine(Context context, XORConfig config) {
        this.context = context;
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor();
        
        // Initialize with default target language
        String targetLang = config.getString("xor-ai", "target_language", "en");
        initializeTranslator("auto", targetLang);
    }

    /**
     * Initialize the translator with source and target languages
     */
    private void initializeTranslator(String sourceLang, String targetLang) {
        // Map language codes
        String sourceLanguage = mapLanguageCode(sourceLang);
        String targetLanguage = mapLanguageCode(targetLang);
        
        TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build();
        
        translator = Translation.getClient(options);
        currentSourceLang = sourceLang;
        currentTargetLang = targetLang;
        
        // Download model if needed
        DownloadConditions conditions = new DownloadConditions.Builder()
            .requireWifi()
            .build();
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(unused -> {
                // Model downloaded successfully
            })
            .addOnFailureListener(e -> {
                // Handle download failure
            });
    }

    /**
     * Translate text to target language
     * 
     * @param text Text to translate
     * @param targetLang Target language code (e.g., "en", "es", "fr")
     * @return Translated text
     */
    public String translate(String text, String targetLang) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Reinitialize translator if target language changed
        if (!targetLang.equals(currentTargetLang)) {
            initializeTranslator(currentSourceLang, targetLang);
        }

        // For synchronous operation, we'll use a blocking approach
        final String[] result = {text};
        final boolean[] completed = {false};
        
        translator.translate(text)
            .addOnSuccessListener(translated -> {
                result[0] = translated;
                completed[0] = true;
            })
            .addOnFailureListener(e -> {
                completed[0] = true;
            });

        // Wait for translation (with timeout)
        long startTime = System.currentTimeMillis();
        while (!completed[0] && System.currentTimeMillis() - startTime < 5000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

        return result[0];
    }

    /**
     * Asynchronous translation
     */
    public void translateAsync(String text, String targetLang, TranslationCallback callback) {
        executor.execute(() -> {
            String translated = translate(text, targetLang);
            callback.onTranslationReady(translated);
        });
    }

    /**
     * Map language codes to ML Kit format
     */
    private String mapLanguageCode(String code) {
        // Handle "auto" for automatic detection
        if ("auto".equals(code)) {
            return TranslateLanguage.ENGLISH; // Default fallback
        }
        
        // Map common language codes
        switch (code.toLowerCase()) {
            case "en":
                return TranslateLanguage.ENGLISH;
            case "es":
                return TranslateLanguage.SPANISH;
            case "fr":
                return TranslateLanguage.FRENCH;
            case "de":
                return TranslateLanguage.GERMAN;
            case "it":
                return TranslateLanguage.ITALIAN;
            case "pt":
                return TranslateLanguage.PORTUGUESE;
            case "ru":
                return TranslateLanguage.RUSSIAN;
            case "ja":
                return TranslateLanguage.JAPANESE;
            case "ko":
                return TranslateLanguage.KOREAN;
            case "zh":
                return TranslateLanguage.CHINESE;
            case "ar":
                return TranslateLanguage.ARABIC;
            case "hi":
                return TranslateLanguage.HINDI;
            default:
                return TranslateLanguage.ENGLISH;
        }
    }

    /**
     * Shutdown the engine
     */
    public void shutdown() {
        if (translator != null) {
            translator.close();
        }
        executor.shutdown();
    }

    /**
     * Callback interface for async translation
     */
    public interface TranslationCallback {
        void onTranslationReady(String translatedText);
    }
}
