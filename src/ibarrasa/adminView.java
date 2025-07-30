/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ibarrasa;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;

/**
 *
 * @author raul1
 */
public class adminView extends javax.swing.JFrame {
    
    private void agregarProductoNuevo() {
    String codigo = generarNuevoCodigoProducto();

    String nombre = JOptionPane.showInputDialog(null, "Ingrese el nombre del producto:");
    if (nombre == null || nombre.trim().isEmpty()) return;

    String descripcion = JOptionPane.showInputDialog(null, "Ingrese la descripción:");
    if (descripcion == null) descripcion = "";

    String stockStr = JOptionPane.showInputDialog(null, "Ingrese el stock inicial:");
    if (stockStr == null) return;

    int stock;
    try {
        stock = Integer.parseInt(stockStr);
        if (stock < 0) throw new NumberFormatException();
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(null, "Stock inválido.");
        return;
    }

    String precioStr = JOptionPane.showInputDialog(null, "Ingrese el precio:");
    if (precioStr == null) return;

    double precio;
    try {
        precio = Double.parseDouble(precioStr);
        if (precio < 0) throw new NumberFormatException();
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(null, "Precio inválido.");
        return;
    }

    

    try {
        
        Connection[] conexiones = {
        connection.getMySQLConnection(),
        connection.getAWSConnection(),
        connection.getMariaDBConnection()
    };
        for (Connection conn : conexiones) {
            if (conn == null) continue;

            String sql = "INSERT INTO productos (codigo, nombre, descripcion, stock, precio) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, codigo);
            ps.setString(2, nombre);
            ps.setString(3, descripcion);
            ps.setInt(4, stock);
            ps.setDouble(5, precio);

            ps.executeUpdate();
            ps.close();

            conn.close();
        }

        JOptionPane.showMessageDialog(null, "Producto agregado en todas las bases con código: " + codigo);
        cargarInventario();  

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error al agregar producto: " + ex.getMessage());
        ex.printStackTrace();
    }
}
    
    private String generarNuevoCodigoProducto() {
    String nuevoCodigo = "PRD021"; // default si no hay ninguno

    try (Connection conn = connection.getMySQLConnection()) {
        String sql = "SELECT codigo FROM productos WHERE codigo LIKE 'PRD%' ORDER BY codigo DESC LIMIT 1";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String ultimoCodigo = rs.getString("codigo"); // ej: "PRD020"
            // Extraer número
            int numero = Integer.parseInt(ultimoCodigo.substring(3));
            numero++; // siguiente número
            nuevoCodigo = String.format("PRD%03d", numero);
        }

        rs.close();
        ps.close();

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error generando código producto: " + e.getMessage());
        e.printStackTrace();
    }

    return nuevoCodigo;
}
    
    private void cargarInventario() {
    DefaultTableModel modelo = (DefaultTableModel) tableInven.getModel();
    modelo.setRowCount(0);

    try (Connection conn = connection.getMySQLConnection()) {
        String sql = "SELECT codigo, nombre, descripcion, stock, precio FROM productos ORDER BY id";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Object[] fila = {
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getInt("stock"),
                String.format("%.2f", rs.getDouble("precio"))
            };
            modelo.addRow(fila);
        }

        rs.close();
        ps.close();

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error cargando inventario: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    public void exportarFacturaCotizacionAdmin() {
    int filaSeleccionada = tableCot.getSelectedRow();
    if (filaSeleccionada == -1) {
        JOptionPane.showMessageDialog(null, "Seleccione una cotización válida.");
        return;
    }

    int cotizacionId = (int) tableCot.getValueAt(filaSeleccionada, 0);

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setSelectedFile(new java.io.File("Factura_Cotizacion_" + cotizacionId + ".pdf"));
    int option = fileChooser.showSaveDialog(null);
    if (option != JFileChooser.APPROVE_OPTION) return;
    String rutaArchivo = fileChooser.getSelectedFile().getAbsolutePath();

    Document documento = new Document();
    try {
        PdfWriter.getInstance(documento, new FileOutputStream(rutaArchivo));
        documento.open();

        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        Paragraph titulo = new Paragraph("Factura - Cotización", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);
        documento.add(new Paragraph(" "));

        try (Connection conn = connection.getMySQLConnection()) {

            String sqlCot = "SELECT nombre, fecha, precio_total, cliente_id FROM cotizaciones WHERE id = ?";
            PreparedStatement psCot = conn.prepareStatement(sqlCot);
            psCot.setInt(1, cotizacionId);
            ResultSet rsCot = psCot.executeQuery();

            if (!rsCot.next()) {
                JOptionPane.showMessageDialog(null, "Cotización no encontrada.");
                rsCot.close();
                psCot.close();
                documento.close();
                return;
            }

            String nombre = rsCot.getString("nombre");
            String fecha = rsCot.getString("fecha");
            double total = rsCot.getDouble("precio_total");
            int clienteId = rsCot.getInt("cliente_id");

            documento.add(new Paragraph("Cotización: " + nombre, subtituloFont));
            documento.add(new Paragraph("Fecha: " + fecha, textoFont));
            documento.add(new Paragraph("Cliente ID: " + clienteId, textoFont));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{4f, 1f, 2f, 2f});
            tabla.addCell("Producto");
            tabla.addCell("Cantidad");
            tabla.addCell("Precio Unitario");
            tabla.addCell("Subtotal");

            String sqlProd = "SELECT p.nombre, cp.cantidad, p.precio, (cp.cantidad * p.precio) as subtotal " +
                             "FROM cotizaciones_productos cp " +
                             "JOIN productos p ON cp.producto_id = p.id " +
                             "WHERE cp.cotizacion_id = ?";
            PreparedStatement psProd = conn.prepareStatement(sqlProd);
            psProd.setInt(1, cotizacionId);
            ResultSet rsProd = psProd.executeQuery();

            while (rsProd.next()) {
                tabla.addCell(rsProd.getString("nombre"));
                tabla.addCell(String.valueOf(rsProd.getInt("cantidad")));
                tabla.addCell(String.format("$%.2f", rsProd.getDouble("precio")));
                tabla.addCell(String.format("$%.2f", rsProd.getDouble("subtotal")));
            }
            rsProd.close();
            psProd.close();

            documento.add(tabla);
            documento.add(new Paragraph(" "));

            Paragraph pTotal = new Paragraph("Total a pagar: $" + String.format("%.2f", total), subtituloFont);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            documento.add(pTotal);

            rsCot.close();
            psCot.close();
        }

        documento.close();

        JOptionPane.showMessageDialog(null, "Factura PDF generada:\n" + rutaArchivo);

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "Error generando PDF: " + ex.getMessage());
        ex.printStackTrace();
    }
}
    
    private void cargarCotizacionesAdmin() {
    DefaultTableModel modelo = (DefaultTableModel) tableCot.getModel();
    modelo.setRowCount(0); 

    try (Connection conn = connection.getMySQLConnection()) {
        String sql = "SELECT id, nombre, cliente_id, precio_total, fecha FROM cotizaciones ORDER BY fecha DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Object[] fila = {
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("cliente_id"),
                String.format("%.2f", rs.getDouble("precio_total")),
                rs.getString("fecha")
            };
            modelo.addRow(fila);
        }

        rs.close();
        ps.close();

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error al cargar cotizaciones: " + e.getMessage());
        e.printStackTrace();
    }
}

    public void exportarFacturaPedidoAdmin() {
    int filaSeleccionada = tablePedidos.getSelectedRow();
    if (filaSeleccionada == -1) {
        JOptionPane.showMessageDialog(null, "Seleccione un pedido válido.");
        return;
    }

    int pedidoId = (int) tablePedidos.getValueAt(filaSeleccionada, 0);

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setSelectedFile(new java.io.File("Factura_Pedido_" + pedidoId + ".pdf"));
    int option = fileChooser.showSaveDialog(null);
    if (option != JFileChooser.APPROVE_OPTION) return;
    String rutaArchivo = fileChooser.getSelectedFile().getAbsolutePath();

    Document documento = new Document();
    try {
        PdfWriter.getInstance(documento, new FileOutputStream(rutaArchivo));
        documento.open();

        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        Paragraph titulo = new Paragraph("Factura - Pedido", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);
        documento.add(new Paragraph(" "));

        try (Connection conn = connection.getMySQLConnection()) {

            // Consulta pedido con cliente
            String sqlPed = "SELECT nombre, fecha, estado, precio_total, cliente_id FROM pedidos WHERE id = ?";
            PreparedStatement psPed = conn.prepareStatement(sqlPed);
            psPed.setInt(1, pedidoId);
            ResultSet rsPed = psPed.executeQuery();

            if (!rsPed.next()) {
                JOptionPane.showMessageDialog(null, "Pedido no encontrado.");
                rsPed.close();
                psPed.close();
                documento.close();
                return;
            }

            String nombre = rsPed.getString("nombre");
            String fecha = rsPed.getString("fecha");
            String estado = rsPed.getString("estado");
            double total = rsPed.getDouble("precio_total");
            int clienteId = rsPed.getInt("cliente_id");

            documento.add(new Paragraph("Pedido: " + nombre, subtituloFont));
            documento.add(new Paragraph("Fecha: " + fecha, textoFont));
            documento.add(new Paragraph("Estado: " + estado, textoFont));
            documento.add(new Paragraph("Cliente ID: " + clienteId, textoFont));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{4f, 1f, 2f, 2f});
            tabla.addCell("Producto");
            tabla.addCell("Cantidad");
            tabla.addCell("Precio Unitario");
            tabla.addCell("Subtotal");

            String sqlProd = "SELECT p.nombre, pp.cantidad, p.precio, (pp.cantidad * p.precio) as subtotal " +
                             "FROM pedidos_productos pp " +
                             "JOIN productos p ON pp.producto_id = p.id " +
                             "WHERE pp.pedido_id = ?";
            PreparedStatement psProd = conn.prepareStatement(sqlProd);
            psProd.setInt(1, pedidoId);
            ResultSet rsProd = psProd.executeQuery();

            while (rsProd.next()) {
                tabla.addCell(rsProd.getString("nombre"));
                tabla.addCell(String.valueOf(rsProd.getInt("cantidad")));
                tabla.addCell(String.format("$%.2f", rsProd.getDouble("precio")));
                tabla.addCell(String.format("$%.2f", rsProd.getDouble("subtotal")));
            }
            rsProd.close();
            psProd.close();

            documento.add(tabla);
            documento.add(new Paragraph(" "));

            Paragraph pTotal = new Paragraph("Total a pagar: $" + String.format("%.2f", total), subtituloFont);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            documento.add(pTotal);

            rsPed.close();
            psPed.close();
        }

        documento.close();

        JOptionPane.showMessageDialog(null, "Factura PDF generada:\n" + rutaArchivo);

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "Error generando PDF: " + ex.getMessage());
        ex.printStackTrace();
    }
}

    private void cargarPedidosAdmin() {
    DefaultTableModel modelo = (DefaultTableModel) tablePedidos.getModel();
    modelo.setRowCount(0); // Limpiar tabla

    try (Connection conn = connection.getMySQLConnection()) {
        String sql = "SELECT id, nombre, cliente_id, precio_total, fecha, estado FROM pedidos ORDER BY fecha DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Object[] fila = {
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("cliente_id"),
                String.format("%.2f", rs.getDouble("precio_total")),
                rs.getString("fecha"),
                rs.getString("estado")
            };
            modelo.addRow(fila);
        }

        rs.close();
        ps.close();

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error al cargar pedidos: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    
    /**
     * Creates new form adminView
     */
    public adminView() {
        initComponents();
        cargarPedidosAdmin();
        cargarCotizacionesAdmin();
        cargarInventario();
        DefaultComboBoxModel<String> modeloStatus = new DefaultComboBoxModel<>();
        modeloStatus.addElement("En Camino");
        modeloStatus.addElement("Entregado");
        statusBox.setModel(modeloStatus);
        tablePedidos.getSelectionModel().addListSelectionListener(e -> {
    if (!e.getValueIsAdjusting()) {
        int fila = tablePedidos.getSelectedRow();
        if (fila >= 0) {
            String estado = tablePedidos.getValueAt(fila, 5).toString(); // columna estado
            statusBox.setSelectedItem(estado);
            // Si ya está entregado, bloquear combo
            if (estado.equalsIgnoreCase("Entregado")) {
                statusBox.setEnabled(false);
            } else {
                statusBox.setEnabled(true);
            }
        }
    }
});
    
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablePedidos = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        statusBox = new javax.swing.JComboBox<>();
        pedPDF = new javax.swing.JToggleButton();
        statUpdate = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableCot = new javax.swing.JTable();
        pdf = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tableInven = new javax.swing.JTable();
        newProd = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(680, 399));

        tablePedidos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "ID", "Nombre", "ID del Cliente", "Total", "Fecha", "Estado"
            }
        ));
        jScrollPane1.setViewportView(tablePedidos);

        jLabel2.setText("Estado:");

        statusBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        pedPDF.setText("Exportar PDF");
        pedPDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pedPDFActionPerformed(evt);
            }
        });

        statUpdate.setText("Actualizar");
        statUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statUpdateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(statUpdate)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(statusBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(pedPDF)))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(statusBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pedPDF))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statUpdate)
                .addContainerGap(67, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Pedidos", jPanel1);

        tableCot.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "ID", "Nombre", "ID del Cliente", "Total", "Fecha"
            }
        ));
        jScrollPane2.setViewportView(tableCot);

        pdf.setText("Descargar PDF");
        pdf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pdfActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(pdf)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pdf)
                .addContainerGap(97, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Cotizaciones", jPanel2);

        tableInven.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Código", "Nombre", "Descripción", "En Stock", "Precio"
            }
        ));
        jScrollPane3.setViewportView(tableInven);

        newProd.setText("Nuevo");
        newProd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newProdActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(newProd)
                        .addGap(0, 562, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(newProd)
                .addContainerGap(54, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Inventario", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void pedPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pedPDFActionPerformed
        exportarFacturaPedidoAdmin();
    }//GEN-LAST:event_pedPDFActionPerformed

    private void statUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statUpdateActionPerformed
        int fila = tablePedidos.getSelectedRow();
    if (fila == -1) {
        JOptionPane.showMessageDialog(null, "Seleccione un pedido primero.");
        return;
    }

    String estadoActual = tablePedidos.getValueAt(fila, 5).toString();
    if (estadoActual.equalsIgnoreCase("Entregado")) {
        JOptionPane.showMessageDialog(null, "Este pedido ya está entregado y no puede cambiarse su estado.");
        return;
    }

    String nuevoEstado = statusBox.getSelectedItem().toString();
    if (nuevoEstado.equalsIgnoreCase(estadoActual)) {
        JOptionPane.showMessageDialog(null, "El estado seleccionado es el mismo que el actual.");
        return;
    }

    int pedidoId = (int) tablePedidos.getValueAt(fila, 0); // columna id

    try (Connection conn = connection.getMySQLConnection()) {
        String sqlUpdate = "UPDATE pedidos SET estado = ? WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sqlUpdate);
        ps.setString(1, nuevoEstado);
        ps.setInt(2, pedidoId);
        int mod = ps.executeUpdate();
        ps.close();

        if (mod > 0) {
            JOptionPane.showMessageDialog(null, "Estado actualizado correctamente.");
            // Actualizar tabla y combo
            tablePedidos.setValueAt(nuevoEstado, fila, 5);
            if (nuevoEstado.equalsIgnoreCase("Entregado")) {
                statusBox.setEnabled(false);
            }
        } else {
            JOptionPane.showMessageDialog(null, "No se pudo actualizar el estado.");
        }

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error al actualizar estado: " + ex.getMessage());
        ex.printStackTrace();
    }
    }//GEN-LAST:event_statUpdateActionPerformed

    private void pdfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdfActionPerformed
        exportarFacturaCotizacionAdmin();
    }//GEN-LAST:event_pdfActionPerformed

    private void newProdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProdActionPerformed
        agregarProductoNuevo();
    }//GEN-LAST:event_newProdActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(adminView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(adminView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(adminView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(adminView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new adminView().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton newProd;
    private javax.swing.JButton pdf;
    private javax.swing.JToggleButton pedPDF;
    private javax.swing.JButton statUpdate;
    private javax.swing.JComboBox<String> statusBox;
    private javax.swing.JTable tableCot;
    private javax.swing.JTable tableInven;
    private javax.swing.JTable tablePedidos;
    // End of variables declaration//GEN-END:variables
}
