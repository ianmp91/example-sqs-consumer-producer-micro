package com.example.sqsmicro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.example.sqsmicro", // Tu paquete actual
		"com.example.sqslib"          // 🚨 El paquete donde está SqsLibraryProducer (debes usar el nombre real)
})
public class SqsConsumerProducerMicroApplication {

	public static void main(String[] args) {
		SpringApplication.run(SqsConsumerProducerMicroApplication.class, args);
	}

}
