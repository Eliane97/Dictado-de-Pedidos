package com.eli.pedidos.utils;

import com.eli.pedidos.PDFUtils;
import com.eli.pedidos.Pedido;
import com.eli.pedidos.Producto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFParser {

    public enum TipoPDF {
        CLIENTE_UNICO,
        MULTIPLES_CLIENTES,
        SIN_CLIENTE
    }

    public static List<Pedido> parsePedidos(File archivoPdf) throws Exception {
        String texto = PDFUtils.extraerTexto(archivoPdf);
        TipoPDF tipo = detectarTipoPDF(texto);

        switch (tipo) {
            case CLIENTE_UNICO:
                return parseClienteUnico(texto.split("\\r?\\n"));
            case MULTIPLES_CLIENTES:
                return parseConClientes(texto);
            case SIN_CLIENTE:
            default:
                return parseSinCliente(texto);
        }
    }

    private static TipoPDF detectarTipoPDF(String texto) {
        int cantidadClientes = 0;
        boolean contieneCuit = false;

        String[] lineas = texto.split("\\r?\\n");

        for (String linea : lineas) {
            if (linea.toLowerCase().contains("pedido")) {
                cantidadClientes++;
            }
            if (linea.toLowerCase().contains("cuit")) {
                contieneCuit = true;
            }
        }

        if (cantidadClientes == 0) {
            return TipoPDF.SIN_CLIENTE;
        } else if (cantidadClientes == 2 && contieneCuit) {
            return TipoPDF.CLIENTE_UNICO;
        } else {
            return TipoPDF.MULTIPLES_CLIENTES;
        }
    }


    private static List<Pedido> parseConClientes(String texto) {
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

                if (despuesDeCantidad && palabra.matches("\\d+")) {
                    continue;
                }

            } catch (NumberFormatException ignored) {
            }

            if (palabra.matches("\\d{1,3}(\\.\\d{3})*,\\d{2}")) break;

            descripcion.append(palabra).append(" ");
        }

        return (cantidad != -1 && descripcion.length() > 0)
                ? new Producto(cantidad, descripcion.toString().trim())
                : null;
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

                if (i == 0 && partes.length > i + 1 && partes[i + 1].matches("\\d+")) {
                    continue;
                }

                if (cantidad == -1) {
                    cantidad = numero;
                    continue;
                }

            } catch (NumberFormatException ignored) {
            }

            if (palabra.matches("\\d{1,3}(\\.\\d{3})*,\\d{2}")) break;

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

        private static List<Pedido> parseClienteUnico(String[] lineas) {
            List<Producto> productos = new ArrayList<>();
            String cliente = null;

            // Patrón para "Destinatario:"
            Pattern destinatarioPattern = Pattern.compile("(?i)^\\s*destinatario\\s*:\\s*([^,]+)");

            // Patrón ajustado para "Razón social.:"
            Pattern razonSocialPattern = Pattern.compile("(?i)^\\s*raz[oó]n social\\s*\\.\\s*:\\s*\\d*\\s*(.+)$");

            // Búsqueda del nombre del cliente
            for (String linea : lineas) {
                Matcher matcher = destinatarioPattern.matcher(linea);
                if (matcher.find()) {
                    cliente = matcher.group(1).trim();
                    break;
                }
            }

            if (cliente == null) {
                for (String linea : lineas) {
                    Matcher matcher = razonSocialPattern.matcher(linea);
                    if (matcher.find()) {
                        cliente = matcher.group(1).trim();
                        break;
                    }
                }
            }

            // Lógica para parsear los productos
            boolean parsingProductos = false;

            for (String linea : lineas) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                // Activar el parsing de productos al encontrar la línea de encabezado de la tabla de productos.
                if (linea.contains("Código") && linea.contains("Descripción") && linea.contains("Cant.") && linea.contains("Vlr. unit.")) {
                    parsingProductos = true;
                    continue; // Saltar esta línea ya que es el encabezado de la tabla.
                }

                if (parsingProductos) {
                    // Detener el parseo de productos al encontrar la línea de resumen final.
                    if (linea.toLowerCase().contains("items") && linea.toLowerCase().contains("peso") && linea.toLowerCase().contains("total")) {
                        break;
                    }

                    // Intentar parsear la línea como un producto.
                    Producto producto = parseProductoClienteUnico(linea);
                    if (producto != null) {
                        productos.add(producto);
                    }
                }
            }

            List<Pedido> listaPedidos = new ArrayList<>();
            listaPedidos.add(new Pedido(cliente != null ? cliente : "", productos));
            return listaPedidos;
        }

        // Método auxiliar para parsear una línea de texto de producto
        private static Producto parseProductoClienteUnico(String linea) {
            // Nuevo patrón REGEX adaptado al formato de líneas de producto del nuevo PDF:
            // Ejemplo de línea: "95 Alf Rasta Blanco 18 962,50 17.325,00"
            // Captura:
            // Grupo 1: Código (ej. "95")
            // Grupo 2: Descripción (ej. "Alf Rasta Blanco") - puede contener espacios
            // Grupo 3: Cantidad (ej. "18") - asumiendo que es un entero
            // Grupo 4: Vlr. unit. (ej. "962,50") - puede contener comas y puntos
            // Grupo 5: Vlr. total (ej. "17.325,00") - puede contener comas y puntos
            Pattern productoPattern = Pattern.compile(
                    "^\\s*(\\d+)\\s+(.*?)\\s+(\\d+)\\s+([\\d.,]+)\\s+([\\d.,]+)$"
            );
            Matcher matcher = productoPattern.matcher(linea.trim());

            if (matcher.find() && matcher.groupCount() >= 5) { // Ahora esperamos al menos 5 grupos.
                String descripcion = matcher.group(2).trim(); // La descripción es el segundo grupo.
                String cantidadStr = matcher.group(3).trim(); // La cantidad es el tercer grupo (asumiendo que es el entero antes del Vlr. unit.).

                try {
                    int cantidad = Integer.parseInt(cantidadStr);

                    if (!descripcion.isEmpty()) { // Si la descripción no está vacía, es un producto válido.
                        return new Producto(cantidad, descripcion);
                    }
                } catch (NumberFormatException e) {
                    // Si la cantidad no es un número entero, esta línea no es un producto o el formato es inesperado.
                    // No es necesario imprimir aquí a menos que quieras depurar la inconsistencia.
                    // System.err.println("Error al parsear la cantidad '" + cantidadStr + "' en la línea: " + linea + ". " + e.getMessage());
                }
            }
            return null;
        }
    }

