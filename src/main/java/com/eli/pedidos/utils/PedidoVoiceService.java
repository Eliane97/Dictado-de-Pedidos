package com.eli.pedidos.utils;

import com.eli.pedidos.Pedido;
import com.eli.pedidos.Producto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoVoiceService {
    public List<String> generarItemsAudibles(List<Pedido> pedidos) {
        List<String> itemsAudibles = new ArrayList<>();

        if (pedidos == null || pedidos.isEmpty()) {
            itemsAudibles.add("No se encontraron pedidos.");
            return itemsAudibles;
        }

        // Podrías añadir un mensaje de bienvenida si quieres, por ejemplo:
        // itemsAudibles.add("Pedido de " + pedidos.get(0).getCliente()); // Si siempre es un cliente único

        for (Pedido pedido : pedidos) {
            // Opcional: añadir información del cliente si el PDF es de múltiples clientes
            if (pedidos.size() > 1 && pedido.getCliente() != null && !pedido.getCliente().isEmpty()) {
                itemsAudibles.add("Para el cliente " + pedido.getCliente() + ":");
            }

            if (pedido.getProductos() == null || pedido.getProductos().isEmpty()) {
                itemsAudibles.add("No se encontraron productos para el pedido del cliente " + pedido.getCliente() + ".");
                continue; // Pasa al siguiente pedido si no hay productos
            }

            for (Producto producto : pedido.getProductos()) {
                String itemAudible = producto.getCantidad() + " de " + producto.getDescripcion();
                itemsAudibles.add(itemAudible);
            }
        }
        return itemsAudibles;
    }

    /**
     * Genera una única cadena de texto combinando todos los items audibles,
     * para una lectura continua.
     *
     * @param pedidos La lista de objetos Pedido parseados.
     * @return Una única String con todos los ítems audibles concatenados.
     */
    public String generarLecturaCompleta(List<Pedido> pedidos) {
        List<String> items = generarItemsAudibles(pedidos);
        return String.join(". ", items) + "."; // Une los ítems con un punto y espacio, y añade un punto final
    }

}
