package org.example.untils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Утилітарний клас для обчислення схожості текстів та обробки рядків.
 * <p>
 * Містить методи для:
 * - Обчислення cosine similarity між векторами
 * - Витягування спільних слів (індикаторів)
 * - Токенізації та нормалізації українських слів
 */
public class TextSimilarityUtils {

    /**
     * Обчислює cosine similarity між двома векторами чисел (embedding).
     * <p>
     * Косинусна схожість вимірює кут між векторами:
     * - 1.0 — однакові напрямки (повна схожість)
     * - 0.0 — ортогональні (немає схожості)
     * - -1.0 — протилежні напрямки
     *
     * @param a перший вектор (наприклад, embedding шаблонного речення)
     * @param b другий вектор (наприклад, embedding речення з документа)
     * @return значення схожості від -1 до 1
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];   // Скалярний добуток
            na += a[i] * a[i];    // Квадрат довжини вектора a
            nb += b[i] * b[i];    // Квадрат довжини вектора b
        }

        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Витягує індикатори — спільні слова, які одночасно присутні
     * як у документному рядку, так і у фрагменті шаблону.
     *
     * @param docLine           рядок з документа
     * @param templateFragment  фрагмент шаблону
     * @return список спільних слів (індикаторів)
     */
    public static List<String> extractCommonIndicators(String docLine, String templateFragment) {
        Set<String> docWords = tokenize(docLine);
        Set<String> templateWords = tokenize(templateFragment);

        docWords.retainAll(templateWords); // залишити лише спільні слова
        return new ArrayList<>(docWords);
    }

    /**
     * Розбиває текст на набір унікальних, нормалізованих слів.
     * <p>
     * - Перетворює в нижній регістр
     * - Видаляє розділові знаки
     * - Застосовує нормалізацію (стемінг)
     * - Ігнорує слова довжиною ≤ 2 символи
     *
     * @param text вхідний текст
     * @return множина унікальних слів
     */
    public static Set<String> tokenize(String text) {
        return Arrays.stream(
                        text.toLowerCase()
                                .replaceAll("[“”«»\"'.,;:!?()\\[\\]]", "") // видалення пунктуації
                                .split("\\s+") // розбиття на слова
                )
                .map(TextSimilarityUtils::normalizeWord)
                .filter(s -> s.length() > 2) // фільтр коротких слів
                .collect(Collectors.toSet());
    }

    /**
     * Нормалізує слово — застосовує спрощений стемінг для української мови:
     * видаляє поширені закінчення, щоб отримати основу слова.
     * <p>
     * Приклад: "наказами" → "наказ", "втратилася" → "втрат"
     *
     * @param word слово
     * @return нормалізована (стемована) основа
     *
     * @see <a href="http://www.senyk.poltava.ua/projects/ukr_stemming/stemming_about.html">
     *    Інформація взята з ресурсу</a>
     */
    public static String normalizeWord(String word) {
        // Спочатку видаляємо дієслівно-похідні суфікси
        word = word.replaceAll("(ов)*ува(в|вши|вшись|ла|ло|ли|ння|нні|нням|нню|ти|вся|всь|лись|лися|тись|тися)$", "");

        // Потім застосовуємо базову стемінг-нормалізацію
        return word.replaceAll("(ами|ів|ої|ий|им|их|а|у|і|е|о|я|ю|ь|ти|тися)$", "");
    }

}
