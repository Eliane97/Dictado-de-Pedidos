package com.eli.pedidos.utils;

import com.eli.pedidos.PDFUtils;
import com.eli.pedidos.Pedido;
import com.eli.pedidos.Producto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PDFParser {
    public static List<Pedido> parsePedidos(File archivoPdf) throws Exception {
        String texto = PDFUtils.extraerTexto(archivoPdf);

        if (esTipoConCliente(texto)) {
            return parseConCliente(texto);
        } else if (esTipoSinCliente(texto)) {
            return parseSinCliente(texto);
        } else {
            return new ArrayList<>();
        }
    }

    private static boolean esTipoConCliente(String texto) {
        return texto.contains("PEDIDO:");
    }

    private static boolean esTipoSinCliente(String texto) {
        return !texto.contains("PEDIDO:");
    }

    private static List<Pedido> parseConCliente(String texto) {
        List<Pedido> pedidos = new ArrayList<>();
        String[] lineas = texto.split("\\r?\\n");
        String clienteActual = null;
        List<Producto> productosActuales = new ArrayList<>();

        for (String linea : lineas) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            if (linea.contains("PEDIDO:")) {
                if (clienteActual != null && !productosActuales.isEmpty()) {
                    pedidos.add(new Pedido(clienteActual, productosActuales));
                    productosActuales = new ArrayList<>();
                }
                clienteActual = linea.split("PEDIDO:")[0].trim();
                continue;
            }

            Producto producto = parseProductoConCliente(linea);
            if (producto != null) {
                productosActuales.add(producto);
            }
        }

        if (clienteActual != null && !productosActuales.isEmpty()) {
            pedidos.add(new Pedido(clienteActual, productosActuales));
        }

        return pedidos;
    }

    private static List<Pedido> parseSinCliente(String texto) {
        List<Producto> productos = new ArrayList<>();
        String[] lineas = texto.split("\\r?\\n");

        for (String linea : lineas) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            Producto producto = parseProductoSinCliente(linea);
            if (producto != null) {
                productos.add(producto);
            }
        }

        List<Pedido> lista = new ArrayList<>();
        lista.add(new Pedido("", productos));
        return lista;
    }

    private static Producto parseProductoConCliente(String linea) {
        String[] partes = linea.trim().split("\\s+");
        int cantidad = -1;
        StringBuilder descripcion = new StringBuilder();
        boolean inicioDescripcion = false;

        for (int i = 0; i < partes.length; i++) {
            String palabra = partes[i];

            try {
                int numero = Integer.parseInt(palabra);

                // Si estamos al comienzo y hay al menos dos números seguidos, salteamos el primero (el código)
                if (i == 0 && partes.length > i + 1 && partes[i + 1].matches("\\d+")) {
                    continue; // saltamos el código
                }

                // Si no tenemos cantidad todavía, este número es la cantidad
                if (cantidad == -1) {
                    cantidad = numero;
                    continue;
                }

            } catch (NumberFormatException ignored) {}

            // Cortar si parece un precio
            if (palabra.matches("\\d{1,3}(\\.\\d{3})*,\\d{2}")) break;

            // Comenzar a armar la descripción cuando haya texto
            if (!inicioDescripcion && palabra.matches(".*[a-zA-ZáéíóúÁÉÍÓÚñÑ].*")) {
                inicioDescripcion = true;
            }

            if (inicioDescripcion) {
                descripcion.append(palabra).append(" ");
            }
        }

        return (cantidad != -1 && descripcion.length() > 0)
                ? new Producto(cantidad, descripcion.toString().trim())
                : null;
    }

    private static Producto parseProductoSinCliente(String linea) {
        String[] partes = linea.trim().split("\\s+");
        int cantidad = -1;
        StringBuilder descripcion = new StringBuilder();
        boolean despuesDeCantidad = false;

        for (int i = 0; i < partes.length; i++) {
            String palabra = partes[i];

            try {
                int posibleCantidad = Integer.parseInt(palabra);
                if (cantidad == -1) {
                    cantidad = posibleCantidad;
                    despuesDeCantidad = true;
                    continue;
                }

                // Si ya encontré la cantidad, y este número parece un código, lo ignoro
                if (despuesDeCantidad && palabra.matches("\\d+")) {
                    continue;
                }

            } catch (NumberFormatException ignored) {}

            // Cortar si parece un precio
            if (palabra.matches("\\d{1,3}(\\.\\d{3})*,\\d{2}")) break;

            descripcion.append(palabra).append(" ");
        }

        return (cantidad != -1 && descripcion.length() > 0)
                ? new Producto(cantidad, descripcion.toString().trim())
                : null;
    }
}
