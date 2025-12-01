package ProyectoSistemaOperativo.kernel;

import ProyectoSistemaOperativo.observer.ProcessObserver;
import ProyectoSistemaOperativo.ui.TaskManagerFrame;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.software.os.OSProcess;
public class KernelManager {
    private SystemInfo systemInfo  = new SystemInfo();
    private OperatingSystem os = systemInfo.getOperatingSystem();
    private PropertyChangeSupport support;
    private static final Logger logger = LogManager.getLogger(KernelManager.class);

    public KernelManager() {
        os = systemInfo.getOperatingSystem();
        support = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
/**
 * Obtiene una lista ordenada por el PID de los procesos
 * aja eso
 */
public List<OSProcess> getProcesses() {
    List<OSProcess> processList = os.getProcesses();
    return processList.stream()
            .sorted(Comparator.comparingInt(OSProcess::getProcessID))
            .collect(Collectors.toList());
}

/**
 * Obtiene un proceso específico por su PID
 */
public OSProcess getProcessById(int pid) {
    return os.getProcess(pid);
}

/**
 * Termina un proceso por su PID
 */
public boolean terminateProcess(int pid) {
    try {
        // Usar comando del sistema para terminar el proceso
        Process killProcess = Runtime.getRuntime().exec("taskkill /PID " + pid + " /F");
        int exitCode = killProcess.waitFor();
        return exitCode == 0;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

/**
 * Suspende un proceso por su PID (Windows - usa pssuspend de Sysinternals)
 */
public boolean suspendProcess(int pid) {
    try {
        // Usar pssuspend.exe para suspender el proceso (requiere Sysinternals tools)
        Process suspendProcess = Runtime.getRuntime().exec("pssuspend.exe " + pid);
        int exitCode = suspendProcess.waitFor();
        if (exitCode == 0) {
            logger.info("Proceso " + pid + " suspendido exitosamente.");
            return true;
        } else {
            logger.warn("No se pudo suspender el proceso " + pid + ". Asegúrate de que pssuspend.exe esté en el PATH.");
            return false;
        }
    } catch (Exception e) {
        logger.error("Error suspendiendo proceso " + pid, e);
        return false;
    }
}

/**
 * Reanuda un proceso por su PID (Windows - requiere herramientas externas)
 */
public boolean resumeProcess(int pid) {
    try {
        // Similar a suspend, requiere herramientas externas
        logger.warn("Reanudar proceso no implementado completamente en Windows. Requiere herramientas externas.");
        return false; // No implementado
    } catch (Exception e) {
        logger.error("Error reanudando proceso " + pid, e);
        return false;
    }
}

/**
 * Cambia la prioridad de un proceso
 * priority: "LOW", "BELOW_NORMAL", "NORMAL", "ABOVE_NORMAL", "HIGH", "REALTIME"
 */
public boolean changePriority(int pid, String priority) {
    try {
        // Usar wmic para cambiar la prioridad
        String wmicCommand = "wmic process where ProcessId=" + pid + " call setpriority \"" + getPriorityClass(priority) + "\"";
        Process priorityProcess = Runtime.getRuntime().exec(wmicCommand);
        int exitCode = priorityProcess.waitFor();
        return exitCode == 0;
    } catch (Exception e) {
        logger.error("Error cambiando prioridad del proceso " + pid, e);
        return false;
    }
}

private String getPriorityClass(String priority) {
    switch (priority.toUpperCase()) {
        case "REALTIME": return "256";
        case "HIGH": return "128";
        case "ABOVE_NORMAL": return "32768";
        case "NORMAL": return "32";
        case "BELOW_NORMAL": return "16384";
        case "LOW": return "64";
        default: return "32"; // NORMAL
    }
}
    public void updateProcessList() {
        try {
            List<OSProcess> processes = getProcesses();
            support.firePropertyChange("processes", null, processes);
        } catch (Exception e) {
            logger.error("Error updating processes", e);
        }
    }
}
