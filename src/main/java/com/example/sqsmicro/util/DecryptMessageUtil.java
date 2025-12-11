package com.example.sqsmicro.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivateKey;
import java.util.Base64;

@Component
public class DecryptMessageUtil {

    private PrivateKey privateKey;

    public DecryptMessageUtil(@Value("classpath:private_key.pem") Resource privateKeyResource) throws Exception {
        if (!privateKeyResource.exists()) {
            throw new RuntimeException("ERROR FATAL: No se encuentra private_key.pem en src/main/resources");
        }
        loadPrivateKey(privateKeyResource);
    }

    private void loadPrivateKey(Resource resource) throws Exception {
        try (Reader reader = new InputStreamReader(resource.getInputStream());
             PEMParser pemParser = new PEMParser(reader)) {

            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof PEMKeyPair) {
                this.privateKey = converter.getKeyPair((PEMKeyPair) object).getPrivate();
            } else if (object instanceof PrivateKeyInfo) {
                this.privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
            } else {
                throw new RuntimeException("El archivo private_key.pem no tiene un formato soportado (esperado PKCS#1 o PKCS#8)");
            }
        }
    }

    public String decryptPayload(String encryptedBase64) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }
}
