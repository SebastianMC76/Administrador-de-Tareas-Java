package ProyectoSistemaOperativo.ui;

import ProyectoSistemaOperativo.kernel.KernelManager;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Ventana del Monitor del Sistema - MÓDULO 5
 * Muestra estadísticas de rendimiento del sistema similares al Task Manager
 */
public class SystemMonitorFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger(SystemMonitorFrame.class);
    private Timer refreshTimer;
    private KernelManager kernelManager;

    // Componentes de CPU
    private JLabel cpuLabel;
    private JProgressBar cpuProgressBar;

    // Componentes de Memoria
    private JLabel memoryLabel;
    private JProgressBar memoryProgressBar;
    private JLabel memoryDetailsLabel;

    // Componentes de Disco
    private JLabel diskReadLabel;
    private JLabel diskWriteLabel;

    // Componentes de Red
    private JLabel networkUpLabel;
    private JLabel networkDownLabel;

    private SystemInfo systemInfo;
    private CentralProcessor processor;
    private GlobalMemory memory;

    public SystemMonitorFrame(KernelManager kernelManager) {
        this.kernelManager = kernelManager;
        systemInfo = new SystemInfo();
        processor = systemInfo.getHardware().getProcessor();
        memory = systemInfo.getHardware().getMemory();

        setTitle("Monitor del Sistema");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        startAutoRefresh();
        logger.info("Monitor del Sistema inicializado.");
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Panel principal con métricas
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Panel CPU
        JPanel cpuPanel = createCpuPanel();
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(cpuPanel, gbc);

        // Panel Memoria
        JPanel memoryPanel = createMemoryPanel();
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        mainPanel.add(memoryPanel, gbc);

        // Panel Disco
        JPanel diskPanel = createDiskPanel();
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        mainPanel.add(diskPanel, gbc);

        // Panel Red
        JPanel networkPanel = createNetworkPanel();
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 1;
        mainPanel.add(networkPanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(20, 20, 40)); // Fondo oscuro
        JButton closeButton = new JButton("Cerrar");
        closeButton.setBackground(new Color(100, 50, 50)); // Rojo oscuro
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        logger.info("Interfaz del Monitor del Sistema inicializada.");
    }

    private JPanel createCpuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Poder del Reino"));

        cpuLabel = new JLabel("Poder del Reino: Calculando...");
        cpuProgressBar = new JProgressBar(0, 100);
        cpuProgressBar.setStringPainted(true);

        panel.add(cpuLabel, BorderLayout.NORTH);
        panel.add(cpuProgressBar, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMemoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Energía Vital"));

        memoryLabel = new JLabel("Energía Vital: Calculando...");
        memoryProgressBar = new JProgressBar(0, 100);
        memoryProgressBar.setStringPainted(true);
        memoryDetailsLabel = new JLabel("Total: -- | Usada: -- | Disponible: --");

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.add(memoryProgressBar, BorderLayout.CENTER);
        detailsPanel.add(memoryDetailsLabel, BorderLayout.SOUTH);

        panel.add(memoryLabel, BorderLayout.NORTH);
        panel.add(detailsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDiskPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Disco"));

        diskReadLabel = new JLabel("Lectura: Calculando...");
        diskWriteLabel = new JLabel("Escritura: Calculando...");

        panel.add(diskReadLabel);
        panel.add(diskWriteLabel);

        return panel;
    }

    private JPanel createNetworkPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Red"));

        networkUpLabel = new JLabel("Subida: Calculando...");
        networkDownLabel = new JLabel("Bajada: Calculando...");

        panel.add(networkUpLabel);
        panel.add(networkDownLabel);

        return panel;
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(1000, e -> updateSystemStats());
        refreshTimer.start();
    }

    private void updateSystemStats() {
        try {
            updateCpuStats();
            updateMemoryStats();
            updateDiskStats();
            updateNetworkStats();
        } catch (Exception e) {
            logger.error("Error actualizando estadísticas del sistema", e);
        }
    }

    private void updateCpuStats() {
        double[] load = processor.getSystemLoadAverage(1);
        double cpuLoad = load[0] * 100;
        cpuLabel.setText(String.format("Uso de CPU: %.1f%%", cpuLoad));
        cpuProgressBar.setValue((int) cpuLoad);
    }

    private void updateMemoryStats() {
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;

        double memoryPercent = (double) usedMemory / totalMemory * 100;

        memoryLabel.setText(String.format("Uso de Memoria: %.1f%%", memoryPercent));
        memoryProgressBar.setValue((int) memoryPercent);

        String totalGB = String.format("%.1f GB", totalMemory / (1024.0 * 1024.0 * 1024.0));
        String usedGB = String.format("%.1f GB", usedMemory / (1024.0 * 1024.0 * 1024.0));
        String availableGB = String.format("%.1f GB", availableMemory / (1024.0 * 1024.0 * 1024.0));

        memoryDetailsLabel.setText(String.format("Total: %s | Usada: %s | Disponible: %s",
                totalGB, usedGB, availableGB));
    }

    private void updateDiskStats() {
        List<HWDiskStore> disks = systemInfo.getHardware().getDiskStores();
        long totalReadBytes = 0;
        long totalWriteBytes = 0;

        for (HWDiskStore disk : disks) {
            totalReadBytes += disk.getReadBytes();
            totalWriteBytes += disk.getWriteBytes();
        }

        String readMB = String.format("%.1f MB/s", totalReadBytes / (1024.0 * 1024.0));
        String writeMB = String.format("%.1f MB/s", totalWriteBytes / (1024.0 * 1024.0));

        diskReadLabel.setText("Lectura: " + readMB);
        diskWriteLabel.setText("Escritura: " + writeMB);
    }

    private void updateNetworkStats() {
        List<NetworkIF> networks = systemInfo.getHardware().getNetworkIFs();
        long totalBytesSent = 0;
        long totalBytesRecv = 0;

        for (NetworkIF net : networks) {
            totalBytesSent += net.getBytesSent();
            totalBytesRecv += net.getBytesRecv();
        }

        String upMB = String.format("%.1f MB", totalBytesSent / (1024.0 * 1024.0));
        String downMB = String.format("%.1f MB", totalBytesRecv / (1024.0 * 1024.0));

        networkUpLabel.setText("Subida: " + upMB);
        networkDownLabel.setText("Bajada: " + downMB);
    }
}
