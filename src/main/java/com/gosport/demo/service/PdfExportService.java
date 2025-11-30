package com.gosport.demo.service;

import com.gosport.demo.model.User;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfExportService {

    // Color corporativo de GoSports (negro)
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(33, 37, 41); // #212529
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(52, 58, 64); // #343a40

    public ByteArrayOutputStream exportarUsuariosPdf(List<User> usuarios) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // ===============================
        // ENCABEZADO DEL DOCUMENTO
        // ===============================
        Paragraph titulo = new Paragraph("GOSPORTS")
            .setFontSize(24)
            .setBold()
            .setFontColor(HEADER_COLOR)
            .setTextAlignment(TextAlignment.CENTER);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph("Reporte de Usuarios Registrados")
            .setFontSize(14)
            .setFontColor(ACCENT_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(5);
        document.add(subtitulo);

        // Fecha de generación
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Paragraph fecha = new Paragraph("Generado el: " + LocalDateTime.now().format(formatter))
            .setFontSize(10)
            .setItalic()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(fecha);

        // Línea separadora
        document.add(new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ACCENT_COLOR)
            .setMarginBottom(15));

        // ===============================
        // RESUMEN ESTADÍSTICO
        // ===============================
        long totalUsuarios = usuarios.size();
        long usuariosActivos = usuarios.stream().filter(User::getActivo).count();
        long usuariosInactivos = totalUsuarios - usuariosActivos;
        long administradores = usuarios.stream().filter(u -> "ADMIN".equals(u.getRol())).count();
        long usuariosNormales = totalUsuarios - administradores;

        Table estadisticas = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(20);

        // Encabezados de estadísticas
        estadisticas.addHeaderCell(crearCeldaEncabezado("Total Usuarios"));
        estadisticas.addHeaderCell(crearCeldaEncabezado("Activos"));
        estadisticas.addHeaderCell(crearCeldaEncabezado("Inactivos"));
        estadisticas.addHeaderCell(crearCeldaEncabezado("Admins"));
        estadisticas.addHeaderCell(crearCeldaEncabezado("Users"));

        // Datos de estadísticas
        estadisticas.addCell(crearCeldaDato(String.valueOf(totalUsuarios)));
        estadisticas.addCell(crearCeldaDato(String.valueOf(usuariosActivos)));
        estadisticas.addCell(crearCeldaDato(String.valueOf(usuariosInactivos)));
        estadisticas.addCell(crearCeldaDato(String.valueOf(administradores)));
        estadisticas.addCell(crearCeldaDato(String.valueOf(usuariosNormales)));

        document.add(estadisticas);

        // ===============================
        // TABLA DE USUARIOS
        // ===============================
        Paragraph tituloTabla = new Paragraph("Listado Detallado de Usuarios")
            .setFontSize(12)
            .setBold()
            .setFontColor(HEADER_COLOR)
            .setMarginTop(10)
            .setMarginBottom(10);
        document.add(tituloTabla);

        // Crear tabla con 8 columnas
        float[] columnWidths = {1, 3, 3, 2, 2, 1.5f, 1.5f, 2};
        Table tabla = new Table(UnitValue.createPercentArray(columnWidths))
            .setWidth(UnitValue.createPercentValue(100));

        // Encabezados de la tabla
        tabla.addHeaderCell(crearCeldaEncabezado("ID"));
        tabla.addHeaderCell(crearCeldaEncabezado("Nombre"));
        tabla.addHeaderCell(crearCeldaEncabezado("Email"));
        tabla.addHeaderCell(crearCeldaEncabezado("Teléfono"));
        tabla.addHeaderCell(crearCeldaEncabezado("Documento"));
        tabla.addHeaderCell(crearCeldaEncabezado("Género"));
        tabla.addHeaderCell(crearCeldaEncabezado("Rol"));
        tabla.addHeaderCell(crearCeldaEncabezado("Estado"));

        // Datos de usuarios
        DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (User u : usuarios) {
            tabla.addCell(crearCeldaDato(String.valueOf(u.getId())));
            tabla.addCell(crearCeldaDato(u.getName()));
            tabla.addCell(crearCeldaDato(u.getEmail()));
            tabla.addCell(crearCeldaDato(u.getTelefono() != null ? u.getTelefono() : "N/A"));
            tabla.addCell(crearCeldaDato(u.getTipoDocumento() + " " + u.getNumeroIdentificacion()));
            tabla.addCell(crearCeldaDato(u.getGenero() != null ? u.getGenero() : "N/A"));
            
            // Celda de Rol con color
            Cell celdaRol = new Cell().add(new Paragraph(u.getRol()))
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
            if ("ADMIN".equals(u.getRol())) {
                celdaRol.setBackgroundColor(new DeviceRgb(220, 53, 69)); // Rojo
                celdaRol.setFontColor(ColorConstants.WHITE);
            } else {
                celdaRol.setBackgroundColor(new DeviceRgb(13, 110, 253)); // Azul
                celdaRol.setFontColor(ColorConstants.WHITE);
            }
            tabla.addCell(celdaRol);
            
            // Celda de Estado con color
            Cell celdaEstado = new Cell().add(new Paragraph(u.getActivo() ? "Activo" : "Inactivo"))
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
            if (u.getActivo()) {
                celdaEstado.setBackgroundColor(new DeviceRgb(25, 135, 84)); // Verde
                celdaEstado.setFontColor(ColorConstants.WHITE);
            } else {
                celdaEstado.setBackgroundColor(new DeviceRgb(108, 117, 125)); // Gris
                celdaEstado.setFontColor(ColorConstants.WHITE);
            }
            tabla.addCell(celdaEstado);
        }

        document.add(tabla);

        // ===============================
        // PIE DE PÁGINA
        // ===============================
        Paragraph piePagina = new Paragraph("\n\nDocumento generado automáticamente por GoSports")
            .setFontSize(8)
            .setItalic()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY)
            .setMarginTop(20);
        document.add(piePagina);

        Paragraph confidencial = new Paragraph("CONFIDENCIAL - Solo para uso interno")
            .setFontSize(8)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(new DeviceRgb(220, 53, 69));
        document.add(confidencial);

        document.close();
        return baos;
    }

    // ===============================
    // MÉTODOS AUXILIARES
    // ===============================
    private Cell crearCeldaEncabezado(String texto) {
        return new Cell()
            .add(new Paragraph(texto).setBold())
            .setBackgroundColor(HEADER_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10)
            .setPadding(8);
    }

    private Cell crearCeldaDato(String texto) {
        return new Cell()
            .add(new Paragraph(texto))
            .setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5);
    }
}