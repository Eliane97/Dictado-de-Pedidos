package com.eli.pedidos.utils;

import com.eli.pedidos.PDFUtils;
import com.eli.pedidos.Pedido;
import com.eli.pedidos.Producto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher; // Necesario para trabajar con Pattern
import java.util.regex.Pattern; // Necesario para trabajar con expresiones regulares

public class PDFParser {
    public static List<Pedido> parsePedidos(File archivoPdf) throws Exception {
        String texto = PDFUtils.extraerTexto(archivoPdf);

        if (esTipoConCliente(texto)) {
            return parseConCliente(texto);
        } else if (esTipoSinCliente(texto)) {
            return parseSinCliente(texto);
        } else {
            // Si tenés otro tipo, podés agregar más parseos aquí.
            return new ArrayList<>();
        }
    }

    private static boolean esTipoConCliente(String texto) {
        return texto.contains("PEDIDO"); // o cualquier otro criterio que uses
    }

    private static boolean esTipoSinCliente(String texto) {
        return !texto.contains("PEDIDO"); // o el criterio inverso
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
                clienteActual = linea.split("PEDIDO:")[0].trim(); // Asegurarse de que el split sea por "PEDIDO:"
                continue;
            }

            Producto producto = parseProducto(linea);
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

            Producto producto = parseProducto(linea);
            if (producto != null) {
                productos.add(producto);
            }
        }

        // Como no hay cliente, podés crear un Pedido con cliente vacío o null
        Pedido pedidoSinCliente = new Pedido("", productos);
        List<Pedido> lista = new ArrayList<>();
        lista.add(pedidoSinCliente);
        return lista;
    }

    private static Producto parseProducto(String linea) {
        // Patrón para extraer la cantidad y la descripción.
        // La descripción se truncará antes de un número (1 a 3 dígitos) seguido de un punto.
        // ^\\s*(\\d+)\\s+      : Captura la cantidad (grupo 1) al inicio de la línea.
        // (.*?)                : Captura la descripción de forma no codiciosa (grupo 2).
        // (?:\\s+\\d{1,3}\\..*)? : Grupo no capturado y opcional. Busca un patrón numérico
        //                        (1 a 3 dígitos, seguido de punto y cualquier cosa después)
        //                        y lo consume, haciendo que la descripción (.*?) se trunque antes.
        Pattern productoPattern = Pattern.compile("^\\s*(\\d+)\\s+(.*?)(?:\\s+\\d{1,3}\\..*)?$");

        Matcher matcher = productoPattern.matcher(linea);

        if (matcher.find()) {
            try {
                int cantidad = Integer.parseInt(matcher.group(1));
                String descripcion = matcher.group(2).trim();

                // Asegurarse de que la descripción no esté vacía después de truncar
                if (!descripcion.isEmpty()) {
                    return new Producto(cantidad, descripcion);
                }
            } catch (NumberFormatException e) {
                // Si la cantidad no es un número válido, no es un producto.
                return null;
            }
        }
        return null;
    }
}