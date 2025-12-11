package com.example.sqsmicro.listener;

import com.example.sqslib.producer.SqsProducerService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.DecryptMessageUtil;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SqsListenerConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsListenerConsumer.class);
    private String colaAwsSqsProducer;
    private final DecryptMessageUtil decryptMessageUtil;
    private final SqsProducerService sqsProducerService;


    public SqsListenerConsumer(@Value("${cola.aws.sqs.producer}") String colaAwsSqsProducer,
                               DecryptMessageUtil decryptMessageUtil,
                               SqsProducerService sqsProducerService) {
        this.colaAwsSqsProducer = colaAwsSqsProducer;
        this.decryptMessageUtil = decryptMessageUtil;
        this.sqsProducerService = sqsProducerService;
    }

    @SqsListener("${cola.aws.sqs.consumer}")
    public void processMessage(MessageDto messageDto) {
        try {
            // 1. Desencriptar
            String originalPayload = decryptMessageUtil.decryptPayload(messageDto.encryptedPayload());
            log.info("Mensaje descifrado: " + originalPayload);
            // 2. Procesar (Tu lógica)
            String response = originalPayload + " EXITOSO";

            // 3. Responder a cola-aws-sqs-2 usando Lib (A)
            sqsProducerService.send(colaAwsSqsProducer, response);

        } catch (Exception e) {
            e.printStackTrace();
            // Manejar error
        }
    }
}
