package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.untils.JsonSerializable;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor /* <- необхідно для Jackson Jackson під час десеріалізації JSON → Java:
спочатку створює порожній обʼєкт (через конструктор без аргументів),
потім викликає сеттери для кожного поля.
*/
public class ErrorResponse implements JsonSerializable {
    private String message;
    private int errorCode;

}
