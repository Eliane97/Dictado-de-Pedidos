package com.eli.pedidos;

import com.eli.pedidos.utils.PDFParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map; // Solo si decides usar Map para el error, si no, puedes quitarlo

@RestController
@CrossOrigin
public class PedidoController {

    @PostMapping("/api/procesar-pdf")
    public ResponseEntity<List<Pedido>> procesarPDF(@RequestParam("file") MultipartFile file) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("pedido", ".pdf");
            file.transferTo(tempFile);

            // PDFParser.parsePedidos ya te devuelve una List<Pedido> sin los resúmenes.
            List<Pedido> pedidos = PDFParser.parsePedidos(tempFile);

            // Spring Boot serializará directamente esta lista de objetos Pedido a JSON.
            return ResponseEntity.ok(pedidos);

        } catch (Exception e) {
            e.printStackTrace();
            // Si hay un error, puedes devolver un status 500 y un cuerpo nulo,
            // o un JSON de error si defines una clase de error.
            return ResponseEntity.status(500).body(null);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
