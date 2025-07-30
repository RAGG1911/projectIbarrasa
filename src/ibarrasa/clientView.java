/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package ibarrasa;
import java.util.List;
import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author raul1
 */





public class clientView extends javax.swing.JFrame {
    
    private void cargarPedidosEnTabla() {
    DefaultTableModel modelo = new DefaultTableModel();
    modelo.addColumn("Nombre");
    modelo.addColumn("Precio Total");
    modelo.addColumn("Fecha");
    modelo.addColumn("Estado");

    

    try {
        Connection con = connection.getMySQLConnection(); // Puedes cambiar la conexión según necesites

    if (con == null) {
        JOptionPane.showMessageDialog(null, "No se pudo conectar a la base de datos.");
        return;
    }
        PreparedStatement ps = con.prepareStatement(
            "SELECT nombre, precio_total, fecha, estado FROM pedidos WHERE cliente_id = ?"
        );
        ps.setInt(1, idCliente);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String nombre = rs.getString("nombre");
            double total = rs.getDouble("precio_total");
            Timestamp fecha = rs.getTimestamp("fecha");
            String estado = rs.getString("estado");

            modelo.addRow(new Object[]{
                nombre,
                String.format("$ %.2f", total),
                fecha.toString(),
                estado
            });
        }

        rs.close();
        ps.close();
        con.close();

        tablePedidos.setModel(modelo);

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error al cargar pedidos: " + ex.getMessage());
        ex.printStackTrace();
    }
}

    
    private void eliminarCotizacion(String nombreCot) {
    if (nombreCot == null || nombreCot.trim().isEmpty()) {
        JOptionPane.showMessageDialog(null, "Seleccione una cotización válida.");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(null,
            "¿Está seguro de eliminar la cotización: " + nombreCot + "?", 
            "Confirmar eliminación", JOptionPane.YES_NO_OPTION);

    if (confirm != JOptionPane.YES_OPTION) return;

    try {
        Connection[] conexiones = {
            connection.getMySQLConnection(),
            connection.getAWSConnection(),
            connection.getMariaDBConnection()
        };

        for (Connection con : conexiones) {
            if (con == null) continue;

            // Buscar ID de la cotización por nombre
            PreparedStatement psBuscar = con.prepareStatement(
                "SELECT id FROM cotizaciones WHERE nombre = ? AND cliente_id = ?"
            );
            psBuscar.setString(1, nombreCot);
            psBuscar.setInt(2, idCliente);
            ResultSet rs = psBuscar.executeQuery();

            int idCotizacion = -1;
            if (rs.next()) {
                idCotizacion = rs.getInt("id");
            }
            rs.close();
            psBuscar.close();

            if (idCotizacion != -1) {
                // Eliminar cotización (FK con ON DELETE CASCADE borra detalles)
                PreparedStatement psEliminar = con.prepareStatement(
                    "DELETE FROM cotizaciones WHERE id = ?"
                );
                psEliminar.setInt(1, idCotizacion);
                psEliminar.executeUpdate();
                psEliminar.close();
            }

            con.close();
        }

        JOptionPane.showMessageDialog(null, "Cotización eliminada correctamente.");

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error al eliminar: " + ex.getMessage());
        ex.printStackTrace();
    }
}

    
    private String generarCodigoCotizacion(Connection con) throws SQLException {
    String codigo = "COT";
    String sql = "SELECT COUNT(*) AS total FROM cotizaciones";
    PreparedStatement ps = con.prepareStatement(sql);
    ResultSet rs = ps.executeQuery();
    int count = 0;
    if (rs.next()) {
        count = rs.getInt("total");
    }
    rs.close();
    ps.close();

    
    String numero = String.format("%03d", count + 1);
    return codigo + numero;  
}

    
    private void actualizarTotalCotizacion() {
    DefaultTableModel model = (DefaultTableModel) newCotTable.getModel();
    double total = 0.0;

    for (int i = 0; i < model.getRowCount(); i++) {
        int cantidad = Integer.parseInt(model.getValueAt(i, 2).toString());
        double precio = Double.parseDouble(model.getValueAt(i, 3).toString());
        total += cantidad * precio;
    }

    newPrice.setText(String.format("%.2f", total));
}
    
    private void prepararNuevaCotizacion() {
    
    DefaultTableModel model = (DefaultTableModel) newCotTable.getModel();
    model.setRowCount(0);

    
    newPrice.setText("");

    
    itemCant.setText("");
    searchField.setText("");

    
    productBox.removeAllItems();

    try {
        Connection con = connection.getMySQLConnection();
        String sql = "SELECT id, nombre FROM productos ORDER BY nombre";
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            int id = rs.getInt("id");
            String nombre = rs.getString("nombre");
            ((JComboBox) productBox).addItem(new ItemProducto(id, nombre));
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al cargar productos: " + e.getMessage());
    }
}
    
    public class ItemProducto {
    private int id;
    private String nombre;

    public ItemProducto(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    
    @Override
    public String toString() {
        return nombre;  
    }
}
    
    private Map<String, Integer> cotizacionesMap = new HashMap<>();
    
 
    
    public class CotizacionItem {
        private int id;
        private String nombre;

        public CotizacionItem(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
    
    private void cargarCotizaciones(int cliente_id) {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        cotizacionesMap.clear(); 

    try {
        Connection con = connection.getMySQLConnection(); 
        String sql = "SELECT id, nombre FROM cotizaciones WHERE cliente_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, cliente_id);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            int id = rs.getInt("id");
            String nombre = rs.getString("nombre");

            modelo.addElement(nombre);
            cotizacionesMap.put(nombre, id); 
        }

        cotList.setModel(modelo);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al cargar cotizaciones: " + e.getMessage());
    }
    }
    
    
    
    private int idCliente;
    /**
     * Creates new form clientView
     */
    public clientView(int idCliente) {
        initComponents();        
        this.idCliente = idCliente;
        cargarCotizaciones(idCliente);
        cargarPedidosEnTabla();
        prepararNuevaCotizacion();
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cotMenu = new javax.swing.JPopupMenu();
        deleteItem = new javax.swing.JMenuItem();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        cotList = new javax.swing.JList<>();
        viewCot = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        viewCotTable = new javax.swing.JTable();
        pdf = new javax.swing.JButton();
        buy = new javax.swing.JButton();
        delete = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        priceField = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tablePedidos = new javax.swing.JTable();
        cancelPed = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        productBox = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        itemCant = new javax.swing.JTextField();
        addItem = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        newCotTable = new javax.swing.JTable();
        saveCot = new javax.swing.JButton();
        searchField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        newPrice = new javax.swing.JTextField();
        searchItem = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();

        deleteItem.setText("Eliminar");
        deleteItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteItemActionPerformed(evt);
            }
        });
        cotMenu.add(deleteItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        cotList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(cotList);

        viewCot.setText("Ver");
        viewCot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewCotActionPerformed(evt);
            }
        });

        viewCotTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Nombre", "Descripcion", "Cantidad", "Precio Unitario"
            }
        ));
        jScrollPane2.setViewportView(viewCotTable);

        pdf.setText("Descargar como PDF");

        buy.setText("Realizar Pedido");
        buy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyActionPerformed(evt);
            }
        });

        delete.setText("Eliminar");
        delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteActionPerformed(evt);
            }
        });

        jLabel4.setText("Total:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1)
                    .addComponent(viewCot, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(pdf)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(delete)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buy))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 545, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(priceField, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(priceField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pdf)
                    .addComponent(buy)
                    .addComponent(delete)
                    .addComponent(viewCot))
                .addContainerGap(80, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Mis Cotizaciones", jPanel1);

        tablePedidos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane4.setViewportView(tablePedidos);

        cancelPed.setText("Cancelar Pedido");
        cancelPed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelPedActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(cancelPed)
                        .addGap(515, 515, 515)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelPed)
                .addContainerGap(98, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Mis Pedidos", jPanel4);

        jLabel1.setText("Producto:");

        productBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel2.setText("Cantidad:");

        addItem.setText("Añadir");
        addItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addItemActionPerformed(evt);
            }
        });

        newCotTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Producto", "Descripción", "Precio Unitario", "Cantidad"
            }
        ));
        newCotTable.setComponentPopupMenu(cotMenu);
        jScrollPane3.setViewportView(newCotTable);

        saveCot.setText("Guardar");
        saveCot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCotActionPerformed(evt);
            }
        });

        jLabel5.setText("Total:");

        searchItem.setText("Buscar:");
        searchItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchItemActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(saveCot)
                        .addGap(379, 379, 379))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 453, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(searchItem)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                            .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(productBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addComponent(jLabel5))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(itemCant, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                                .addComponent(newPrice))))
                    .addComponent(addItem))
                .addContainerGap(59, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(searchItem)
                            .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(productBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(itemCant, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(newPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addItem)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveCot)
                .addContainerGap(91, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Nueva Cotización", jPanel2);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 675, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 348, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Ibarrín", jPanel3);

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

    private void addItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addItemActionPerformed
        ItemProducto seleccionado = (ItemProducto) productBox.getSelectedItem();
if (seleccionado == null) {
    JOptionPane.showMessageDialog(this, "Selecciona un producto.");
    return;
}

String cantidadStr = itemCant.getText().trim();
if (cantidadStr.isEmpty() || !cantidadStr.matches("\\d+")) {
    JOptionPane.showMessageDialog(this, "Ingresa una cantidad válida.");
    return;
}

int cantidad = Integer.parseInt(cantidadStr);

try {
    Connection con = connection.getMySQLConnection();
    String sql = "SELECT descripcion, precio, stock FROM productos WHERE id = ?";
    PreparedStatement ps = con.prepareStatement(sql);
    ps.setInt(1, seleccionado.getId());
    ResultSet rs = ps.executeQuery();

    if (rs.next()) {
        String descripcion = rs.getString("descripcion");
        double precio = rs.getDouble("precio");
        int stock = rs.getInt("stock");

        if (cantidad > stock) {
            JOptionPane.showMessageDialog(this, "No hay suficiente stock disponible (Stock: " + stock + ")");
            return;
        }

        // Agregar producto a la tabla
        DefaultTableModel model = (DefaultTableModel) newCotTable.getModel();
        model.addRow(new Object[]{
            seleccionado.toString(), descripcion, cantidad, precio
        });

        actualizarTotalCotizacion();
        itemCant.setText(""); // limpiar campo de cantidad

    } else {
        JOptionPane.showMessageDialog(this, "Producto no encontrado.");
    }

} catch (Exception e) {
    JOptionPane.showMessageDialog(this, "Error al añadir producto: " + e.getMessage());
}
    }//GEN-LAST:event_addItemActionPerformed

    private void viewCotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewCotActionPerformed
        String nombreSeleccionado = cotList.getSelectedValue();

if (nombreSeleccionado == null) {
    JOptionPane.showMessageDialog(this, "Selecciona una cotización.");
    return;
}

int idCotizacion = cotizacionesMap.get(nombreSeleccionado);


try {
   
    DefaultTableModel model = (DefaultTableModel) viewCotTable.getModel();
    model.setRowCount(0);

   
    Connection con = connection.getMySQLConnection();

    
    String sql = "SELECT p.nombre, p.descripcion, cp.cantidad, p.precio " +
                 "FROM cotizaciones_productos cp " +
                 "JOIN productos p ON cp.producto_id = p.id " +
                 "WHERE cp.cotizacion_id = ?";
    PreparedStatement ps = con.prepareStatement(sql);
    ps.setInt(1, idCotizacion);
    ResultSet rs = ps.executeQuery();

    double totalCotizacion = 0.0;

    
    while (rs.next()) {
        String nombre = rs.getString("nombre");
        String descripcion = rs.getString("descripcion");
        int cantidad = rs.getInt("cantidad");
        double precio = rs.getDouble("precio");

        model.addRow(new Object[]{nombre, descripcion, cantidad, precio});
        totalCotizacion += (cantidad * precio);
    }

    
    priceField.setText(String.format("%.2f", totalCotizacion));

} catch (Exception e) {
    JOptionPane.showMessageDialog(this, "Error al cargar detalles: " + e.getMessage());
}
    }//GEN-LAST:event_viewCotActionPerformed

    private void searchItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchItemActionPerformed
     String codigoBuscado = searchField.getText().trim();

    if (codigoBuscado.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Ingrese un código para buscar.");
        return;
    }

    try {
        Connection con = connection.getMySQLConnection();
        String sql = "SELECT id, nombre FROM productos WHERE codigo = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, codigoBuscado);
        ResultSet rs = ps.executeQuery();

        productBox.removeAllItems(); // limpiar antes de mostrar el resultado

        if (rs.next()) {
            int id = rs.getInt("id");
            String nombre = rs.getString("nombre");
            ((JComboBox) productBox).addItem(new ItemProducto(id, nombre));
        } else {
            JOptionPane.showMessageDialog(this, "Producto no encontrado.");
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al buscar producto: " + e.getMessage());
    }
    }//GEN-LAST:event_searchItemActionPerformed

    private void deleteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteItemActionPerformed
        int fila = newCotTable.getSelectedRow();
        if (fila != -1) {
            DefaultTableModel model = (DefaultTableModel) newCotTable.getModel();
            model.removeRow(fila);
            actualizarTotalCotizacion();
        }
    }//GEN-LAST:event_deleteItemActionPerformed

    private void saveCotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCotActionPerformed
        
    String nombreCot = JOptionPane.showInputDialog(null, "Ingrese el nombre de la cotización:");
    if (nombreCot == null || nombreCot.trim().isEmpty()) {
        JOptionPane.showMessageDialog(null, "Debe ingresar un nombre válido.");
        return;
    }

    double precioTotal = Double.parseDouble(newPrice.getText());
    java.sql.Date fechaActual = new java.sql.Date(System.currentTimeMillis());

    try {
        Connection[] conexiones = {
            connection.getMySQLConnection(),
            connection.getAWSConnection(),
            connection.getMariaDBConnection()
        };

        for (Connection con : conexiones) {
            if (con == null) continue;

            String codigoCotizacion = generarCodigoCotizacion(con);
            PreparedStatement psCot = con.prepareStatement(
                "INSERT INTO cotizaciones (codigo, nombre, cliente_id, fecha, precio_total) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            psCot.setString(1, codigoCotizacion);
            psCot.setString(2, nombreCot);
            psCot.setInt(3, idCliente);
            psCot.setDate(4, fechaActual);
            psCot.setDouble(5, precioTotal);
            psCot.executeUpdate();

            ResultSet rs = psCot.getGeneratedKeys();
            int idCotizacion = -1;
            if (rs.next()) {
                idCotizacion = rs.getInt(1);
            }
            rs.close();
            psCot.close();

            if (idCotizacion != -1) {
                for (int i = 0; i < newCotTable.getRowCount(); i++) {
                    String nombreProd = newCotTable.getValueAt(i, 0).toString();
                    int cantidad = (int) newCotTable.getValueAt(i, 2);

                    PreparedStatement psBuscar = con.prepareStatement(
                        "SELECT id FROM productos WHERE nombre = ?"
                    );
                    psBuscar.setString(1, nombreProd);
                    ResultSet rsProd = psBuscar.executeQuery();

                    if (rsProd.next()) {
                        int productoId = rsProd.getInt("id");

                        PreparedStatement psDet = con.prepareStatement(
                            "INSERT INTO cotizaciones_productos (cotizacion_id, producto_id, cantidad) VALUES (?, ?, ?)"
                        );
                        psDet.setInt(1, idCotizacion);
                        psDet.setInt(2, productoId);
                        psDet.setInt(3, cantidad);
                        psDet.executeUpdate();
                        psDet.close();
                    }

                    rsProd.close();
                    psBuscar.close();
                }
            }

            con.close();
        }

        JOptionPane.showMessageDialog(null, "Cotización guardada");
        cargarCotizaciones(idCliente);
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error al guardar la cotización: " + ex.getMessage());
        ex.printStackTrace();
    }
    }//GEN-LAST:event_saveCotActionPerformed

    private void deleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteActionPerformed
        String seleccion = (String) cotList.getSelectedValue(); // Asegúrate que cotList contenga Strings
        eliminarCotizacion(seleccion);
        cargarCotizaciones(idCliente); // Si tienes método para recargar la lista
    }//GEN-LAST:event_deleteActionPerformed

    private void buyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buyActionPerformed
        String nombreCot = cotList.getSelectedValue();

    if (nombreCot == null || nombreCot.trim().isEmpty()) {
        JOptionPane.showMessageDialog(null, "Seleccione una cotización válida.");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(null,
            "¿Está seguro de convertir la cotización '" + nombreCot + "' en pedido?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION);

    if (confirm != JOptionPane.YES_OPTION) return;

    

    try {
        Connection[] conexiones = {
        connection.getMySQLConnection(),
        connection.getAWSConnection(),
        connection.getMariaDBConnection()
    };
        for (Connection con : conexiones) {
            if (con == null) {
                System.out.println("Una conexión no está disponible, se omite.");
                continue;
            }

            // 1) Obtener cotización
            PreparedStatement psCot = con.prepareStatement(
                "SELECT id, codigo, nombre, cliente_id, fecha FROM cotizaciones WHERE nombre = ? AND cliente_id = ?"
            );
            psCot.setString(1, nombreCot);
            psCot.setInt(2, idCliente);
            ResultSet rsCot = psCot.executeQuery();

            if (!rsCot.next()) {
                rsCot.close();
                psCot.close();
                con.close();
                continue;
            }

            int idCotizacion = rsCot.getInt("id");
            String codigoCot = rsCot.getString("codigo");
            String nombreCotDB = rsCot.getString("nombre");
            int clienteCot = rsCot.getInt("cliente_id");
            Timestamp fechaCot = rsCot.getTimestamp("fecha");

            rsCot.close();
            psCot.close();

            // 2) Obtener productos de la cotización y calcular precio total
            PreparedStatement psProdCot = con.prepareStatement(
                "SELECT cp.producto_id, cp.cantidad, p.precio FROM cotizaciones_productos cp " +
                "JOIN productos p ON cp.producto_id = p.id WHERE cp.cotizacion_id = ?"
            );
            psProdCot.setInt(1, idCotizacion);
            ResultSet rsProd = psProdCot.executeQuery();

            // Guardar productos temporalmente
            List<int[]> productos = new ArrayList<>();
            double precioTotal = 0.0;

            while (rsProd.next()) {
                int productoId = rsProd.getInt("producto_id");
                int cantidad = rsProd.getInt("cantidad");
                double precioUnitario = rsProd.getDouble("precio");

                productos.add(new int[]{productoId, cantidad});
                precioTotal += cantidad * precioUnitario;
            }

            rsProd.close();
            psProdCot.close();

            // 3) Insertar nuevo pedido con precio_total
            PreparedStatement psInsertPed = con.prepareStatement(
                "INSERT INTO pedidos (codigo, nombre, cliente_id, fecha, estado, precio_total) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            psInsertPed.setString(1, codigoCot);
            psInsertPed.setString(2, nombreCotDB);
            psInsertPed.setInt(3, clienteCot);
            psInsertPed.setTimestamp(4, fechaCot);
            psInsertPed.setString(5, "Realizado");
            psInsertPed.setDouble(6, precioTotal);
            psInsertPed.executeUpdate();

            ResultSet rsKeys = psInsertPed.getGeneratedKeys();
            int idPedido;
            if (rsKeys.next()) {
                idPedido = rsKeys.getInt(1);
            } else {
                psInsertPed.close();
                con.close();
                throw new SQLException("No se pudo obtener el ID del pedido.");
            }
            rsKeys.close();
            psInsertPed.close();

            // 4) Insertar productos en pedidos_productos y actualizar stock
            PreparedStatement psInsertProdPed = con.prepareStatement(
                "INSERT INTO pedidos_productos (pedido_id, producto_id, cantidad) VALUES (?, ?, ?)"
            );

            for (int[] prod : productos) {
                int productoId = prod[0];
                int cantidad = prod[1];

                psInsertProdPed.setInt(1, idPedido);
                psInsertProdPed.setInt(2, productoId);
                psInsertProdPed.setInt(3, cantidad);
                psInsertProdPed.executeUpdate();

                // Actualizar stock
                PreparedStatement psUpdateStock = con.prepareStatement(
                    "UPDATE productos SET stock = stock - ? WHERE id = ?"
                );
                psUpdateStock.setInt(1, cantidad);
                psUpdateStock.setInt(2, productoId);
                psUpdateStock.executeUpdate();
                psUpdateStock.close();
            }

            psInsertProdPed.close();

            // 5) Eliminar cotización
            PreparedStatement psEliminarCot = con.prepareStatement(
                "DELETE FROM cotizaciones WHERE id = ?"
            );
            psEliminarCot.setInt(1, idCotizacion);
            psEliminarCot.executeUpdate();
            psEliminarCot.close();

            con.close();
        }

        JOptionPane.showMessageDialog(null, "Pedido Realizado");
        cargarCotizaciones(idCliente);
        cargarPedidosEnTabla();
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error al realizar pedido: " + ex.getMessage());
        ex.printStackTrace();
    }
    }//GEN-LAST:event_buyActionPerformed

    private void cancelPedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelPedActionPerformed
        int filaSeleccionada = tablePedidos.getSelectedRow();

    if (filaSeleccionada == -1) {
        JOptionPane.showMessageDialog(null, "Seleccione un pedido para cancelar.");
        return;
    }

    String nombrePedido = tablePedidos.getValueAt(filaSeleccionada, 0).toString();

    int confirm = JOptionPane.showConfirmDialog(null,
        "¿Está seguro de cancelar el pedido: " + nombrePedido + "?",
        "Confirmar cancelación", JOptionPane.YES_NO_OPTION);

    if (confirm != JOptionPane.YES_OPTION) return;

    try {
        Connection[] conexiones = {
            connection.getMySQLConnection(),
            connection.getAWSConnection(),
            connection.getMariaDBConnection()
        };

        for (Connection con : conexiones) {
            if (con == null) continue;

            // Buscar ID del pedido
            int pedidoId = -1;
            PreparedStatement psBuscar = con.prepareStatement(
                "SELECT id FROM pedidos WHERE nombre = ? AND cliente_id = ?"
            );
            psBuscar.setString(1, nombrePedido);
            psBuscar.setInt(2, idCliente);
            ResultSet rs = psBuscar.executeQuery();

            if (rs.next()) {
                pedidoId = rs.getInt("id");
            }
            rs.close();
            psBuscar.close();

            if (pedidoId != -1) {
                // Obtener productos del pedido
                PreparedStatement psProductos = con.prepareStatement(
                    "SELECT producto_id, cantidad FROM pedidos_productos WHERE pedido_id = ?"
                );
                psProductos.setInt(1, pedidoId);
                ResultSet rsProductos = psProductos.executeQuery();

                // Restaurar stock
                while (rsProductos.next()) {
                    int productoId = rsProductos.getInt("producto_id");
                    int cantidad = rsProductos.getInt("cantidad");

                    PreparedStatement psActualizarStock = con.prepareStatement(
                        "UPDATE productos SET stock = stock + ? WHERE id = ?"
                    );
                    psActualizarStock.setInt(1, cantidad);
                    psActualizarStock.setInt(2, productoId);
                    psActualizarStock.executeUpdate();
                    psActualizarStock.close();
                }

                rsProductos.close();
                psProductos.close();

                // Eliminar el pedido (se eliminan también los productos por ON DELETE CASCADE)
                PreparedStatement psEliminar = con.prepareStatement(
                    "DELETE FROM pedidos WHERE id = ?"
                );
                psEliminar.setInt(1, pedidoId);
                psEliminar.executeUpdate();
                psEliminar.close();
            }

            con.close();
        }

        JOptionPane.showMessageDialog(null, "Pedido cancelado y stock restaurado.");
        cargarPedidosEnTabla(); // Refrescar tabla

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error al cancelar el pedido: " + ex.getMessage());
        ex.printStackTrace();
    }
    }//GEN-LAST:event_cancelPedActionPerformed

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
            java.util.logging.Logger.getLogger(clientView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(clientView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(clientView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(clientView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                
                new clientView(1).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addItem;
    private javax.swing.JButton buy;
    private javax.swing.JButton cancelPed;
    private javax.swing.JList<String> cotList;
    private javax.swing.JPopupMenu cotMenu;
    private javax.swing.JButton delete;
    private javax.swing.JMenuItem deleteItem;
    private javax.swing.JTextField itemCant;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable newCotTable;
    private javax.swing.JTextField newPrice;
    private javax.swing.JButton pdf;
    private javax.swing.JTextField priceField;
    private javax.swing.JComboBox<String> productBox;
    private javax.swing.JButton saveCot;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton searchItem;
    private javax.swing.JTable tablePedidos;
    private javax.swing.JButton viewCot;
    private javax.swing.JTable viewCotTable;
    // End of variables declaration//GEN-END:variables
}
