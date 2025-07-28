/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ibarrasa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 *
 * @author raul1
 */
public class connection {
    // Conexión 1: MySQL local (MySQL Workbench)
    public static Connection getMySQLConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/ibarrasa_bd";
        String usuario = "root";
        String contrasena = "password";
        return DriverManager.getConnection(url, usuario, contrasena);
    }

    // Conexión 2: AWS RDS (MySQL en la nube)
    public static Connection getAWSConnection() throws SQLException {
        String url = "jdbc:mysql://<tu-endpoint-rds>.rds.amazonaws.com:3306/ibarrasa_bd";
        String usuario = "ragg";
        String contrasena = "alexis1911";
        return DriverManager.getConnection(url, usuario, contrasena);
    }

    // Conexión 3: MariaDB local (HeidiSQL)
    public static Connection getMariaDBConnection() throws SQLException {
        String url = "jdbc:mariadb://localhost:3307/ibarrasa_db";
        String usuario = "root";
        String contrasena = "mariaPassword";
        return DriverManager.getConnection(url, usuario, contrasena);
    }
}
