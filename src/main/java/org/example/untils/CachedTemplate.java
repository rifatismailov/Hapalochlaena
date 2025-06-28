package org.example.untils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor /* <- необхідно для Jackson Jackson під час десеріалізації JSON → Java:
спочатку створює порожній обʼєкт (через конструктор без аргументів),
потім викликає сеттери для кожного поля.
*/
public class CachedTemplate  implements JsonSerializable {
    private Map<String, List<String>> fragments;
    private Map<String, List<float[]>> embeddings;

}

