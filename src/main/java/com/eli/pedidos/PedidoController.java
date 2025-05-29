package com.eli.pedidos;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.eli.pedidos.utils.PDFParser;



@RestController
@CrossOrigin
public class PedidoController {
    @PostMapping("/api/procesar-pdf")
    public ResponseEntity<Map<String, Object>> procesarPDF(@RequestParam("file") MultipartFile file) {
        try {
            File tempFile = File.createTempFile("pedido", ".pdf");
            file.transferTo(tempFile);

            List<Pedido> pedidos = PDFParser.parsePedidos(tempFile);
            List<String> mensajes = new ArrayList<>();


            for (Pedido pedido : pedidos) {
                mensajes.add("Cliente: " + pedido.getCliente());
                for (Producto producto : pedido.getProductos()) {
                    mensajes.add(producto.getCantidad() + " " + producto.getDescripcion());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("mensajes", mensajes);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo procesar el archivo"));
        }
    }

}
