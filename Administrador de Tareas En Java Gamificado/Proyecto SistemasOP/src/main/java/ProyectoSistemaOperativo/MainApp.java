package ProyectoSistemaOperativo;

import ProyectoSistemaOperativo.kernel.KernelManager;
import ProyectoSistemaOperativo.ui.TaskManagerFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainApp {
    private static final Logger logger = LogManager.getRootLogger();

    public static void main(String[] args) {
        logger.info("Iniciando");
        KernelManager kernelManager = new KernelManager();
        TaskManagerFrame frame = new TaskManagerFrame(kernelManager);

        frame.setVisible(true);

        logger.info("Administrador de Tareas Iniciado.");
    }
}
