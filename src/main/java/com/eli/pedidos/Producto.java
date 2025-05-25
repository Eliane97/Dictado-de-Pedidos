
package com.eli.pedidos;

public class Producto {
    private int cantidad;
    private String descripcion;

    public Producto(int cantidad, String descripcion) {
        this.cantidad = cantidad;
        this.descripcion = descripcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return cantidad + " " + descripcion;
    }
}
