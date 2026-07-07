package com.tswcscores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TsWcScoresApplication {

  public static void main(String[] args) {
    SpringApplication.run(TsWcScoresApplication.class, args);
  }
}
