package org.example.untils;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor /* <- необхідно для Jackson Jackson під час десеріалізації JSON → Java:
спочатку створює порожній обʼєкт (через конструктор без аргументів),
потім викликає сеттери для кожного поля.
*/
public class DocRequest implements JsonSerializable {
    private String clientId;
    private String doc;
    private String body;
}

