package chisa.zhida;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ZhidaAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZhidaAiApplication.class, args);
    }
}
