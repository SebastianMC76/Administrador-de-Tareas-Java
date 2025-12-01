package ProyectoSistemaOperativo.observer;

import oshi.software.os.OSProcess;
import java.util.List;

public interface ProcessObserver {
    void onProcessUpdate(List<OSProcess> processes);
}
