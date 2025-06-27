package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.untils.JsonSerializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor /* <- необхідно для Jackson Jackson під час десеріалізації JSON → Java:
спочатку створює порожній обʼєкт (через конструктор без аргументів),
потім викликає сеттери для кожного поля.
*/
public class Message implements JsonSerializable {
    //String user, String destination, Object payload
    private String user;
    private String destination;
    private String payload;
}
