# Microservicio Consumidor/Productor (C)

Este servicio actúa como el procesador central. Lee mensajes encriptados, los procesa y notifica el resultado.

## 🔄 Flujo de Mensajería

1.  **Consume (Decryption):** Lee mensajes de la cola de entrada.
    * **Origen:** `cola-aws-sqs-1`.
    * **Seguridad:** Utiliza la **Clave Privada (RSA)** para **DESCIFRAR** el payload y acceder a la data cruda.
    * **Acción:** Procesa la lógica de negocio y **elimina** el mensaje de la cola tras el éxito.
2.  **Produce:** Envía el resultado del proceso o la siguiente tarea.
    * **Destino:** `cola-aws-sqs-2` (para que sea leído por el Servicio B).

## 🛠 Requisitos

* **OS:** macOS (Probado en Tahoe 26.1)
* **Java:** JDK 25
* **Librería Interna:** `sqs-consumer-producer-lib` (A)

## 🔑 Configuración de Seguridad

**IMPORTANTE:** Este servicio custodia la **Clave Privada**. No debe compartirse ni subirse al repositorio si es un entorno productivo.

Para desarrollo local, asegura que la clave esté disponible:
```bash
export DECRYPTION_PRIVATE_KEY_PATH=/path/to/private_key.pem
```

## 📦 Instalación para Microservicios
Añade la dependencia a tu archivo `build.gradle` (asumiendo que esta librería está publicada en tu repositorio local o Nexus):

```groovy
repositories {
    mavenLocal() //Agregar al miroservicio
    mavenCentral()
}


dependencies {
    implementation 'com.example.sqslib:sqs-consumer-producer-lib:0.0.1-SNAPSHOT'
}
```
## ⚙️ Configuración (application.yml)

```yaml
server:
  port: 8082
spring:
  application:
    name: sqs-consumer-producer-micro
  main:
    allow-bean-definition-overriding: true
  cloud:
    aws:
      region:
        static: us-east-1 # Una región arbitraria
      # Configuración específica para SQS que apunta a ElasticMQ
      sqs:
        endpoint:
          # Apunta al puerto del contenedor de ElasticMQ
          uri: http://localhost:9324
      credentials:
        # Credenciales dummy que cumplen con el requisito del SDK de AWS
        access-key: dummy
        secret-key: dummy
cola:
  aws:
    sqs:
      consumer: "cola-aws-sqs-1"
      producer: "cola-aws-sqs-2"
```

## 🚀 Ejecución

```bash
make clean
make build
make autoRun
```
