package org.example.service.match;

import lombok.AllArgsConstructor;
import lombok.Getter;
@AllArgsConstructor
@Getter
public class MatchMeta {
    private final String templateName;
    private final double score;
    private final int lineCount;

}

