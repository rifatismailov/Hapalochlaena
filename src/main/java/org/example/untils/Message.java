package org.example.untils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor /* <- необхідно для Jackson Jackson під час десеріалізації JSON → Java:
спочатку створює порожній обʼєкт (через конструктор без аргументів),
потім викликає сеттери для кожного поля.
*/
public class Message implements JsonSerializable {
    private String user;
    private String destination;
    private String payload;
}
