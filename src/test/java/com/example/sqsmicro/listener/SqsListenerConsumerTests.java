package com.example.sqsmicro.listener;

import com.example.sqslib.producer.SqsProducerService;
import com.example.sqsmicro.records.MessageDto;
import com.example.sqsmicro.util.DecryptMessageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SqsListenerConsumerTests {

    @Mock
    private DecryptMessageUtil decryptMessageUtil;

    @Mock
    private SqsProducerService sqsProducerService;

    @InjectMocks
    private SqsListenerConsumer sqsListenerConsumer;

    @BeforeEach
    void setUp() {
        // Creamos los mocks manualmente
        decryptMessageUtil = Mockito.mock(DecryptMessageUtil.class);
        sqsProducerService = Mockito.mock(SqsProducerService.class);
        sqsListenerConsumer = new SqsListenerConsumer(
                "cola-aws-sqs-2",
                decryptMessageUtil,
                sqsProducerService
        );
    }

    @Test
    void shouldDecryptProcessAndSendResponse() throws Exception {
        // 1. Arrange (Preparar datos)
        String encryptedPayload = "BASE64_ENCRYPTED_STRING";
        String decryptedPayload = "LAX-123";

        MessageDto messageDto = new MessageDto(
                Map.of("keyId", "abc-123"),
                encryptedPayload,
                "keyId"
        );

        // Simulamos que el servicio de desencriptación funciona
        when(decryptMessageUtil.decryptPayload(encryptedPayload)).thenReturn(decryptedPayload);

        // 2. Act (Ejecutar)
        sqsListenerConsumer.processMessage(messageDto);

        // 3. Assert (Verificar)
        // Verificar que se llamó al descifrador
        verify(decryptMessageUtil).decryptPayload(encryptedPayload);

        // Verificar que, tras procesar, se envió un mensaje a la cola 2
        // Asumiendo que la respuesta es el payload procesado + "PROCESSED"
        verify(sqsProducerService).send(eq("cola-aws-sqs-2"), eq("LAX-123 EXITOSO"));
    }
}
