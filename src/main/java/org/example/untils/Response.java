package org.example.untils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor /* <- необхідно для Jackson Jackson під час десеріалізації JSON → Java:
спочатку створює порожній обʼєкт (через конструктор без аргументів),
потім викликає сеттери для кожного поля.
*/
public class Response implements JsonSerializable {
    private String message;
    private int code;
}
