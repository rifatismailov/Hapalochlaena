package org.example.untils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Утилітарний клас для обчислення схожості текстів та обробки рядків.
 */
public class TextSimilarityUtils {

    /**
     * Обчислює cosine similarity між двома векторами.
     * Це метрика, яка показує наскільки два вектори схожі по напрямку.
     *
     * @param a перший вектор (наприклад, embedding речення з шаблону)
     * @param b другий вектор (наприклад, embedding речення з документа)
     * @return значення схожості від -1 до 1 (де 1 — повна схожість, 0 — ортогональність)
     */
    public static double cosineSimilarity( float[] a, float[] b) {
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }

        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Витягує індикатори — спільні слова, які є одночасно у рядку з документа і у фрагменті шаблону.
     *
     * @param docLine рядок з документа
     * @param templateFragment фрагмент шаблону
     * @return список спільних слів
     */
    public static  List<String> extractCommonIndicators(String docLine, String templateFragment) {
        Set<String> docWords = tokenize(docLine);
        Set<String> templateWords = tokenize(templateFragment);
        docWords.retainAll(templateWords);
        return new ArrayList<>(docWords);
    }

    /**
     * Розбиває текст на нормалізовані слова.
     *
     * @param text вхідний рядок
     * @return множина унікальних, нормалізованих слів
     */
    public static Set<String> tokenize( String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[“”«»\"'.,;:!?()\\[\\]]", "")
                        .split("\\s+"))
                .map(TextSimilarityUtils::normalizeWord)
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }

    /**
     * Проста нормалізація (стемінг) українських слів — видалення поширених закінчень.
     *
     * @param word слово
     * @return основа слова
     */
    public static String normalizeWord(String word) {
        return word.replaceAll("(ами|ів|ої|ий|им|их|а|у|і|е|о|я|ю|ь|ти|тися)$", "");
    }
}
