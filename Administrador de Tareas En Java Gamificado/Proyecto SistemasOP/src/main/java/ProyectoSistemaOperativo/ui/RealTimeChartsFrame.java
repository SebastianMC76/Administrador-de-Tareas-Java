package ProyectoSistemaOperativo.ui;

import ProyectoSistemaOperativo.kernel.KernelManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSProcess;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

/**
 * Ventana de Gráficos en Tiempo Real - MÓDULO 6
 * Muestra gráficos similares al "Performance Graph" del Task Manager
 */
public class RealTimeChartsFrame extends JFrame {
    private static final Logger logger = LogManager.getLogger(RealTimeChartsFrame.class);
    private KernelManager kernelManager;
    private Timer updateTimer;

    // Series de datos para los gráficos
    private TimeSeries cpuSeries;
    private TimeSeries memorySeries;
    private TimeSeries processCpuSeries;

    // Componentes de OSHI
    private SystemInfo systemInfo;
    private CentralProcessor processor;
    private GlobalMemory memory;

    // ID del proceso seleccionado
    private int selectedPid = -1;

    public RealTimeChartsFrame(KernelManager kernelManager) {
        this.kernelManager = kernelManager;
        this.systemInfo = new SystemInfo();
        this.processor = systemInfo.getHardware().getProcessor();
        this.memory = systemInfo.getHardware().getMemory();

        setTitle("Gráficos en Tiempo Real");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        startUpdates();

        logger.info("Gráficos en Tiempo Real inicializados.");
    }

    public void setSelectedProcess(int pid) {
        this.selectedPid = pid;
        if (pid != -1) {
            OSProcess process = kernelManager.getProcessById(pid);
            if (process != null) {
                setTitle("Gráficos en Tiempo Real - Proceso: " + process.getName() + " (PID: " + pid + ")");
            }
        } else {
            setTitle("Gráficos en Tiempo Real");
        }
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Crear panel con pestañas para diferentes gráficos
        JTabbedPane tabbedPane = new JTabbedPane();

        // Gráfico de CPU General
        JPanel cpuPanel = createCpuChartPanel();
        tabbedPane.addTab("Poder del Reino", cpuPanel);

        // Gráfico de RAM General
        JPanel memoryPanel = createMemoryChartPanel();
        tabbedPane.addTab("Energía Vital", memoryPanel);

        // Gráfico de CPU por Proceso
        JPanel processCpuPanel = createProcessCpuChartPanel();
        tabbedPane.addTab("Poder de Criatura", processCpuPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(20, 20, 40)); // Fondo oscuro
        JButton closeButton = new JButton("Cerrar");
        closeButton.setBackground(new Color(100, 50, 50)); // Rojo oscuro
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createCpuChartPanel() {
        cpuSeries = new TimeSeries("CPU %");
        TimeSeriesCollection dataset = new TimeSeriesCollection(cpuSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Poder de Ataque del Reino",
                "Tiempo",
                "Porcentaje (%)",
                dataset,
                false,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(750, 400));

        return chartPanel;
    }

    private JPanel createMemoryChartPanel() {
        memorySeries = new TimeSeries("RAM %");
        TimeSeriesCollection dataset = new TimeSeriesCollection(memorySeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Uso de Memoria RAM",
                "Tiempo",
                "Porcentaje (%)",
                dataset,
                false,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(750, 400));

        return chartPanel;
    }

    private JPanel createProcessCpuChartPanel() {
        processCpuSeries = new TimeSeries("CPU Proceso %");
        TimeSeriesCollection dataset = new TimeSeriesCollection(processCpuSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Poder de Ataque de la Criatura Seleccionada",
                "Tiempo",
                "Porcentaje (%)",
                dataset,
                false,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setRange(0.0, 100.0);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(750, 400));

        return chartPanel;
    }

    private void startUpdates() {
        updateTimer = new Timer(1000, e -> updateCharts());
        updateTimer.start();
    }

    private void updateCharts() {
        try {
            Millisecond now = new Millisecond(new Date());

            // Actualizar CPU General
            double[] load = processor.getSystemLoadAverage(1);
            double cpuLoad = Math.min(load[0] * 100, 100.0);
            cpuSeries.add(now, cpuLoad);

            // Mantener solo los últimos 60 puntos (1 minuto)
            if (cpuSeries.getItemCount() > 60) {
                cpuSeries.delete(0, cpuSeries.getItemCount() - 61);
            }

            // Actualizar Memoria
            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            long usedMemory = totalMemory - availableMemory;
            double memoryPercent = (double) usedMemory / totalMemory * 100;
            memorySeries.add(now, memoryPercent);

            if (memorySeries.getItemCount() > 60) {
                memorySeries.delete(0, memorySeries.getItemCount() - 61);
            }

            // Actualizar CPU del proceso seleccionado
            if (selectedPid != -1) {
                OSProcess process = kernelManager.getProcessById(selectedPid);
                if (process != null) {
                    double processCpu = process.getProcessCpuLoadCumulative() * 100;
                    processCpuSeries.add(now, Math.min(processCpu, 100.0));

                    if (processCpuSeries.getItemCount() > 60) {
                        processCpuSeries.delete(0, processCpuSeries.getItemCount() - 61);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error actualizando gráficos", e);
        }
    }

    @Override
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        super.dispose();
    }
}
