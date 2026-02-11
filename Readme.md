# Servidor HTTP Multihilo en Java

Servidor web **HTTP/1.0** multihilo implementado en **Java puro**, **destinado pa hackear a Domiciano y al monitor 😈**.  
Este proyecto tiene como objetivo comprender el funcionamiento interno de un servidor web utilizando **sockets TCP** y manejo manual del protocolo HTTP.

---

## Características

- Escucha conexiones TCP en un **puerto configurable mayor a 1024**
- Arquitectura **multihilo** mediante *thread pool*
- Soporte para **HTTP/1.0** utilizando el método **GET**
- Lectura y visualización por consola de la **línea de solicitud** y los **encabezados HTTP**
- Respuestas HTTP correctamente formateadas (línea de estado, headers y cuerpo) usando **CRLF**
- Servido de archivos estáticos:
    - HTML
    - Imágenes (JPG, GIF, PNG)
    - CSS
    - JavaScript
- Manejo de **errores 404** mediante una página personalizada
- Protección contra **path traversal**
- Lectura de archivos mediante **streaming** para un uso eficiente de memoria
- Cierre seguro de **sockets y streams**
- Ejecución continua del servidor hasta interrupción manual

---

## Estructura del proyecto

```text
src/
 └── www/
     ├── index.html
     ├── style.css
     ├── script.js
     ├── matrix_code.gif
     ├── cyber_wall.jpg
     ├── prueba.html
     └── error404.html
Main.java

Links
http://localhost:8080/
http://localhost:8080/test.html
http://localhost:8080/error404.html
```


