
package com.eli.pedidos;

import java.util.List;

public class Pedido {
    private String cliente;
    private List<Producto> productos;

    public Pedido(String cliente, List<Producto> productos) {
        this.cliente = cliente;
        this.productos = productos;
    }

    public String getCliente() {
        return cliente;
    }

    public List<Producto> getProductos() {
        return productos;
    }
}
