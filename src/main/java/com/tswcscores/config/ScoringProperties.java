package com.tswcscores.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scoring")
public class ScoringProperties {
    /** Очки за точный счёт (включает исход) */
    private int exactScore = 4;
    /** Очки за правильный исход (победитель или ничья) */
    private int correctOutcome = 2;
    /** Бонус за правильную разницу голов (при неточном счёте) */
    private int goalDifference = 1;
}
