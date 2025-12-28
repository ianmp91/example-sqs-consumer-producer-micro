package com.example.sqsmicro.listener;

import com.example.sqslib.iata.FlightLegType;
import com.example.sqslib.iata.IATAAIDXFlightLegNotifRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRQ;
import com.example.sqslib.iata.IATAAIDXFlightLegRS;
import com.example.sqslib.iata.SuccessType;
import com.example.sqslib.producer.SqsProducerService;
import com.example.sqslib.service.XmlService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.DecryptEncryptMessageUtil;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ian.paris
 * @since 2025-12-15
 */
@Slf4j
@Component
public class SqsListenerConsumer {

    private String colaAwsSqsProducer;
    private final DecryptEncryptMessageUtil decryptEncryptMessageUtil;
    private final SqsProducerService sqsProducerService;
    private final XmlService xmlService;

    public SqsListenerConsumer(@Value("${cola.aws.sqs.producer}") String colaAwsSqsTwo,
                               DecryptEncryptMessageUtil decryptEncryptMessageUtil,
                               SqsProducerService sqsProducerService,
                               XmlService xmlService) {
        this.colaAwsSqsProducer = colaAwsSqsTwo;
        this.decryptEncryptMessageUtil = decryptEncryptMessageUtil;
        this.sqsProducerService = sqsProducerService;
        this.xmlService = xmlService;
    }

    @SqsListener("${cola.aws.sqs.consumer}")
    public void processMessage(MessageDto messageDto) {
        try {

            log.info("Reads encrypted messages. Metadata: {} | EncryptedPayload: {}", messageDto.metadata(), messageDto.encryptedPayload());
            // 1. DecryptPayload
            String decryptPayload = decryptEncryptMessageUtil.decryptHybrid(messageDto.encryptedPayload(), messageDto.keyId());
            // Log for tests without lobDebug
            log.debug("Decrypted payload message. Payload: {} ", decryptPayload);

            // 3. Convertir XML a Objeto Java (Generic)
            Object pojo = xmlService.fromXml(decryptPayload);

            // 4. Procesar según el tipo (Switch Expression + Pattern Matching)
            switch (pojo) {
                case IATAAIDXFlightLegRQ rq -> procesarFlightLegRQ(rq, messageDto.metadata());
                case null -> throw new IllegalArgumentException("Payload null");
                default -> log.error("Message Type not supported: " + pojo.getClass());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Manejar error
        }
    }

    private void procesarFlightLegRQ(IATAAIDXFlightLegRQ rq, Map<String, String> metadata) throws Exception {
        // 1. Instanciar la respuesta raíz
        var response = new IATAAIDXFlightLegRS();

        // ---------------------------------------------------------
        // A. SETEO DE ATRIBUTOS (HEADERS XML)
        // ---------------------------------------------------------

        // Obligatorio: Versión del esquema (Coincide con tus XSD 21.3)
        response.setVersion(rq.getVersion());

        // Timestamp actual (El Adapter1 se encargará del formato ISO 8601)
        response.setTimeStamp(LocalDateTime.now());

        // Trazabilidad: Es VITAL devolver el mismo ID que recibiste para que (B) sepa qué responder
        response.setCorrelationID(rq.getCorrelationID());
        response.setTransactionIdentifier(rq.getTransactionIdentifier());

        // Identificador único de este mensaje de respuesta
        response.setTransactionStatusCode("Start"); // O "End" según el flujo
        response.setSequenceNmbr(BigInteger.ONE);
        response.setTarget(rq.getTarget());

        // ---------------------------------------------------------
        // B. SETEO DE ESTADO (SUCCESS vs ERRORS)
        // ---------------------------------------------------------

        // En IATA, la presencia del elemento "Success" vacío indica éxito.
        // Si hubiera error, dejarías Success en null y llenarías setErrors(...)
        SuccessType success = new SuccessType();
        response.setSuccess(success);

        // ---------------------------------------------------------
        // C. SETEO DEL PAYLOAD (DATOS DE NEGOCIO)
        // ---------------------------------------------------------

        // Crear la info del vuelo (FlightLeg)
        // Nota: Como no pasaste la clase FlightLegType, asumo una estructura estándar IATA
        FlightLegType leg = new FlightLegType();

        // Agregarlo a la lista (tu método getFlightLegs inicializa la lista si es null)
        response.getFlightLegs().add(leg);


        // Marshalling y Encriptado (Para B)
        String xmlResponse = xmlService.toXml(response);

        // IMPORTANTE: Encriptar con la Pública de B
        DecryptEncryptMessageUtil.EncryptedMessageBundle encryptedMessageBundle =
                decryptEncryptMessageUtil.encryptHybridWithPublicKey(xmlResponse);

        // Preparar Metadata de respuesta
        Map<String, String> responseMeta = new HashMap<>();
        responseMeta.put("MESSAGE_TYPE", "IATAAIDXFlightLegRS"); // Tipo de respuesta
        responseMeta.put("correlation_id", metadata.get("correlation_id")); // Mantener trazabilidad

        MessageDto responseDto = new MessageDto(
                responseMeta,
                encryptedMessageBundle.encryptedPayload(),
                encryptedMessageBundle.encryptedKey()
        );
        log.debug("Before preparing the SQS shipment. Metadata: {}", responseMeta);
        sqsProducerService.send(colaAwsSqsProducer, responseDto);
    }
}
