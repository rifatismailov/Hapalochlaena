package org.example.service.match;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Представляє результат співпадіння між рядком документа і фрагментом шаблону.
 */
@AllArgsConstructor
@Getter
public class MatchResult {
    /**
     * Рядок з документа, який був проаналізований
     */
    private final String documentLine;
    /**
     * Ключ шаблону (наприклад, "doc_name.doc")
     */
    private final String templateKey;
    /**
     * Фрагмент шаблону, який найбільше співпав з рядком
     */
    private final String templateFragment;
    /**
     * Відсоток схожості (cosine similarity) між рядком і фрагментом
     */
    private final double similarityScore;
    /**
     * Індикатори — ключові слова, що збігаються в документі і шаблоні
     */
    private final List<String> indicators;


}
