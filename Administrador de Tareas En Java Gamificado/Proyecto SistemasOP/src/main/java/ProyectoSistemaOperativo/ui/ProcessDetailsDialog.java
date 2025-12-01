package ProyectoSistemaOperativo.ui;

import ProyectoSistemaOperativo.kernel.KernelManager;
import oshi.software.os.OSProcess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo para mostrar detalles avanzados de un proceso.
 * Similar a las propiedades del Explorador de Windows.
 */
public class ProcessDetailsDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger(ProcessDetailsDialog.class);
    private KernelManager kernelManager;
    private int pid;

    public ProcessDetailsDialog(Frame parent, KernelManager kernelManager, int pid) {
        super(parent, "Detalles del Proceso - PID: " + pid, true);
        this.kernelManager = kernelManager;
        this.pid = pid;

        initUI();
        loadProcessDetails();

        setSize(400, 300);
        setLocationRelativeTo(parent);
        logger.info("ProcessDetailsDialog inicializado para PID: " + pid);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Labels para los detalles
        String[] labels = {"Nombre de la Criatura:", "Ruta del Ejecutable:", "Nivel:", "Parent Nivel:", "Poder de Ataque:", "Vida:", "Tiempo de Invocación:", "Estado:"};
        JTextField[] fields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            detailsPanel.add(new JLabel(labels[i]), gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            fields[i] = new JTextField(20);
            fields[i].setEditable(false);
            detailsPanel.add(fields[i], gbc);
        }

        add(new JScrollPane(detailsPanel), BorderLayout.CENTER);

        JButton closeButton = new JButton("Cerrar");
        closeButton.setBackground(new Color(100, 50, 50)); // Rojo oscuro
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(20, 20, 40)); // Fondo oscuro
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadProcessDetails() {
        OSProcess process = kernelManager.getProcessById(pid);
        if (process != null) {
            // Obtener los componentes del panel
            Container detailsPanel = (Container) ((JScrollPane) getContentPane().getComponent(0)).getViewport().getView();
            Component[] components = detailsPanel.getComponents();
            JTextField[] fields = new JTextField[8];
            int fieldIndex = 0;
            for (Component comp : components) {
                if (comp instanceof JTextField) {
                    fields[fieldIndex++] = (JTextField) comp;
                }
            }

            // Llenar los campos con datos del proceso
            fields[0].setText(process.getName());
            fields[1].setText(process.getPath()); // Ruta del ejecutable
            fields[2].setText(String.valueOf(process.getProcessID()));
            fields[3].setText(String.valueOf(process.getParentProcessID()));
            double cpuLoad = process.getProcessCpuLoadCumulative();
            double cpuPercentValue = Math.min(cpuLoad * 100, 100.0); // Cap at 100%
            fields[4].setText(String.format("%.2f%%", cpuPercentValue));
            fields[5].setText(formatBytes(process.getResidentSetSize()));
            fields[6].setText(formatStartTime(process.getStartTime()));
            fields[7].setText(process.getState().name());

            logger.info("Detalles del proceso cargados para PID: " + pid);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo obtener información del proceso.", "Error", JOptionPane.ERROR_MESSAGE);
            logger.warn("No se pudo obtener proceso con PID: " + pid);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }

    private String formatStartTime(long startTimeMillis) {
        if (startTimeMillis == 0) return "N/A";
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
