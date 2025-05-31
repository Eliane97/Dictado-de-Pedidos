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
        // --- DEBUG: Contenido RAW del PDF extraído ---
        System.out.println("DEBUG: Contenido RAW del PDF extraído (primeras 500 chars):\n---INICIO CONTENIDO PDF---");
        // Asegurarse de que el texto no es null o vacío antes de substring
        if (texto != null && !texto.isEmpty()) {
            System.out.println(texto.substring(0, Math.min(texto.length(), 500)) + (texto.length() > 500 ? "..." : ""));
        } else {
            System.out.println("[Texto extraído vacío o nulo]");
        }
        System.out.println("---FIN CONTENIDO PDF---");
        // --- FIN DEBUG ---

        TipoPDF tipo = detectarTipoPDF(texto);
        // --- DEBUG: TipoPDF detectado ---
        System.out.println("DEBUG: TipoPDF detectado: " + tipo);
        // --- FIN DEBUG ---

        switch (tipo) {
            case CLIENTE_UNICO:
                System.out.println("DEBUG: El PDF fue clasificado como CLIENTE_UNICO. Llamando a parseClienteUnico...");
                return parseClienteUnico(texto.split("\\r?\\n"));
            case MULTIPLES_CLIENTES:
                System.out.println("DEBUG: El PDF fue clasificado como MULTIPLES_CLIENTES. Llamando a parseConClientes...");
                return parseConClientes(texto);
            case SIN_CLIENTE:
            default:
                System.out.println("DEBUG: El PDF fue clasificado como SIN_CLIENTE. Llamando a parseSinCliente...");
                return parseSinCliente(texto);
        }
    }

    private static TipoPDF detectarTipoPDF(String texto) {
        // --- DEBUG: Iniciando detección de TipoPDF ---
        System.out.println("DEBUG: Iniciando detección de TipoPDF...");
        // --- FIN DEBUG ---

        int cantidadClientes = 0;
        boolean contieneCuit = false;

        // Si el texto es nulo o vacío, no hay nada que detectar
        if (texto == null || texto.isEmpty()) {
            System.out.println("DEBUG: Texto vacío para detección de TipoPDF. Asumiendo SIN_CLIENTE.");
            return TipoPDF.SIN_CLIENTE;
        }

        String[] lineas = texto.split("\\r?\\n");

        for (String linea : lineas) {
            // Revisa si 'pedido' (en cualquier caso) está en la línea
            if (linea.toLowerCase().contains("pedido")) {
                cantidadClientes++;
            }
            // Revisa si 'cuit' (en cualquier caso) está en la línea
            if (linea.toLowerCase().contains("cuit")) {
                contieneCuit = true;
            }
        }

        // --- DEBUG: Resultados de detección ---
        System.out.println("DEBUG: Cantidad de líneas con 'pedido': " + cantidadClientes);
        System.out.println("DEBUG: Contiene la palabra 'cuit': " + contieneCuit);
        // --- FIN DEBUG ---

        if (cantidadClientes == 0) {
            return TipoPDF.SIN_CLIENTE;
        } else if ((cantidadClientes == 1 ||cantidadClientes == 2) && contieneCuit) {
            return TipoPDF.CLIENTE_UNICO;
        } else {
            return TipoPDF.MULTIPLES_CLIENTES;
        }
    }


    private static List<Pedido> parseConClientes(String texto) {
        // Implementación de parseConClientes (la tuya)
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
        // Implementación de parseSinCliente (la tuya)
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
        // Implementación de parseProductoSinCliente (la tuya)
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
        // Implementación de parseProductoConCliente (la tuya)
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

        // Patrón para detectar la CABECERA "Razón social.:" (sin asumir el nombre en la misma línea)
        Pattern razonSocialHeaderPattern = Pattern.compile("(?i)^\\s*raz[oó]n social\\s*\\.\\s*:$");

        System.out.println("--- INICIO DEPURACION CLIENTE ---");

        // PASO 1: Intentar buscar el nombre del cliente usando "Destinatario:"
        for (String linea : lineas) {
            System.out.println("DEBUG Cliente Linea (Destinatario): '" + linea.trim() + "'");
            Matcher matcher = destinatarioPattern.matcher(linea);
            if (matcher.find()) {
                cliente = matcher.group(1).trim();
                System.out.println("DEBUG Cliente (Destinatario) encontrado: '" + cliente + "'");
                break; // Cliente encontrado, salimos del bucle
            }
        }

        // PASO 2: Si no se encontró con "Destinatario:", buscar "Razón social.:" y el cliente en una línea posterior
        if (cliente == null) {
            System.out.println("DEBUG Destinatario no encontrado, buscando Razón social y cliente en línea posterior...");
            boolean encontradoRazonSocialHeader = false;
            for (int i = 0; i < lineas.length; i++) {
                String linea = lineas[i].trim();
                System.out.println("DEBUG Cliente Linea (Razón social) - Actual: '" + linea + "'");

                if (!encontradoRazonSocialHeader) {
                    Matcher matcher = razonSocialHeaderPattern.matcher(linea);
                    if (matcher.find()) {
                        encontradoRazonSocialHeader = true;
                        System.out.println("DEBUG Cabecera 'Razón social.:' encontrada. Buscando cliente en las siguientes líneas...");
                        // No rompemos aquí, continuamos para buscar el nombre en las siguientes líneas
                    }
                } else {
                    // Hemos encontrado la cabecera "Razón social.:".
                    // Ahora buscamos la primera línea no vacía que *no* termine en ':'
                    // y no sea una línea de guiones.
                    // Hemos añadido más condiciones para evitar capturar otras cabeceras.
                    if (!linea.isEmpty() &&
                            !linea.endsWith(":") && // <--- ¡CLAVE! Excluir líneas que terminan en dos puntos.
                            !linea.matches("^-+$") && // Excluir líneas de guiones (separadores)
                            !linea.toLowerCase().contains("nombre de fantasía") &&
                            !linea.toLowerCase().contains("telef./e-mail") &&
                            !linea.toLowerCase().contains("dirección") &&
                            !linea.toLowerCase().contains("próximo de") && // Excluir explícitamente esta cabecera
                            !linea.toLowerCase().contains("barrio") &&
                            !linea.toLowerCase().contains("localidad") &&
                            !linea.toLowerCase().contains("vendedor") &&
                            !linea.toLowerCase().contains("transportadora") &&
                            !linea.toLowerCase().contains("comentarios") &&
                            !linea.toLowerCase().contains("cp........") && // Añadido
                            !linea.toLowerCase().contains("cuit/dni") &&
                            !linea.toLowerCase().contains("dni/cuil") &&
                            !linea.toLowerCase().contains("emisión") &&
                            !linea.toLowerCase().contains("forma cobro") &&
                            !linea.toLowerCase().contains("cond. pago") &&
                            !linea.toLowerCase().contains("tipo flete") &&
                            !linea.toLowerCase().contains("ord.compra") &&
                            !linea.toLowerCase().contains("entrega") &&
                            !linea.toLowerCase().contains("lista") &&
                            !linea.matches("\\d{2}/\\d{2}/\\d{4}.*") // Evita capturar fechas como "Emisión..."
                    ) {
                        cliente = linea;
                        System.out.println("DEBUG Cliente (Razón social) encontrado en línea: '" + cliente + "'");
                        break; // Cliente encontrado, salimos del bucle
                    }
                }
            }
        } else {
            System.out.println("DEBUG Cliente ya encontrado por Destinatario: '" + cliente + "'");
        }
        System.out.println("--- FIN DEPURACION CLIENTE ---");

        // ... (el resto del método parseClienteUnico, incluyendo el parseo de productos, se mantiene igual) ...

        boolean parsingProductos = false;

        for (String linea : lineas) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            // Activar el parsing de productos al encontrar la línea de encabezado de la tabla.
            if (linea.contains("Código") && linea.contains("Descripción") && linea.contains("Cant.") && linea.contains("Vlr. unit.")) {
                parsingProductos = true;
                continue; // Saltar esta línea.
            }

            if (parsingProductos) {
                // Detener el parseo de productos al encontrar la línea de resumen final.
                if (linea.toLowerCase().contains("items") && linea.toLowerCase().contains("peso") && linea.toLowerCase().contains("total")) {
                    break;
                }

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
        // Implementación de parseProductoClienteUnico (la tuya)
        // Patrón para una línea de producto en formato: Código Descripción Cantidad Vlr.unit. Vlr.total
        Pattern productoPattern = Pattern.compile(
                "^\\s*([a-zA-Z0-9]+)\\s+(.*?)\\s+(\\d+)\\s+([\\d.,]+)\\s+([\\d.,]+)$"
        );
        Matcher matcher = productoPattern.matcher(linea.trim());

        if (matcher.find() && matcher.groupCount() >= 5) {
            String descripcion = matcher.group(2).trim(); // La descripción es el segundo grupo.
            String cantidadStr = matcher.group(3).trim(); // La cantidad es el tercer grupo.

            try {
                if (cantidadStr.isEmpty()) {
                    return null;
                }
                int cantidad = Integer.parseInt(cantidadStr);

                if (!descripcion.isEmpty()) {
                    return new Producto(cantidad, descripcion);
                }
            } catch (NumberFormatException e) {
                // Se omite la impresión de error para no generar ruido por líneas que no son productos.
            }
        }
        return null;
    }
    }

