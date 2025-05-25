package com.eli.pedidos.utils;

import com.eli.pedidos.PDFUtils;
import com.eli.pedidos.Pedido;
import com.eli.pedidos.Producto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PDFParser {

    public static List<Pedido> parsePedidos(File archivoPdf) throws Exception {
        String textoExtraido = PDFUtils.extraerTexto(archivoPdf);
        return procesarTextoPdf(textoExtraido);
    }

    public static List<Pedido> procesarTextoPdf(String texto) {
        List<Pedido> pedidos = new ArrayList<>();
        String[] lineas = texto.split("\\r?\\n");
        String clienteActual = null;
        List<Producto> productosActuales = new ArrayList<>();

        for (String linea : lineas) {
            linea = linea.trim();

            if (linea.matches("^[A-ZÁÉÍÓÚÑa-záéíóúñ]+.*PEDIDO:\\d+.*")) {
                if (clienteActual != null && !productosActuales.isEmpty()) {
                    pedidos.add(new Pedido(clienteActual, productosActuales));
                    productosActuales = new ArrayList<>();
                }

                clienteActual = linea.split("PEDIDO")[0].trim();
                continue;
            }

            if (linea.matches("^\\d+\\s+\\d+\\s+.+\\s+\\d{1,3}(\\.\\d{3})*,\\d{2}\\s+\\d{1,3}(\\.\\d{3})*,\\d{2}$")) {
                String[] partes = linea.split("\\s+", 4);
                if (partes.length >= 4) {
                    try {
                        int cantidad = Integer.parseInt(partes[1]);
                        String descripcion = partes[2] + " " + partes[3].replaceAll("\\s+\\d{1,3}(\\.\\d{3})*,\\d{2}\\s+\\d{1,3}(\\.\\d{3})*,\\d{2}$", "").trim();
                        productosActuales.add(new Producto(cantidad, descripcion));
                    } catch (NumberFormatException e) {
                        System.out.println("Error al parsear línea de producto: " + linea);
                    }
                }
            }
        }

        if (clienteActual != null && !productosActuales.isEmpty()) {
            pedidos.add(new Pedido(clienteActual, productosActuales));
        }

        return pedidos;
    }
}
