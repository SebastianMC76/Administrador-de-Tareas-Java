package ProyectoSistemaOperativo.ui;

import ProyectoSistemaOperativo.kernel.KernelManager;
import ProyectoSistemaOperativo.observer.ProcessObserver;
import ProyectoSistemaOperativo.observer.ProcessObserverImpl;
import oshi.software.os.OSProcess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.RowSorter;
import javax.swing.SortOrder;

import static oshi.util.FormatUtil.formatBytes;

/**
 * Ventana principal del administrador de tareas,osea la interfaz grafica
 * implementa el patrón observer para ver que proceso esta seleccionado
 * Muestra la lista de procesos del sistema y notifica procesos seleccionados.
 */
public class TaskManagerFrame extends JFrame implements PropertyChangeListener {
    private static final Logger logger = LogManager.getLogger(TaskManagerFrame.class);
    private Timer refreshTimer;

    private JTable processTable;
    private DefaultTableModel processTableModel;
    private KernelManager kernelManager;
    private ProcessObserverImpl processObserver;

    private JPanel infoPanel;
    private JButton terminateButton;
    private JButton suspendButton;
    private JButton resumeButton;
    private JButton detailsButton;
    private JComboBox<String> priorityComboBox;
    private JTextField searchField;
    private JComboBox<String> filterComboBox;
    private JComboBox<String> sortComboBox;
    private TableRowSorter<DefaultTableModel> sorter;
    private int selectedPid = -1;
    private ActionListener priorityListener;

    public TaskManagerFrame(KernelManager kernelManager) {
        setTitle("System Guardians");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(20, 20, 40)); // Dark fantasy background

        this.kernelManager = kernelManager;
        processObserver = new ProcessObserverImpl(this);
        processObserver.addPropertyChangeListener(this);
        kernelManager.addPropertyChangeListener(processObserver);

        initUI();
        loadProcesses();
        startAutoRefresh();
        initializeResources();
    }

    private void initUI() {
        String[] columns = {"Nivel", "Criatura", "Ataque", "Vida", "Estado", "Clase"};
        processTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        processTable.setBackground(new Color(30, 30, 50));
        processTable.setForeground(Color.WHITE);
        processTable.setGridColor(new Color(100, 100, 150));

        sorter = new TableRowSorter<>(processTableModel);
        processTable.setRowSorter(sorter);

        // Custom renderer for CPU column to highlight high CPU usage
        processTable.getColumnModel().getColumn(2).setCellRenderer(new HighCpuRenderer());

        processTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedRow = processTable.getSelectedRow();
                if (selectedRow != -1) {
                    int modelRow = processTable.convertRowIndexToModel(selectedRow);
                    int pid = (int) processTableModel.getValueAt(modelRow, 0);
                    processObserver.setObservedProcessId(pid);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(processTable);
        scrollPane.setBackground(new Color(20, 20, 40));

        // Panel de búsqueda y filtros
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.setBackground(new Color(40, 40, 60));
        searchField = new JTextField(20);
        searchField.setBackground(Color.BLACK);
        searchField.setForeground(Color.WHITE);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
        filterComboBox = new JComboBox<>(new String[]{"Todos", "Guardianes", "Aliados", "Enemigos"});
        filterComboBox.setBackground(new Color(50, 50, 70));
        filterComboBox.setForeground(Color.WHITE);
        filterComboBox.addActionListener(e -> applyFilters());
        sortComboBox = new JComboBox<>(new String[]{"Ninguno", "Nivel Ascendente", "Nivel Descendente", "Nombre Ascendente", "Nombre Descendente", "Ataque Descendente", "Vida Descendente"});
        sortComboBox.setBackground(new Color(50, 50, 70));
        sortComboBox.setForeground(Color.WHITE);
        sortComboBox.addActionListener(e -> applySorting());
        searchPanel.add(new JLabel("Rastreador de Criaturas:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Filtro de Clase:"));
        searchPanel.add(filterComboBox);
        searchPanel.add(new JLabel("Ordenar por:"));
        searchPanel.add(sortComboBox);

        // Panel de información del proceso seleccionado
        infoPanel = new JPanel(new GridLayout(4, 2));
        terminateButton = new JButton("Terminar Proceso");
        terminateButton.setEnabled(false);
        terminateButton.setBackground(new Color(150, 50, 50)); // Rojo oscuro
        terminateButton.setForeground(Color.WHITE);
        terminateButton.addActionListener(e -> terminateSelectedProcess());

        suspendButton = new JButton("Suspender Proceso");
        suspendButton.setEnabled(false);
        suspendButton.setBackground(new Color(150, 100, 50)); // Naranja oscuro
        suspendButton.setForeground(Color.WHITE);
        suspendButton.addActionListener(e -> suspendSelectedProcess());

        resumeButton = new JButton("Reanudar Proceso");
        resumeButton.setEnabled(false);
        resumeButton.setBackground(new Color(50, 150, 50)); // Verde oscuro
        resumeButton.setForeground(Color.WHITE);
        resumeButton.addActionListener(e -> resumeSelectedProcess());

        priorityComboBox = new JComboBox<>(new String[]{"LOW", "BELOW_NORMAL", "NORMAL", "ABOVE_NORMAL", "HIGH", "REALTIME"});
        priorityComboBox.setEnabled(false);
        priorityComboBox.setBackground(new Color(50, 50, 100)); // Azul oscuro
        priorityComboBox.setForeground(Color.WHITE);
        priorityListener = e -> changePriority();
        priorityComboBox.addActionListener(priorityListener);

        detailsButton = new JButton("Detalles del Proceso");
        detailsButton.setEnabled(false);
        detailsButton.setBackground(new Color(100, 50, 100)); // Morado oscuro
        detailsButton.setForeground(Color.WHITE);
        detailsButton.addActionListener(e -> showProcessDetails());

        JButton chartsButton = new JButton("Gráficos en Tiempo Real");
        chartsButton.setBackground(new Color(50, 100, 100)); // Cian oscuro
        chartsButton.setForeground(Color.WHITE);
        chartsButton.addActionListener(e -> showCharts());

        JButton systemMonitorButton = new JButton("Monitor del Sistema");
        systemMonitorButton.setBackground(new Color(100, 100, 50)); // Amarillo oscuro
        systemMonitorButton.setForeground(Color.WHITE);
        systemMonitorButton.addActionListener(e -> showSystemMonitor());

        JButton closeButton = new JButton("Cerrar Programa");
        closeButton.setBackground(new Color(100, 50, 50)); // Rojo oscuro
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> System.exit(0));

        infoPanel.add(terminateButton);
        infoPanel.add(suspendButton);
        infoPanel.add(resumeButton);
        infoPanel.add(priorityComboBox);
        infoPanel.add(detailsButton);
        infoPanel.add(chartsButton);
        infoPanel.add(systemMonitorButton);
        infoPanel.add(closeButton);

        setLayout(new BorderLayout());
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);
        logger.info("Interfaz TaskManagerFrame inicializada.");
    }

    private void initializeResources() {
        // Cargar icono
        try {
            ImageIcon icon = new ImageIcon(TaskManagerFrame.class.getResource("/ProyectoSistemaOperativo/icono.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            logger.error("Error al cargar el icono", e);
        }

        // Reproducir música después de que la ventana sea visible
        SwingUtilities.invokeLater(() -> playAudio("/ProyectoSistemaOperativo/Undertale  fallen down 1 hour.wav"));
    }

    private void playAudio(String audioFilePath) {
        try {
            // Obtener el archivo de audio desde el classpath
            File audioFile = new File(TaskManagerFrame.class.getResource(audioFilePath).toURI());
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            logger.info("Reproduciendo audio: " + audioFilePath);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | java.net.URISyntaxException e) {
            logger.error("Error al reproducir el audio", e);
        }
    }

    private void loadProcesses() {
        List<OSProcess> processes = kernelManager.getProcesses();
        processTableModel.setRowCount(0);
        for (OSProcess process : processes) {
            double cpuLoad = process.getProcessCpuLoadCumulative();
            double cpuPercentValue = Math.min(cpuLoad * 100, 100.0); // Cap at 100%
            String cpuPercent = String.format("%.1f%%", cpuPercentValue);
            String type = getProcessType(process);
            String creatureName = getCreatureName(process.getName());
            Object[] row = {
                    process.getProcessID(), // Nivel
                    creatureName, // Criatura
                    cpuPercent, // Ataque
                    formatBytes(process.getResidentSetSize()), // Vida
                    process.getState().name(), // Estado
                    type // Clase
            };
            processTableModel.addRow(row);
        }
        logger.info("Cargadas " + processes.size() + " criaturas en el reino.");
    }

    private String getCreatureName(String processName) {
        // Map common processes to creature names
        switch (processName.toLowerCase()) {
            case "chrome.exe": return "Dragón de Fuego";
            case "explorer.exe": return "Guardián del Bosque";
            case "svchost.exe": return "Espíritu Ancestral";
            case "system": return "Rey de las Sombras";
            case "idle": return "Espíritu Pacífico";
            case "python.exe": return "Mago Oscuro";
            case "java.exe": return "Caballero Místico";
            case "steam.exe": return "Invocador";
            default: return processName; // Keep original if not mapped
        }
    }

    private String getProcessType(OSProcess process) {
        int pid = process.getProcessID();
        String name = process.getName().toLowerCase();
        if (pid < 100) {
            return "Guardianes";
        } else if (name.contains("discord") || name.contains("word") || name.contains("excel") || name.contains("powerpoint") || name.contains("notepad") || name.contains("calc")) {
            return "Enemigos";
        } else {
            return "Aliados";
        }
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String filter = (String) filterComboBox.getSelectedItem();

        RowFilter<DefaultTableModel, Object> rowFilter = new RowFilter<DefaultTableModel, Object>() {
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                String name = entry.getStringValue(1).toLowerCase();
                if (!name.contains(searchText)) return false;

                if ("Guardianes".equals(filter)) {
                    String type = entry.getStringValue(5); // Tipo is column 5
                    if (!"Guardianes".equals(type)) return false;
                } else if ("Aliados".equals(filter)) {
                    String type = entry.getStringValue(5); // Tipo is column 5
                    if (!"Aliados".equals(type)) return false;
                } else if ("Enemigos".equals(filter)) {
                    String type = entry.getStringValue(5); // Tipo is column 5
                    if (!"Enemigos".equals(type)) return false;
                }

                return true;
            }
        };
        sorter.setRowFilter(rowFilter);
    }

    private double parseMemory(String memStr) {
        try {
            if (memStr.endsWith("GB")) {
                return Double.parseDouble(memStr.replace("GB", "").trim()) * 1024;
            } else if (memStr.endsWith("MB")) {
                return Double.parseDouble(memStr.replace("MB", "").trim());
            } else if (memStr.endsWith("KB")) {
                return Double.parseDouble(memStr.replace("KB", "").trim()) / 1024;
            } else {
                return Double.parseDouble(memStr.replace("B", "").trim()) / 1024 / 1024;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseCpu(String cpuStr) {
        try {
            return Double.parseDouble(cpuStr.replace("%", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(1000, e -> {
            kernelManager.updateProcessList();
        });
        refreshTimer.start();
    }

    public void updateProcessTable(List<OSProcess> processes) {
        DefaultTableModel model = (DefaultTableModel) processTable.getModel();
        model.setRowCount(0);

        for (OSProcess p : processes) {
            double cpuLoad = p.getProcessCpuLoadCumulative();
            double cpuPercentValue = Math.min(cpuLoad * 100, 100.0); // Cap at 100%
            String cpuPercent = String.format("%.1f%%", cpuPercentValue);
            String type = getProcessType(p);
            String creatureName = getCreatureName(p.getName());
            model.addRow(new Object[]{
                    p.getProcessID(), // Nivel
                    creatureName, // Criatura
                    cpuPercent, // Ataque
                    formatBytes(p.getResidentSetSize()), // Vida
                    p.getState().name(), // Estado
                    type // Clase
            });
        }

        // Re-select the previously selected process if it still exists
        if (selectedPid != -1) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((int) model.getValueAt(i, 0) == selectedPid) {
                    int viewRow = processTable.convertRowIndexToView(i);
                    if (viewRow >= 0 && viewRow < processTable.getRowCount()) {
                        processTable.setRowSelectionInterval(viewRow, viewRow);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("observedProcessId".equals(evt.getPropertyName())) {
            int oldPid = (int) evt.getOldValue();
            int newPid = (int) evt.getNewValue();
            selectedPid = newPid;
            logger.info("Cambio en observedProcessId: " + oldPid + " -> " + newPid);
            updateProcessInfo(newPid);
        }
    }

    private void updateProcessInfo(int pid) {
        OSProcess process = kernelManager.getProcessById(pid);
        if (process != null) {
            terminateButton.setEnabled(true);
            suspendButton.setEnabled(true);
            resumeButton.setEnabled(true);
            detailsButton.setEnabled(true);
            priorityComboBox.setEnabled(true);
            priorityComboBox.removeActionListener(priorityListener);
            priorityComboBox.setSelectedItem("NORMAL"); // Default
            priorityComboBox.addActionListener(priorityListener);
        } else {
            terminateButton.setEnabled(false);
            suspendButton.setEnabled(false);
            resumeButton.setEnabled(false);
            detailsButton.setEnabled(false);
            priorityComboBox.setEnabled(false);
        }
    }

    private void terminateSelectedProcess() {
        if (selectedPid != -1) {
            boolean success = kernelManager.terminateProcess(selectedPid);
            if (success) {
                JOptionPane.showMessageDialog(this, "Proceso terminado exitosamente. ¡Has ganado 10 puntos de experiencia!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                loadProcesses(); // Recargar la lista de procesos
                selectedPid = -1;
                updateProcessInfo(selectedPid);
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo terminar el proceso.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void suspendSelectedProcess() {
        if (selectedPid != -1) {
            boolean success = kernelManager.suspendProcess(selectedPid);
            if (success) {
                JOptionPane.showMessageDialog(this, "Proceso suspendido exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo suspender el proceso.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void resumeSelectedProcess() {
        if (selectedPid != -1) {
            boolean success = kernelManager.resumeProcess(selectedPid);
            if (success) {
                JOptionPane.showMessageDialog(this, "Proceso reanudado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo reanudar el proceso.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void changePriority() {
        if (selectedPid != -1) {
            String priority = (String) priorityComboBox.getSelectedItem();
            boolean success = kernelManager.changePriority(selectedPid, priority);
            if (success) {
                JOptionPane.showMessageDialog(this, "Prioridad cambiada exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo cambiar la prioridad.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showProcessDetails() {
        if (selectedPid != -1) {
            ProcessDetailsDialog dialog = new ProcessDetailsDialog(this, kernelManager, selectedPid);
            dialog.setVisible(true);
        }
    }

    private void showCharts() {
        RealTimeChartsFrame chartsFrame = new RealTimeChartsFrame(kernelManager);
        chartsFrame.setSelectedProcess(selectedPid);
        chartsFrame.setVisible(true);
    }

    private void showSystemMonitor() {
        SystemMonitorFrame monitorFrame = new SystemMonitorFrame(kernelManager);
        monitorFrame.setVisible(true);
    }

    private void applySorting() {
        String sortOption = (String) sortComboBox.getSelectedItem();
        List<RowSorter.SortKey> sortKeys = new java.util.ArrayList<>();

        switch (sortOption) {
            case "Nivel Ascendente":
                sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                break;
            case "Nivel Descendente":
                sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
                break;
            case "Nombre Ascendente":
                sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
                break;
            case "Nombre Descendente":
                sortKeys.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
                break;
            case "Ataque Descendente":
                sortKeys.add(new RowSorter.SortKey(2, SortOrder.DESCENDING));
                break;
            case "Vida Descendente":
                sortKeys.add(new RowSorter.SortKey(3, SortOrder.DESCENDING));
                break;
            case "Ninguno":
            default:
                // No sorting
                break;
        }

        sorter.setSortKeys(sortKeys);
    }

    // Custom renderer for CPU column to highlight high CPU usage
    private static class HighCpuRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof String) {
                try {
                    double cpuPercent = Double.parseDouble(((String) value).replace("%", "").trim());
                    if (cpuPercent > 50) {
                        setBackground(java.awt.Color.RED);
                        setForeground(java.awt.Color.WHITE);
                    } else if (cpuPercent > 20) {
                        setBackground(java.awt.Color.YELLOW);
                        setForeground(java.awt.Color.BLACK);
                    } else {
                        setBackground(table.getBackground());
                        setForeground(table.getForeground());
                    }
                } catch (NumberFormatException e) {
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                }
            }

            return this;
        }
    }
}
