package ProyectoSistemaOperativo.observer;

import ProyectoSistemaOperativo.ui.TaskManagerFrame;
import oshi.software.os.OSProcess;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;


public class ProcessObserverImpl implements ProcessObserver, PropertyChangeListener {
    private PropertyChangeSupport support;
    private TaskManagerFrame frame;

    private int observedProcessId;


    public ProcessObserverImpl(TaskManagerFrame frame) {
        support = new PropertyChangeSupport(this);
        this.frame = frame;
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }

    public int getObservedProcessId() {
        return observedProcessId;
    }

    public void setObservedProcessId(int newProcessId) {
        int oldProcessId = this.observedProcessId;
        this.observedProcessId = newProcessId;
        support.firePropertyChange("observedProcessId", oldProcessId, newProcessId);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("processes".equals(evt.getPropertyName())) {
            List<OSProcess> processes = (List<OSProcess>) evt.getNewValue();
            onProcessUpdate(processes);
        }
    }

    @Override
    public void onProcessUpdate(List<OSProcess> processes) {
        SwingUtilities.invokeLater(() -> {
            frame.updateProcessTable(processes);
        });
    }
}
